(ns todo-snap.boundary.todos
  (:require duct.database.sql
            [honey.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn- honey-query [db sql-map]
  (jdbc/query db (sql/format sql-map)))

(defprotocol Todos
  (create-todo [db todo])
  (list-todos [db]))

(extend-protocol Todos
  duct.database.sql.Boundary

  (create-todo [{db :spec} todo-params]
    (jdbc/insert! db :todos todo-params))

  (list-todos [{db :spec}]
    (honey-query db {:from   [:todos]
                     :select [:id :title]})))
