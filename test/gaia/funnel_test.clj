(ns gaia.funnel-test
  (:require
   [clojure.test :refer :all]
   [taoensso.timbre :as log]
   [gaia.funnel :as funnel]))

(log/set-level! :trace)

(def commands
  {:echo
   {:image_name "alpine"
    :cmd ["cat" "/tmp/in"]
    :stdout "/tmp/out"}

   :gdc-extract
   {:image_name "gdc-extract"
    :cmd ["/opt/gdc-scan.py" "cases" "list"]
    :stdout "/tmp/out"}})

(def echo-hello-world
  {:key :echo-hello-world
   :command :echo
   :inputs {"/tmp/in" {:contents "hello world!"}}
   :outputs {"/tmp/out" "hellooooo"}})

(def gdc-extract-process
  {:key :gdc-extract
   :command :gdc-extract
   :inputs {}
   :outputs {"/tmp/out" "gdc-cases.json"}})

(def echo-hi
  (funnel-task commands echo-hello-world))

(deftest funnel-test
  (testing "running flows"
    (let [data {:twenty-six 26.0}]
      (is (= (:twenty-six data) 26.0)))))
