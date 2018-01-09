(defn gaia.db
  (:require
   [ophion.mongo :as mongo]
   [ophion.aggregate :as aggregate]))

(defn connect!
  "ensures indexes exist"
  [config]
  (let [db (mongo/connect! config)]
    (mongo/build-indexes db mongo/base-indexes)
    db))

(defn process->vertex
  [key ])

(defn processes->graph
  [process data])
