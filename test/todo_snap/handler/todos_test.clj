(ns todo-snap.handler.todos-test
  (:require [clojure.test :as t]
            [todo-snap.boundary.todos :as boundary]
            [todo-snap.handler.todos :as handler]))

;; NOTE in a real app, we'd want to use a mocking library

(defrecord MockDB [opts])

(extend-protocol boundary/Todos
  MockDB
  (create-todo [{opts :opts} todo] (:create opts))
  (list-todos [{opts :opts} email] (:list opts))
  (update-todo [{opts :opts} params] (:update opts))
  (delete-todo [{opts :opts} params] (:delete opts))
  (summary [{opts :opts} email] (:summary opts))
  (burndown [{opts :opts} email] (:burndown opts)))

(t/deftest todos-handler-test
  (t/testing "list todos"
    (t/testing "with a valid email"
      (let [db    (->MockDB {:list []})
            email "valid@gmail.com"]
        (t/is (= [:ataraxy.response/ok []]
                 (handler/list-todos db email)))))

    (t/testing "with an invalid email"
      (let [db    (->MockDB {:list []})
            email "invalid"]
        (t/is (= [:ataraxy.response/bad-request "email must be a valid email address"]
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
                 (handler/delete-todo db params))))))

  (t/testing "summary"
    (t/testing "with invalid email"
      (let [db    (->MockDB {:summary []})
            email "nope"]
        (t/is (= [:ataraxy.response/bad-request "email must be a valid email address"]
                 (handler/get-summary db email)))))

    (t/testing "with valid email"
      (let [result-set [{:complete true :count 0}
                        {:complete false :count 1}]
            db    (->MockDB {:summary result-set})
            email "valid@gmail.com"]
        (t/is (= [:ataraxy.response/ok {:complete 0 :incomplete 1}]
                 (handler/get-summary db email))))))

  (t/testing "burndown"
    (t/testing "with invalid email"
      (let [db    (->MockDB {:burndown []})
            email "nope"]
        (t/is (= [:ataraxy.response/bad-request "email must be a valid email address"]
                 (handler/get-burndown db email)))))

    (t/testing "with valid email"
      (let [result-set [{:burndown-total 2
                         :change -1
                         :complete true
                         :id "50a5d34d-b023-4543-a7f1-f4f2ccabcaf8"
                         :op "update"
                         :prev-complete false
                         :title "FINISH HIM"
                         :updated-at "2021-11-18T07:04:20Z"}
                        {:burndown-total 3
                         :change 1
                         :complete false
                         :id "21481d03-a2f4-4ce5-97f3-0e45c4eedf38"
                         :op "insert"
                         :prev-complete nil
                         :title "bake more cookies"
                         :updated-at "2021-11-18T07:04:48Z"}]
            db    (->MockDB {:burndown result-set})
            email "valid@gmail.com"]
        (t/is (= [:ataraxy.response/ok
                  [{"burndownTotal" 2
                    "change" -1
                    "complete" true
                    "id" "50a5d34d-b023-4543-a7f1-f4f2ccabcaf8"
                    "op" "update"
                    "prevComplete" false
                    "title" "FINISH HIM"
                    "updatedAt" "2021-11-18T07:04:20Z"}
                   {"burndownTotal" 3
                    "change" 1
                    "complete" false
                    "id" "21481d03-a2f4-4ce5-97f3-0e45c4eedf38"
                    "op" "insert"
                    "prevComplete" nil
                    "title" "bake more cookies"
                    "updatedAt" "2021-11-18T07:04:48Z"}]]
                 (handler/get-burndown db email)))))))
