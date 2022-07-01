(ns core.cljs.components.core
  (:require ["@mui/material/Button$default" :as button]
            ["@mui/x-data-grid" :refer (DataGrid)]))

(defn table
  "this is a documentation"
  [rows cols]
  [:> DataGrid {:rows rows
                :columns cols}])

(def simple-component
  [:> button {:variant :contained} "Hello World"])

