(ns todo-snap.boundary.todos
  (:require duct.database.sql
            [honey.sql :as sql]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io]))

(defn- honey-query [db-spec sql-map]
  (jdbc/query db-spec (sql/format sql-map)))

(defprotocol Todos
  "A repository for users' todos"

  (create-todo [db todo]
    "Inserts a todo for a given user")

  (list-todos [db email]
    "Lists undeleted todos for a given user")

  (update-todo [db params]
    "Updates a todo by id and owner email")

  (delete-todo [db params]
    "Deletes a todo by id and owner email")

  (summary [db email]
    "Gets summary information (complete/incomplete counts) for a given user")

  (burndown [db email]
    "Gets complete state change events for a given user"))

(def public-todo-cols
  [:id :title :complete])

(defn- where-user-todos [email]
  [:and
   [[:not :deleted]]
   [:= :email email]])

(defn- update-sql
  [id email set-clause]
  {:update    :todos,
   :set       set-clause
   :where     (conj (where-user-todos email) [:= :id id])
   :returning public-todo-cols})

(defn- perform-update [db id email set-clause]
  (->> set-clause
       (update-sql id email)
       (honey-query db)
       (first)))

(def ^:private burndown-query
  (slurp (io/resource "todo_snap/queries/burndown.sql")))

(extend-protocol Todos
  duct.database.sql.Boundary

  (create-todo [{db :spec} todo-params]
    (jdbc/insert! db :todos todo-params))

  (list-todos [{db :spec} email]
    (honey-query db {:from   [:todos]
                     :where  (where-user-todos email)
                     :select public-todo-cols}))

  (update-todo [{db :spec} {:keys [id complete title email]}]
    (let [set-clause (into {}
                           (filter (comp some? second))
                           [[:complete complete] [:title title]])]
      (perform-update db id email set-clause)))

  (delete-todo [{db :spec} {:keys [id email]}]
    (perform-update db id email {:deleted true}))

  (summary [{db :spec} email]
    (honey-query db {:from     [:todos]
                     :where    (where-user-todos email)
                     :group-by [:complete]
                     :select   [:complete [[:count :*]]]}))

  (burndown [{db :spec} email]
    (jdbc/query db [burndown-query email])))
