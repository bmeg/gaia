(ns gaia.config
  (:require
   [clojure.walk :as walk]
   [clojure.math.combinatorics :as combinatorics]
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [yaml.core :as yaml]
   [protograph.template :as template]
   [ophion.config :as config]
   [gaia.command :as command]
   [gaia.store :as store]
   [gaia.swift :as swift]
   [gaia.funnel :as funnel]))

(def config-keys
  [:variables
   :commands
   :processes
   :agents])

(defn parse-yaml
  [path]
  (yaml/parse-string (slurp path)))

(defn index-seq
  [f s]
  (reduce
   (fn [m x]
     (assoc m (f x) x))
   {} s))

(defn filter-map
  [f m]
  (into
   {}
   (filter
    (fn [[k v]]
      (f k v))
    m)))

(defn cartesian-map
  [map-of-seqs]
  (let [order (mapv vec map-of-seqs)
        heading (map first order)
        cartes (apply combinatorics/cartesian-product (map last order))]
    (map
     (fn [product]
       (into {} (map vector heading product)))
     cartes)))

(defn clean-string
  [s]
  (string/replace s #"[^a-zA-Z0-9\-]" ""))

(defn template-vars
  [{:keys [key vars inputs outputs] :as process}]
  (let [arrays (filter-map (fn [k v] (coll? v)) vars)
        series (cartesian-map arrays)]
    (map
     (fn [arrayed]
       (let [order (sort-by first arrayed)
             values (map (comp clean-string last) order)
             unique (string/join "-" (conj values key))
             env (walk/stringify-keys (merge vars arrayed))]
         (merge
          process
          {:key unique
           :vars env
           :inputs (template/evaluate-map inputs env)
           :outputs (template/evaluate-map outputs env)})))
     series)))

(defn transform-commands
  [commands]
  (index-seq
   (comp keyword :key)
   commands))

(defn transform-processes
  [commands processes]
  (let [templates (template/map-cat template-vars processes)]
    (template/map-cat
     (fn [process]
       (let [command (get-in commands (keyword (:command process)))]
         (command/apply-composite commands command process)))
     processes)))

(defn load-flow-config
  [path]
  (let [config
        (into
         {}
         (map
          (fn [key]
            (try
              [key (parse-yaml (str path "." (name key) ".yaml"))]
              (catch Exception e (do (log/info "bad yaml" path key) [key {}]))))
          config-keys))
        config (update config :commands transform-commands)
        config (update config :processes (partial transform-processes commands))]
    config))

    ;;     config (update config :commands (partial index-seq (comp keyword :key)))
    ;;     config (update config :processes (partial template/map-cat template-vars))
    ;; (update
    ;;  config
    ;;  :processes
    ;;  (fn [processes]
    ;;    (template/map-cat
    ;;     (fn [process]
    ;;       (let [command (get-in config [:commands (keyword (:command process))])]
    ;;         (command/apply-composite config command process)))
    ;;     processes)))))

(defn load-config
  [path]
  (let [config (config/read-path path)
        network (load-flow-config (get-in config [:flow :path]))]
    (assoc config :gaia network)))

(defn load-store
  [config]
  (condp = (keyword (:type config))
    :file (store/load-file-store config)
    :swift (swift/load-swift-store config)
    (store/load-file-store config)))

(defn load-executor
  [config store commands]
  (condp = (keyword (:target config))
    :funnel (funnel/load-funnel-executor config store commands)))
