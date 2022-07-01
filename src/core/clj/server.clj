(ns core.clj.server
  (:require
   [reitit.ring :as ring]
   [reitit.coercion.spec]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.dev.pretty :as pretty]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
            ;; Uncomment to use
   [reitit.ring.middleware.dev :as dev]
   [ring.adapter.jetty :as jetty]
   [muuntaja.core :as m]
   [xtdb.api :as xt]
   [aero.core :as aero]
   [integrant.core :as ig]
   [core.clj.routes :as routes]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

(defn parse-string-to-date [date]
  (prn date)
  (.parse
   (java.text.SimpleDateFormat. "yyyy-MM-dd") date))

(s/def ::account keyword?)
(s/def ::type keyword?)
(s/def ::amount number?)
(s/def ::date (st/spec
               {:spec
                (s/and string? #(re-matches #"^\d{4}\-(0?[1-9]|1[012])\-(0?[1-9]|[12][0-9]|3[01])$" %))
                :decode/string (fn [_ date-str] parse-string-to-date date-str)}))
(s/def ::post-entry-body (s/coll-of (s/keys :req-un [::account ::type ::amount ::date]) :kind vector?))

(comment

  (st/coerce ::post-entry-body  [{:account :cash :type :credit :amount 109756 :date "2022-01-01"}] st/string-transformer)
  (s/valid? ::post-entry-body [{:account :cash :type :credit :amount 109756 :date "2022-01-01"}
                               {:account :cash :type :credit :amount 109756 :date "2022-01-01"}])

  (s/valid? ::date "22-22-22")
  )

(def jdbc-url (:db_url (aero/read-config "config.edn")))

(def inject-db-conn-middleware
  {:name ::node
   :compile (fn [{:keys [node]} _]
              (fn [handler]
                (fn [request]
                  (handler (assoc request :node node)))))})

(def config
  {::app {:node (ig/ref ::node)}
   ::server {:app (ig/ref ::app)}
   ::node {:jdbc-url jdbc-url}})

(defmethod ig/init-key ::app [_ {:keys [node]}]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "my-api"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/get-all-entries"
      {:get {:summary "get all journal entries"
             :handler routes/get-all-entries}}]
     ["/post-entries"
      {:post {:summary "post entries"
              :parameters {:body ::post-entry-body}
              ;;:responses {200 {:body {:account string? :type string? :amount number?}}}
              :handler routes/post-entries}}]]
    {;;:reitit.middleware/transform dev/print-request-diffs ;; pretty diffs
       ;;:validate spec/validate ;; enable spec validation for route data
       ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
     :exception pretty/exception
     :data {
            :coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :node node
            :middleware [;; swagger feature
                         swagger/swagger-feature
                         muuntaja/format-middleware
                         coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         inject-db-conn-middleware
                           ]}})
   (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/"
         :config {:validatorUrl nil
                  :operationsSorter "alpha"}})
      (ring/create-default-handler))))


(defmethod ig/init-key ::node [_ {:keys [jdbc-url]}]
  (xt/start-node {:xtdb.jdbc/connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                               :db-spec {:jdbcUrl jdbc-url}}
                   :xtdb/tx-log {:xtdb/module 'xtdb.jdbc/->tx-log
                                 :connection-pool :xtdb.jdbc/connection-pool}
                   :xtdb/document-store {:xtdb/module 'xtdb.jdbc/->document-store
                                         :connection-pool :xtdb.jdbc/connection-pool}}))


(defmethod ig/init-key ::server [_ {:keys [app]}]
  (jetty/run-jetty app {:port 3000 :join? false}))

(defmethod ig/halt-key! ::server [_ system]
  (prn system)
  (.stop system))


(defmethod ig/halt-key! ::node [_ system]
  (prn system)
  (.close system))

(def system
  (ig/init config))

(comment
  (ig/halt! system)

  (.close (::node system))
  (.stop (::server system))

  ((::app system) {:request-method :post :uri "/post-entries"
                   :body-params [{:account :cash :type :credit :amount 12345 :date "2021-01-01"}]})
  
  ((::app system) {:request-method :get :uri "/get-all-entries"})
  (.close node)

  (xt/q (xt/db (::node system)) '{:find [(pull ?e [*])]
                                  :where [[?e :xt/id ?id]
                                          [?e :amount amount]]})

  (xt/q (xt/db (::node system)) '{:find [p]
                                  :where [[p :amount 1000]]})

  (((partial routes/post-entries (::node system))) {:parameters {:body {:account "aziz"}}})

  (core.clj.db.queries/get-all-entries (::node system))

  (m/default-options)

  )

