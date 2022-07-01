(ns core.clj.routes
  (:require
   [core.clj.db.queries :as db]
   [core.clj.handlers :as handlers]))


(defn parse-string-to-date [date]
  (prn date)
  (.parse
   (java.text.SimpleDateFormat. "yyyy-MM-dd") date))

(defn parse-dates [body]
  (mapv #(assoc % :date (parse-string-to-date (:date %))) body))


(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body (str "Hello " name "!\n")}))

(defn get-all-entries [request]
  ;;(prn request)
    {:status 200
     :body
     (db/get-all-entries (:node request))})

(defn post-entries
 "I want to get a vector of maps, each map is a post to an account
  three steps are done:
  1- add account id to each map based on account name
  2- prepare each map to be posted"
  [request]
  (let [node (:node request)
        body (-> request :parameters :body)
        entries body]
    (clojure.pprint/pprint body)
    (handlers/post-entries entries node)
    {:status 200
     :body "records added to db"}))

