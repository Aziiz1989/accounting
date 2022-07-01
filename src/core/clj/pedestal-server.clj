(ns core.clj.server
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.test :refer [response-for raw-response-for servlet-response-for]]
   [io.pedestal.http.ring-middlewares :as ring]
   [io.pedestal.http.content-negotiation :as conneg]
   [xtdb.api :as xt]
   [aero.core :as aero]
   [integrant.core :as ig]
   [core.clj.routes :as routes]))

(def jdbc-url (:db_url (aero/read-config "config.edn")))

(def supported-types ["application/edn"]) 

(def content-neg-intc (conneg/negotiate-content supported-types))

(def config
  {::service-map {:env                 :dev
                  ::http/routes        (ig/ref ::routes)
                  ::http/join? false
              ;;::http/resource-path "/public"
                  ::http/type          :jetty
                  ::http/port          8080
                 ::http/allowed-origins {:creds true :allowed-origins (constantly true)}
                  }
   ::server {:service-map (ig/ref ::service-map)}
   ::node {:jdbc-url jdbc-url}
   ::routes {:node (ig/ref ::node)}})

(defmethod ig/init-key ::server [_ {:keys [service-map] :as pedestal}]
  ;;(prn service-map)
  (-> service-map
      http/default-interceptors
      http/dev-interceptors
      http/create-server 
      http/start
      ((partial assoc pedestal :service))))

(defmethod ig/init-key ::service-map [_ x]
  x)

(defmethod ig/halt-key! ::server [_ {:keys [service] :as pedestal}]
  (http/stop service)
  (assoc pedestal :service nil))


(defmethod ig/init-key ::node [_ {:keys [jdbc-url]}]
  (xt/start-node {:xtdb.jdbc/connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                              :db-spec {:jdbcUrl jdbc-url}}
                  :xtdb/tx-log {:xtdb/module 'xtdb.jdbc/->tx-log
                                :connection-pool :xtdb.jdbc/connection-pool}
                  :xtdb/document-store {:xtdb/module 'xtdb.jdbc/->document-store
                                        :connection-pool :xtdb.jdbc/connection-pool}}))

(defmethod ig/halt-key! ::node [_ {:keys [node] :as pedestal}]
  (.close node)
  (assoc pedestal ::node nil))

(defmethod ig/init-key ::routes [_ {:keys [node] :as opts}]
   #{["/greet" :get `routes/hello-world]
     ["/get-all-entries" :get (partial routes/get-all-entries (:node opts)) :route-name :get-all-entries]
     ["/post-entries" :post [(ring/multipart-params) (body-params/body-params) (partial routes/post-entries node)] :route-name :post-entries]})

(def system
  (ig/init config))

(comment
  (ig/halt! system)
  )
;;(.close (::node system))

(comment
  (def node (::node system))
  (xt/entity-history (xt/db node) 100 :desc {:with-docs? true})

  (def query (xt/q (xt/db node) '{:find [(pull ?e [*])]
                                  :where [[?e :xt/id ?id]]}))
  (mapv first query)

  (def tempserv (::http/service-fn (http/create-servlet (::service-map system))))
  (:body (response-for tempserv
                        :post "/post-entries"
                        :headers {"Content-Type" "application/edn"} 
                        :body "[{:account :cash :amount 400 :type :debit}]"
                        :edn-params [{:account :cash :amount 400 :type :debit}]))
  system


  )
