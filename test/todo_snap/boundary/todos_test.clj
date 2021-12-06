(ns todo-snap.boundary.todos-test
  (:require [todo-snap.boundary.todos :as todos]
            [duct.database.sql :as duct-sql]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as rag-jdbc]
            [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]))

;; helpers

(def ^:private pg-db-spec
  {:connection-uri
   "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres"})

(defn make-test-db
  ([] (make-test-db (str (java.util.UUID/randomUUID))))

  ([db-name]
   (jdbc/execute! pg-db-spec [(str "DROP DATABASE IF EXISTS \"" db-name "\"")] {:transaction? false})
   (jdbc/execute! pg-db-spec [(str "CREATE DATABASE \"" db-name "\"")] {:transaction? false})

   (let [connection-uri (str "jdbc:postgresql://localhost:5432/" db-name "?user=postgres&password=postgres")
         db-spec        {:connection-uri connection-uri}
         ;; NOTE this depends on having a repl running
         migrations     (ig/init-key :duct.migrator.ragtime/resources {:path "todo_snap/migrations"})]

     (ragtime/migrate-all (rag-jdbc/sql-database db-spec) {} migrations)

     (duct-sql/->Boundary db-spec))))

(def ^:private valid-email "valid@gmail.com")

(defn- insert-todo! [db title]
  (first (todos/create-todo db {:title title :email valid-email})))

(defn- complete-todo! [db todo]
  (todos/update-todo db {:complete true
                         :email    valid-email
                         :id       (:id todo)}))

(defn- delete-todo! [db todo]
  (todos/delete-todo db {:email valid-email
                         :id    (:id todo)}))

(defn- strip-burndown-event
  "Keeps only the deterministic fields of a todo audit event for test assertions"
  [event]
  (select-keys event [:burndown_total :change :complete
                      :prev_complete :op :title :deleted]))

;; tests

(t/deftest todos-boundary-test-create
  (t/testing "create and complete a todo"
    (let [db           (make-test-db)
          cookies-todo (insert-todo! db "bake cookies")]

      (complete-todo! db cookies-todo)

      (let [listed-todos  (todos/list-todos db valid-email)
            expected-todo (-> cookies-todo
                              (select-keys todos/public-todo-cols)
                              (assoc :complete true))]
        (t/is (= [expected-todo] listed-todos))))))

(t/deftest todos-boundary-test-summary
  (t/testing "todos summary"
    (let [db           (make-test-db)
          cookies-todo (insert-todo! db "bake cookies")]
      (complete-todo! db cookies-todo)
      (insert-todo! db "eat cookies")
      (insert-todo! db "bake more cookies")

      (t/is (= [{:complete false :count 2} {:complete true :count 1}]
               (todos/summary db valid-email))))))

(t/deftest todos-boundary-test-burndown
  (t/testing "todos burndown"
    (let [db (make-test-db)]
      (let [first-todo  (insert-todo! db "first")
            second-todo (insert-todo! db "second")
            third-todo  (insert-todo! db "third")]

        (complete-todo! db second-todo)

        (let [fourth-todo (insert-todo! db "fourth")]
          (complete-todo! db third-todo)

          (let [fifth-todo (insert-todo! db "fifth")]
            (complete-todo! db fifth-todo)
            (delete-todo! db fourth-todo)
            (complete-todo! db first-todo)
            (delete-todo! db first-todo))))

      (let [expected-events [{:burndown_total 1,
                              :change         1,
                              :complete       false,
                              :op             "insert",
                              :prev_complete  nil,
                              :deleted        false
                              :title          "first"}

                             {:burndown_total 2,
                              :change         1,
                              :complete       false,
                              :op             "insert",
                              :prev_complete  nil,
                              :deleted        false
                              :title          "second"}

                             {:burndown_total 3,
                              :change         1,
                              :complete       false,
                              :op             "insert",
                              :prev_complete  nil,
                              :deleted        false
                              :title          "third"}

                             {:burndown_total 2,
                              :change         -1,
                              :complete       true,
                              :op             "update",
                              :prev_complete  false
                              :deleted        false
                              :title          "second"}

                             {:burndown_total 3,
                              :change         1,
                              :complete       false,
                              :op             "insert",
                              :prev_complete  nil,
                              :deleted        false
                              :title          "fourth"}

                             {:burndown_total 2,
                              :change         -1,
                              :complete       true,
                              :op             "update",
                              :prev_complete  false,
                              :deleted        false
                              :title          "third"}

                             {:burndown_total 3,
                              :change         1,
                              :complete       false,
                              :op             "insert",
                              :prev_complete  nil,
                              :deleted        false
                              :title          "fifth"}

                             {:burndown_total 2,
                              :change         -1,
                              :complete       true,
                              :op             "update",
                              :prev_complete  false,
                              :deleted        false
                              :title          "fifth"}

                             {:burndown_total 1,
                              :change         -1,
                              :complete       false,
                              :op             "update",
                              :prev_complete  false,
                              :deleted        true
                              :title          "fourth"}

                             {:burndown_total 0,
                              :change         -1,
                              :complete       true,
                              :op             "update",
                              :prev_complete  false,
                              :deleted        false
                              :title          "first"}

                             {:burndown_total 0,
                              :change         0,
                              :complete       true,
                              :deleted        true,
                              :op             "update",
                              :prev_complete  true,
                              :title          "first"}]]

        (t/is (= expected-events
                 (map strip-burndown-event (todos/burndown db valid-email))))))))
