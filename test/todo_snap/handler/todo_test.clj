(ns todo-snap.handler.todo-test
  (:require [clojure.test :as t]
            [todo-snap.boundary.todos :as boundary]
            [todo-snap.handler.todos :as handler]))

(defrecord MockDB [opts])

(extend-protocol boundary/Todos
  MockDB
  (create-todo [{opts :opts} todo] (:create opts))
  (list-todos [{opts :opts} email] (:list opts))
  (update-todo [{opts :opts} params] (:update opts))
  (delete-todo [{opts :opts} params] (:delete opts))
  (summary [{opts :opts} email] (:summary opts))
  (burndown [{opts :opts} email] (:burndown opts)))

(t/deftest todos-test
  (t/testing "list todos"
    (t/testing "with a valid email"
      (let [db    (->MockDB {:list []})
            email "valid@gmail.com"]
        (t/is (= [:ataraxy.response/ok []]
                 (handler/list-todos db email)))))

    (t/testing "with an invalid email"
      (let [db    (->MockDB {:list []})
            email "invalid"]
        (t/is (= [:ataraxy.response/bad-request "must be a valid email address"]
                 (handler/list-todos db email))))))

  (t/testing "create todo"
    (t/testing "with valid params"
      (let [db     (->MockDB {:create {}})
            params {:email "valid@gmail.com" :title "bake cookies"}]
        (t/is (= [:ataraxy.response/ok {}]
                 (handler/create-todo db params)))))

    (t/testing "with invalid params"
      (let [db     (->MockDB {:create {}})
            params {:email ""}]
        (t/is (= [:ataraxy.response/bad-request {:email ["should match regex"]
                                                 :title ["missing required key"]}]
                 (handler/create-todo db params))))))

  (t/testing "update todo"
    (t/testing "with valid params"
      (let [db     (->MockDB {:update {}})
            params {:id       "50a5d34d-b023-4543-a7f1-f4f2ccabcaf8"
                    :email    "valid@gmail.com"
                    :complete "true"
                    :title    "bake cookies"}]
        (t/is (= [:ataraxy.response/ok {}]
                 (handler/update-todo db params)))))

    (t/testing "with invalid uuid"
      (let [db     (->MockDB {:update {}})
            params {:id       "not a uuid"
                    :email    "valid@gmail.com"
                    :complete "true"
                    :title    "bake cookies"}]
        (t/is (= [:ataraxy.response/bad-request {:id ["should be a uuid"]}]
                 (handler/update-todo db params)))))

    (t/testing "with invalid params"
      (let [db     (->MockDB {:update {}})
            params {:id       "50a5d34d-b023-4543-a7f1-f4f2ccabcaf8"
                    :email    "invalid"
                    :complete "truthy"}]
        (t/is (= [:ataraxy.response/bad-request {:complete ["should be either true or false"]
                                                 :email    ["should match regex"]}]
                 (handler/update-todo db params))))))

  (t/testing "delete todo"
    (t/testing "with valid params"
      (let [db     (->MockDB {:delete {}})
            params {:id    "50a5d34d-b023-4543-a7f1-f4f2ccabcaf8"
                    :email "valid@gmail.com"}]
        (t/is (= [:ataraxy.response/no-content {}]
                 (handler/delete-todo db params)))))

    (t/testing "with invalid email"
      (let [db     (->MockDB {:delete {}})
            params {:id    "50a5d34d-b023-4543-a7f1-f4f2ccabcaf8"
                    :email "not an email"}]
        (t/is (= [:ataraxy.response/bad-request {:email ["should match regex"]}]
                 (handler/delete-todo db params)))))

    (t/testing "with invalid uuid"
      (let [db     (->MockDB {:delete {}})
            params {:id    "not a uuid"
                    :email "valid@gmail.com"}]
        (t/is (= [:ataraxy.response/bad-request {:id ["should be a uuid"]}]
                 (handler/delete-todo db params)))))))
