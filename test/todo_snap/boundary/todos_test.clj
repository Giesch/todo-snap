(ns todo-snap.boundary.todos-test
  (:require [todo-snap.boundary.todos :as todos]
            [duct.core :as duct]
            [duct.database.sql :as duct-sql]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as rag-jdbc]
            [integrant.core :as ig]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]))

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
         migrations     (ig/init-key :duct.migrator.ragtime/resources {:path "todo_snap/migrations"})]

     (ragtime/migrate-all (rag-jdbc/sql-database db-spec) {} migrations)

     (duct-sql/->Boundary db-spec))))

(def ^:private valid-email "valid@gmail.com")

(defn- insert-todo [db title]
  (first (todos/create-todo db {:title title :email valid-email})))

(defn- complete-todo [db todo]
  (todos/update-todo db {:complete true
                         :email    valid-email
                         :id       (:id todo)}))

(t/deftest todos-boundary-test
  (t/testing "create and complete a todo"
    (let [db           (make-test-db)
          cookies-todo (insert-todo db "bake cookies")]

      (complete-todo db cookies-todo)

      (let [listed-todos  (todos/list-todos db valid-email)
            expected-todo (assoc (select-keys cookies-todo todos/public-todo-cols) :complete true)]
        (t/is (= listed-todos [expected-todo])))))

  (t/testing "todos summary"
    (let [db                (make-test-db)
          cookies-todo      (insert-todo db "bake cookies")]
      (complete-todo db cookies-todo)
      (insert-todo db "eat cookies")
      (insert-todo db "bake more cookies")

      (t/is (= [{:complete false :count 2} {:complete true :count 1}]
               (todos/summary db valid-email))))))
