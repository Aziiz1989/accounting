(ns core.cljs.app
  (:require
   ["@mui/material/Box$default" :as box]
   ["@mui/material/Grid$default" :as grid]
   [ajax.core :refer [GET]]
   [ajax.edn :refer [edn-response-format]]
   [core.cljs.components.core :as components]
   [reagent.core :as r]
   [reagent.dom :as rd]))


(defonce all-entries (r/atom []))
(def errors (r/atom []))

(def entries-table-columns
  [{:headerName "Account Number"
    :field :acc-num
    :flex 1
    }
   {:headerName "Account"
    :field :account
    :flex 1
    }
   {:headerName "Credit"
    :field :credit
    :flex 1
    }
   {:headerName "Debit"
    :field :debit
    :flex 1
    }])

(def get-data
 (GET 
  "http://127.0.0.1:8080/get-all-entries"
  {:handler #(swap! all-entries :entries %)
   :error-handler #(%)
   :response-format (edn-response-format)}))

(defn prep-data [data]
  (mapv #(assoc % :id (str (random-uuid))) (flatten (seq data))))


(defn main-component []
  (let [entries @all-entries]
    ;;[:div {:style {:height 200 :width "50%"}}
    [:> box {:sx {:flexGrow 1}}
     [:> grid {:container true :style {:height 600}}
      [:> grid {:item true :xs "4"} components/simple-component]
      [:> grid {:item true :xs "8"} (components/table (prep-data entries) entries-table-columns)]]]))

(defn init []
   get-data
  (rd/render [main-component] (js/document.getElementById "root")))


(comment
  (def my-atom (atom {:response nil :error nil}))
  (GET "localhost:9000/greet"
       {:handler (fn [response] (swap! myatom :key response))})

(defn handler [response]
  (swap! my-atom :response response))

(defn error-handler [{:keys [status status-text]}]
  (swap! my-atom :error [status status-text]))

(GET 
  "http://127.0.0.1:8080/get-all-entries"
  {:handler handler
   :error-handler error-handler
   :response-format (edn-response-format)})

  @my-atom
)
