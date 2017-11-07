(ns gaia.swift
  (:require
   [clojure.string :as string]
   [gaia.store :as store])
  (:import
   [org.javaswift.joss.client.factory AccountFactory]))

(defn get-container
  [account container]
  (.getContainer account container))

(defn container-exists?
  [container]
  (.exists container))

(defn swift-connect
  [{:keys [username password url method tenant-id tenant-name region container root]}]
  (let [factory (AccountFactory.)]
    (.setUsername factory username)
    (.setPassword factory password)
    (.setAuthUrl factory url)
    (if region (.setPreferredRegion factory region))
    (if tenant-id (.setTenantId factory tenant-id))
    (if tenant-name (.setTenantName factory tenant-name))
    (if method (.setAuthenticationMethod factory method))
    (let [account (.createAccount factory)
          container (get-container account container)]
      {:account account
       :container container
       :root root})))

(defn get-object
  [{:keys [container]} key]
  (.getObject container key))

(defn key-exists?
  [{:keys [container] :as swift} key]
  (let [object (get-object swift key)]
    (.exists object)))

(def encoded-slash #"%2F")

(defn get-path
  [object]
  (string/replace (.getPath object) encoded-slash "/"))

(defn all-keys
  [{:keys [container] :as swift}]
  (loop [dirs (.listDirectory container)
         all []]
    (if (empty? dirs)
      all
      (let [head (first dirs)
            remaining (rest dirs)]
        (if (.isDirectory head)
          (recur
           (concat remaining (.listDirectory container head))
           all)
          (recur
           remaining
           (conj all (get-path head))))))))

(deftype SwiftStore [swift]
  store/Store
  (present?
    [store key]
    (key-exists? swift key))
  (computing? [store key] false)
  (url-root [store] (:root swift))
  (existing-keys
    [store]
    (all-keys swift)))

(defn load-swift-store
  [config]
  (let [swift (swift-connect config)]
    (SwiftStore. swift)))

