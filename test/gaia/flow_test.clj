(ns gaia.flow-test
  (:require
   [clojure.test :refer :all]
   [taoensso.timbre :as log]
   [gaia.flow :as flow]))

(log/set-level! :trace)

(def commands
  {:line
   (fn [{:keys [m x b]}]
     {:z (+ (* m x) b)})

   :triangle
   (fn [{:keys [a b]}]
     {:c
      (Math/sqrt
       (+
        (* a a)
        (* b b)))})})

(def line-nodes
  [{:key :line-five
    :command :line
    :inputs {:m :one
             :x :two
             :b :three}
    :outputs {:z :five}}

   {:key :line-eleven
    :command :line
    :inputs {:m :two
             :x :three
             :b :five}
    :outputs {:z :eleven}}

   {:key :line-omega
    :command :line
    :inputs {:m :one
             :x :three
             :b :five}
    :outputs {:z :omega}}

   {:key :line-twenty-six
    :command :line
    :inputs {:m :three
             :x :five
             :b :eleven}
    :outputs {:z :twenty-six}}

   {:key :line-four
    :command :line
    :inputs {:m :one
             :x :two
             :b :two}
    :outputs {:z :four}}

   {:key :line-twelve
    :command :line
    :inputs {:m :two
             :x :five
             :b :two}
    :outputs {:z :twelve}}

   {:key :triangle-five
    :command :triangle
    :inputs {:a :three
             :b :four}
    :outputs {:c :five}}

   {:key :triangle-thirteen
    :command :triangle
    :inputs {:a :five
             :b :twelve}
    :outputs {:c :thirteen}}])

(def initial-flow
  {:command commands})

(def line-flow
  (reduce
   flow/add-node
   initial-flow
   line-nodes))

(deftest flow-test
  (testing "running flows"
    (let [{:keys [data]} (flow/run-flow line-flow {:one 1 :two 2 :three 3})
          next (flow/update-data line-flow data :three 11)
          alternate (flow/run-flow line-flow next)]
      (log/info "flow" line-flow)
      (log/info "data" data)
      (log/info "next" next)
      (log/info "alternate" (:data alternate))
      (is (= (:twenty-six data) 26))
      (is (= (:thirteen data) 13.0)))))
