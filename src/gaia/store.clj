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
  (key->url [store key])
  ;; (put-key [store key])
  ;; (get-key [store key])
  (delete [store key])
  (existing-keys [store]))

(defprotocol Bus
  (put [bus topic message])
  (listen [bus topic fn]))

(defprotocol Executor
  (execute [executor key inputs outputs command])
  (status [executor task-id]))

(deftype FileStore [root container]
  Store
  (present?
    [store key]
    (let [path (join-path [root container (name key)])
          file (io/file path)]
      (.exists file)))
  (computing? [store key] false)
  (protocol [store] "file://")
  (url-root [store] (join-path [root container]))
  (key->url [store key]
    (str (protocol store) (join-path [root container (name key)])))
  (delete [store key]
    (io/delete-file (join-path [root container (name key)])))
  (existing-keys
    [store]
    (let [base (url-root store)
          files (kafka/dir->files base)]
      (mapv (partial file->key base) files))))

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
  [config container]
  (FileStore. (:root config) container))

(defn file-store-generator
  [config]
  (fn [container]
    (load-file-store config container)))
