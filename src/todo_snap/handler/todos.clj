(ns todo-snap.handler.todos
  (:require [ataraxy.response :as response]
            [integrant.core :as ig]
            [malli.core :as m]
            [malli.error :as me]
            [malli.transform :as mt]
            [todo-snap.boundary.todos :as todos]))

;; validation

(def create-todo-params
  [:map
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
    [:complete {:optional true} [:enum "true" "false"]]
    [:title {:optional true} :string]]

   [:fn {:error/message "at least one property is required"}
    '(fn [{:keys [title complete]}]
       (some some? [title complete]))]])

(def valid-update?
  (m/validator update-todo-params))

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

(defn list-todos [db]
  [::response/ok (todos/list-todos db)])

(defmethod ig/init-key :todo-snap.handler.todos/list
  [_ {:keys [db]}]
  (fn [{[_] :ataraxy/result}]
    (list-todos db)))

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

(comment
  (parse-update {:id "1d3806f8-1db8-41fd-9218-5f07c992f3a2" :complete "true"})
  (parse-update {:id "1d3806f8-1db8-41fd-9218-5f07c992f3a2"})
  (parse-update {:id "1d3806f8-1db8-41fd-9218-5f07c992f3a2" :complete nil})
  ;;
  )

(defn update-todo [db params]
  (cond
    (not (valid-update? params))
    [::response/bad-request (malli-error update-todo-params params)]

    (nil? (parse-uuid (:id params)))
    [::response/bad-request "id must be a valid uuid"]

    :else [::response/ok (->> params
                              (parse-update)
                              (todos/update-todo db))]))

(defmethod ig/init-key :todo-snap.handler.todos/update
  [_ {:keys [db]}]
  (fn [{[_ params] :ataraxy/result}]
    (update-todo db params)))
