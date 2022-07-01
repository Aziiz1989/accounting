(ns core.clj.handlers
  (:require
   [core.clj.db.utils :as db-utils]
   [xtdb.api :as xt]))

(defn post-entries [entries node]
  (xt/submit-tx node (db-utils/prepare-docs (map db-utils/add-ids-to-entries entries))))

