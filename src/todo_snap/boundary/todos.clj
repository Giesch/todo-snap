(ns todo-snap.boundary.todos
  (:require duct.database.sql
            [honey.sql :as sql]
            [clojure.java.jdbc :as jdbc]))

(defn- honey-query [db-spec sql-map]
  (jdbc/query db-spec (sql/format sql-map)))

(defprotocol Todos
  (create-todo [db todo])
  (list-todos [db email])
  (update-todo [db params])
  (delete-todo [db params]))

(def public-cols
  [:id :title :complete])

(defn- update-sql
  [id email set-clause]
  {:update    :todos,
   :set       set-clause
   :where     [:and [:= :id id] [:= :email email]]
   :returning public-cols})

(defn- perform-update [db id email set-clause]
  (->> set-clause
       (update-sql id email)
       (honey-query db)
       (first)))

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
      (perform-update db id email set-clause)))

  (delete-todo [{db :spec} {:keys [id email]}]
    (perform-update db id email [:deleted true])))
