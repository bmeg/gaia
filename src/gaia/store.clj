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
  (present? [store key])
  (computing? [store key])
  (protocol [store])
  (url-root [store])
  ;; (put-key [store key])
  ;; (get-key [store key])
  ;; (delete-key [store key])
  (existing-keys [store]))

(defprotocol Bus
  (put [bus topic message])
  (listen [bus topic fn]))

(defprotocol Executor
  (execute [executor key inputs outputs command])
  (status [executor task-id]))

(deftype FileStore [root]
  Store
  (present?
    [store key]
    (let [path (join-path [root (name key)])
          file (io/file path)]
      (.exists file)))
  (computing? [store key] false)
  (protocol [store] "file://")
  (url-root [store] root)
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



