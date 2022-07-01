(ns core.clj.db.queries
  (:require [xtdb.api :as xt]))

(defn get-all-entries [node]
  (xt/q (xt/db node) '{:find [{:id id
                               :date date
                               :acc-num acc-num
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
                               [?e :amount amount]
                               [?e :id acc-num]
                               [?e :date date]
                               ]}))
