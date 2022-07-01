(ns core.clj.budget
  (:require [core.clj.server :refer [node]]
            [xtdb.api :as xt]))

(def account-ids
  {:cash 100
   :common-stock 300
   :equipment 101
   :accounts-paypable 200})

(defn prepare-docs [docs]
  (mapv (fn [doc]
          [::xt/put doc]) docs))

(defn add-to-journal [{:keys [account] :as entries}]
  (assoc entries :xt/id (account-ids account))
  )



(comment
(add-to-journal {:account :cash
               :type :debit
               :date #inst "2022-01-03"
                 :amount 20000})
(account-ids :cash)
(add-to-journal journal)
  )
(def journal [{:account :cash
               :type :debit
               :date #inst "2022-01-03"
               :amount 20000}
              {:account :common-stock
               :type :credit
               :date #inst "2022-01-03"
               :amount 20000}
              {:account :equipment
               :type :debit
               :date #inst "2022-01-05"
               :amount 3500}
              {:account :account-payable
               :type :credit
               :date #inst "2022-01-05"
               :amount 3500}])

(comment
  jdbc-url
  (xt/submit-tx node (prepare-docs [(add-to-journal {:account :common-stock
                                                    :type :credit
                                                    :amount 4000
                                                    :date #inst "2022-01-10"})]))
  (xt/entity-history (xt/db node) 100 :desc {:with-docs? true})
  (xt/q (xt/db node) '{:find [(pull ?e [*])]
                       :where [[?e :xt/id ?id]]})

  (xt/q (xt/db node) '{:find [{:id id
                              :account account
                              :credit (if (= trx-type :credit)
                                amount
                                nil)
                              :debit (if (= trx-type :debit)
                                amount
                                nil)}]
                       :where [[?e :xt/id id]
                               [?e :account account]
                               [?e :type trx-type]
                               [?e :amount amount]]})
  )
