(ns todo-snap.handler.todos
  (:require [ataraxy.response :as response]
            [integrant.core :as ig]
            [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [todo-snap.boundary.todos :as todos]))

;; validation

(def valid-email-schema
  [:and
   {:error/message "must be a valid email address"}
   ;; https://stackoverflow.com/questions/33736473/how-to-validate-email-in-clojure
   :string [:re #".+\@.+\..+"]])

(def create-todo-params
  [:map
   [:email valid-email-schema]
   [:title :string]])

(def valid-create?
  (m/validator create-todo-params))

(defn- parse-uuid [id]
  (try (java.util.UUID/fromString id)
       (catch IllegalArgumentException _ nil)))

(def update-todo-params
  [:and
   [:map
    [:id :string]
    [:email valid-email-schema]
    [:complete {:optional true} [:enum "true" "false"]]
    [:title {:optional true} :string]]

   [:fn {:error/message "at least one property is required"}
    '(fn [{:keys [title complete]}]
       (some some? [title complete]))]])

(def valid-update?
  (m/validator update-todo-params))

(def valid-email?
  (m/validator valid-email-schema))

(def delete-todo-params
  [:map
   [:id :string]
   [:email valid-email-schema]])

(def valid-delete?
  (m/validator delete-todo-params))

(defn strip-params
  "Removes all extra keys from params according to a malli schema."
  [params schema]
  (m/decode schema params (mt/strip-extra-keys-transformer)))

(defn- malli-error [schema params]
  (-> schema
      (m/explain params)
      (me/humanize)))

;;;; handlers
;; NOTE these defmethods might be a good candidate for a macro

;; list

(defn list-todos [db email]
  (if (valid-email? email)
    [::response/ok (todos/list-todos db email)]
    [::response/bad-request "must be a valid email address"]))

(defmethod ig/init-key :todo-snap.handler.todos/list
  [_ {:keys [db]}]
  (fn [{[_ email] :ataraxy/result}]
    (list-todos db email)))

;; create

(defn create-todo [db params]
  (if-not (valid-create? params)
    [::response/bad-request (malli-error create-todo-params params)]

    (let [valid-params (strip-params params create-todo-params)]
      [::response/ok (todos/create-todo db valid-params)])))

(defmethod ig/init-key :todo-snap.handler.todos/create
  [_ {:keys [db]}]
  (fn [{[_ params] :ataraxy/result}]
    (create-todo db params)))

;; update

(defn- to-nullable-bool [s]
  (if s (= s "true") nil))

(defn- parse-update [params]
  (some-> params
          (strip-params update-todo-params)
          (update :id parse-uuid)
          (update :complete to-nullable-bool)))

(defn- parse-delete [params]
  (some-> params
          (strip-params delete-todo-params)
          (update :id parse-uuid)))

(defn update-todo
  "Updates the title or completed status of a todo belonging to a particular user"
  [db params]
  (cond
    (not (valid-update? params))
    [::response/bad-request (malli-error update-todo-params params)]

    (nil? (parse-uuid (:id params)))
    [::response/bad-request "id must be a valid uuid"]

    :else (if-let [result (->> params
                               (parse-update)
                               (todos/update-todo db))]
            [::response/ok result]
            [::response/not-found])))

(defmethod ig/init-key :todo-snap.handler.todos/update
  [_ {:keys [db]}]
  (fn [{[_ params] :ataraxy/result}]
    (update-todo db params)))

(defn delete-todo
  "Soft deletes a todo belonging to a particular user"
  [db params]
  (cond
    (not (valid-delete? params))
    [::response/bad-request (malli-error delete-todo-params params)]

    (nil? (parse-uuid (:id params)))
    [::response/bad-request "id must be a valid uuid"]

    :else (if-let [result (->> params
                               (parse-delete)
                               (todos/delete-todo db))]
            [::response/ok result]
            [::response/not-found])))

(defmethod ig/init-key :todo-snap.handler.todos/delete
  [_ {:keys [db]}]
  (fn [{[_ params] :ataraxy/result}]
    (delete-todo db params)))

(defn- get-count
  "Gets the count matching complete from the results-set results"
  [results complete]
  (->> results
       (filter (fn [row]
                 (= complete (:complete row))))
       (first)
       (:count)))

(defn- summary-json
  "Rearranges the summary results to be returned as json"
  [results]
  {:complete   (get-count results true)
   :incomplete (get-count results false)})

(defn get-summary
  "Counts complete and incomplete todos for a single user"
  [db email]
  (if (valid-email? email)
    [::response/ok (summary-json (todos/summary db email))]
    [::response/bad-request "must be a valid email address"]))

(defmethod ig/init-key :todo-snap.handler.todos/summary
  [_ {:keys [db]}]
  (fn [{[_ email] :ataraxy/result}]
    (get-summary db email)))
