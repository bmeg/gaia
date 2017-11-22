(ns gaia.command-test
  (:require
   [clojure.test :refer :all]
   [taoensso.timbre :as log]
   [gaia.config :as config]
   [gaia.flow :as flow]
   [gaia.command :as command]))

(log/set-level! :trace)

(def composite-data
  (let [config (config/parse-yaml "resources/test/bmeg.flows.yaml")
        commands (reduce (fn [m command] (assoc m (keyword (:key command)) command)) {} (take 4 config))
        processes (drop 4 config)]
    {:commands commands
     :processes processes}))

(deftest inner-test
  (testing "inner composite process generation"
    (let [inner (command/apply-composite
                 composite-data
                 (get-in composite-data [:commands :inner-composite])
                 (first (:processes composite-data)))]
      (log/info (with-out-str (clojure.pprint/pprint inner))))))

(deftest outer-test
  (testing "outer composite process generation"
    (let [inner (command/apply-composite
                 composite-data
                 (get-in composite-data [:commands :outer-composite])
                 (last (:processes composite-data)))]
      (log/info (with-out-str (clojure.pprint/pprint inner))))))
