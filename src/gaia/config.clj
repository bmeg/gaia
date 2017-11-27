(ns gaia.config
  (:require
   [clojure.walk :as walk]
   [clojure.math.combinatorics :as combinatorics]
   [taoensso.timbre :as log]
   [yaml.core :as yaml]
   [protograph.template :as template]))

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

(defn template-vars
  [{:keys [key vars inputs outputs] :as process}]
  (let [arrays (filter-map (fn [k v] (coll? v)) vars)
        series (cartesian-map arrays)]
    (map
     (fn [arrayed]
       (let [env (merge vars arrayed)]
         (merge
          process
          {:key (template/evaluate-template key env)
           :vars env
           :inputs (template/evaluate-map inputs env)
           :outputs (template/evaluate-map outputs env)})))
     series)))

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
          config-keys))]
    (-> config
        (update
         :commands
         (partial index-seq (comp keyword :key)))
        (update
         :processes
         (fn [processes]
           (template/mapcat template-vars processes))))))

     ;; (fn [commands]
     ;;   (reduce
     ;;    (fn [m command]
     ;;      (assoc m (keyword (:key command)) command))
     ;;    {} commands))

