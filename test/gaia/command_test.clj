(ns gaia.command-test
  (:require
   [clojure.test :refer :all]
   [taoensso.timbre :as log]
   [gaia.config :as config]
   [gaia.flow :as flow]
   [gaia.command :as command]))

(log/set-level! :trace)

(def composite-data
  (config/load-flow-config "resources/test/test"))
  ;; (let [config (config/parse-yaml "resources/test/bmeg.flows.yaml")
  ;;       index (config/index-seq (comp keyword :key) config)
  ;;       commands (config/filter-map (fn [k v] (= (keyword (:type v)) :command)) index)
  ;;       processes (config/filter-map (fn [k v] (= (keyword (:type v)) :process)) index)]
  ;;   {:commands commands
  ;;    :processes processes})


(defn pp
  [clj]
  (with-out-str (clojure.pprint/pprint clj)))

(deftest inner-test
  (testing "inner composite process generation"
    (log/info "commands" (pp (:commands composite-data)))
    (log/info "processes" (pp (:processes composite-data)))
    (let [process-index (config/index-seq
                         (comp keyword :key)
                         (:processes composite-data))
          inner (command/apply-composite
                 composite-data
                 (get-in composite-data [:commands :inner-composite])
                 (get process-index :inner-demo-invoke-TCGA-LUAD))]
      (log/info (with-out-str (clojure.pprint/pprint inner))))))

(deftest outer-test
  (testing "outer composite process generation"
    (let [process-index (config/index-seq
                         (comp keyword :key)
                         (:processes composite-data))
          inner (command/apply-composite
                 composite-data
                 (get-in composite-data [:commands :outer-composite])
                 (get process-index :outer-demo-invoke))]
      (log/info (pp inner)))))
