(ns todo-snap.boundary.todos
  (:require duct.database.sql
            [honey.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn- honey-query [db-spec sql-map]
  (jdbc/query db-spec (sql/format sql-map)))

(defprotocol Todos
  (create-todo [db todo])
  (list-todos [db email])
  (update-todo [db params]))

(def public-cols
  [:id :title :complete])

(extend-protocol Todos
  duct.database.sql.Boundary

  (create-todo [{db :spec} todo-params]
    (jdbc/insert! db :todos todo-params))

  (list-todos [{db :spec} email]
    (honey-query db {:from   [:todos]
                     :where [:= :email email]
                     :select public-cols}))

  (update-todo [{db :spec} {:keys [id complete title email]}]
    (let [set-clause (into {}
                           (filter (comp some? second))
                           [[:complete complete] [:title title]])]

      (first (honey-query db {:update    :todos,
                              :set       set-clause
                              :where     [:and [:= :id id] [:= :email email]]
                              :returning public-cols})))))
