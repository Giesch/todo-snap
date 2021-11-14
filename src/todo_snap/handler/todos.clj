(ns todo-snap.handler.todos
  (:require [ataraxy.response :as response]
            [integrant.core :as ig]
            [malli.core :as m]
            [malli.transform :as mt]
            [todo-snap.boundary.todos :as todos]))

;; validation

(def create-todo-params
  [:map
   [:title :string]])

(def valid-create?
  (m/validator create-todo-params))

(defn strip-params [schema params]
  (m/decode schema params (mt/strip-extra-keys-transformer)))

;; handlers

(defmethod ig/init-key :todo-snap.handler.todos/list
  [_ {:keys [db]}]
  (fn [{[_] :ataraxy/result}]
    [::response/ok (todos/list-todos db)]))

(defmethod ig/init-key :todo-snap.handler.todos/create
  [_ {:keys [db]}]
  (fn [{[_ params] :ataraxy/result}]
    (if-not (valid-create? params)
      [::response/bad-request]

      (let [stripped-params (strip-params create-todo-params params)
            created-todo (todos/create-todo db stripped-params)]
        [::response/ok created-todo]))))
