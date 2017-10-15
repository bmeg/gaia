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
    :stdout "/tmp/out"}})

(def echo-hello-world
  {:key :echo-hello-world
   :command :echo
   :inputs {"/tmp/in" {:contents "hello world!"}}
   :outputs {"/tmp/out" "hellooooo"}})

(def echo-hi (funnel-task commands ))

(deftest funnel-test
  (testing "running flows"
    (let [funnel (funnel/funnel-connect)
          {:keys [data]} (flow/run-flow line-flow starting-data)
          next (flow/update-data line-flow data :three 11)
          alternate (flow/run-flow line-flow next)]
      (log/info "flow" line-flow)
      (log/info "data" data)
      (log/info "next" next)
      (log/info "alternate" (:data alternate))
      (is (= (:twenty-six data) 26.0))
      (is (= (:thirteen data) 13.0))
      (is (> (get-in alternate [:data :thirteen]) 25))
      (is (> (get-in alternate [:data :twenty-six]) 150)))))
