(ns gaia.store
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io]
   [protograph.kafka :as kafka]))

(defn join-path
  [fragments]
  (let [separator (java.io.File/separator)]
    (string/join separator fragments)))

(defn snip
  [s prefix]
  (if (.startsWith s prefix)
    (.substring s (inc (.length prefix)))
    s))

(defn file->key
  [root file]
  (let [path (.getAbsolutePath file)]
    (snip path root)))

(defn dir-for
  [path]
  (.substring
   path 0
   (.lastIndexOf path java.io.File/separator)))

(defn ensure-path
  [path]
  (let [dir (io/file (dir-for path))]
    (.mkdirs dir)))

(defprotocol Store
  (present? [store container key])
  (computing? [store container key])
  (protocol [store])
  (url-root [store container])
  ;; (put-key [store key])
  ;; (get-key [store key])
  (delete [store container key])
  (existing-keys [store container]))

(defprotocol Bus
  (put [bus topic message])
  (listen [bus topic fn]))

(defprotocol Executor
  (execute [executor key inputs outputs command])
  (status [executor task-id]))

(deftype FileStore [root]
  Store
  (present?
    [store container key]
    (let [path (join-path [root (name container) (name key)])
          file (io/file path)]
      (.exists file)))
  (computing? [store key] false)
  (protocol [store] "file://")
  (url-root [store] root)
  (delete [store key]
    (io/delete-file (join-path [root (name key)])))
  (existing-keys
    [store]
    (let [files (kafka/dir->files root)]
      (mapv (partial file->key root) files))))

(defn absent?
  [store key]
  (not (present? store key)))

(defn existing-paths
  [store]
  (let [existing (existing-keys store)]
    (into
     {}
     (map
      (fn [key]
        [key {:url (join-path [(url-root store) key]) :state :complete}])
      existing))))

(defn load-file-store
  [config]
  (FileStore. (:root config)))



