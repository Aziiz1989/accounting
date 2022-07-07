(ns core.clj.ui
  (:require [hiccup.page :as html]))

(def cols
  [:Date :Account :Credit :Debit])

(def data
  [{:Date "2022-01-01"
    :Account :cash
    :Credit nil
    :Debit 500}
   {:Date "2022-01-01"
    :Account :cash
    :Credit 400
    :Debit nil}])

(def account-ids
  {:cash 100
   :common-stock 300
   :equipment 101
   :accounts-paypable 200
   :expenses 201})

(defn table [cols data]
  ;; [:div {:class "shadow-sm overflow-hidden my-8 mx-3 w-3/5 inset-y-0 right-0  fixed"}
    [:table {:class "table"}
     [:thead
      (into [:tr] (mapv #(into [] [:th %]) cols))]
     (->> data
          (map vals)
          (mapv (fn [m] (into [:tr] (mapv #(into [] [:td %]) m))))
          (into [:tbody]))])

(defn input []
  [:div {:class "field"}
   [:div#innerform {:class "control"}
   [:input {:type "number" :name "amount" :class "input"}]
    [:div  {:class "select"} (into [:select {:name "account"}] (mapv #(into [] [:option %]) (keys account-ids)))]]])

(defn form []
  [:form#myform 
  (input)
   ])

(defn submitted [values]
  [:h3 {:hx-get "/input" :hx-trigger "mouseenter" :hx-swap "outerHTML"}
   (str "form submitted with values " values)])

(defn base-page
  [form table]
  (html/html5
   {:class "bg-white" :lang "eng"}
   [:head
    [:link {:rel "stylesheet"
            :href "https://cdn.jsdelivr.net/npm/bulma@0.9.4/css/bulma.min.css"}]
    (html/include-js "https://unpkg.com/htmx.org@1.7.0")
    [:title "testing htmx"]]
   [:body
    [:div {:class "tile is-ancestor"}
     [:div {:class "tile is-parent is-6"}
      [:div
       form
       [:button {:hx-post "/submitted" :hx-swap "beforeend" :hx-target "#myform" :hx-include "#myform" :class "button"} "Submit"]]]
     [:div {:class "tile is-parent"} table]]]))





