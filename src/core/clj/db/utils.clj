(ns core.clj.db.utils
  (:require [xtdb.api :as xt]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def account-ids
  {:cash 100
   :common-stock 300
   :equipment 101
   :accounts-paypable 200
   :expenses 201})

(defn prepare-docs [docs]
  (mapv (fn [doc]
          [::xt/put doc]) docs))

(defn add-ids-to-entries [{:keys [account] :as entries}]
  (assoc entries :xt/id (uuid) :id (account-ids account)))

