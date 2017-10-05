(ns gaia.flow
  (:require
   [clojure.set :as set]
   [taoensso.timbre :as log]))

(defn make-node
  [{:keys [key inputs command outputs] :as node}])

(defn in? 
  [coll el]
  (some #(= el %) coll))

(defn set-conj
  [coll el]
  (conj (or coll (set nil)) el))

(defn data->process
  [flow data process]
  (-> flow
      (update-in [:data data :to] set-conj process)
      (update-in [:process process :from] set-conj data)))

(defn process->data
  [flow process data]
  (-> flow
      (update-in [:process process :to] set-conj data)
      (update-in [:data data :from] set-conj process)))

(defn add-node
  [flow
   {:keys [key inputs command outputs]
    :as node}]
  (let [process (reduce
                 (fn [flow data]
                   (data->process flow data key))
                 flow (vals inputs))
        data (reduce
              (fn [flow data]
                (process->data flow key data))
              process (vals outputs))
        full (assoc-in data [:flow key] node)]
    full))

(defn flow-space
  [flow]
  (set
   (keys
    (:data flow))))

(defn missing-data
  [flow data]
  (set/difference
   (flow-space flow)
   (set (keys data))))

(defn flow-complete?
  [flow data]
  (empty? (missing-data flow data)))

(defn runnable?
  [flow data process]
  (let [inputs (-> flow :flow process :inputs vals)]
    (every? data inputs)))

(defn able-processes
  [flow data]
  (filter (partial runnable? flow data) (-> flow :process keys)))

(defn process-produces?
  [flow missing process]
  (let [out (-> flow :flow process :outputs vals)]
    (not (empty? (set/intersection missing (set out))))))

(defn find-candidates
  [flow data]
  (let [missing (missing-data flow data)
        able (able-processes flow data)]
    (log/trace "missing" missing)
    (log/trace "able" able)
    (filter
     (partial process-produces? flow missing)
     able)))

(defn pull-data
  [data inputs]
  (into
   {}
   (map
    (fn [[arg key]]
      [arg (get data key)])
    inputs)))

(defn stuff-data
  [data outputs]
  (into
   {}
   (map
    (fn [[out key]]
      {key (get data out)})
    outputs)))

(defn run-process
  [flow data process]
  (log/trace "run" process)
  (let [{:keys [inputs outputs command]} (get-in flow [:flow process])
        run (get-in flow [:command command])
        args (pull-data data inputs)
        result (run args)
        out (stuff-data result outputs)]
    (log/trace "command" command)
    (log/trace "inputs" inputs)
    (log/trace "args" args)
    (log/trace "outputs" outputs)
    (log/trace "result" result)
    (log/trace "out" out)
    out))

(defn step-flow
  [flow data]
  (let [candidates (find-candidates flow data)
        outcome (mapv (partial run-process flow data) candidates)]
    (log/trace "candidates" candidates)
    (reduce merge data outcome)))

(defn run-flow
  [flow data]
  (loop [data data]
    (if (flow-complete? flow data)
      {:data data :complete? true}
      (let [next (step-flow flow data)]
        (if (= next data)
          {:data data :complete? false}
          (recur next))))))

(defn setcat
  [seqs]
  (reduce into #{} seqs))

(defn find-descendants
  [flow key]
  (loop [covered #{}
         found #{key}]
    (if (empty? found)
      covered
      (let [process (setcat (map (fn [key] (get-in flow [:data key :to])) found))
            to (setcat (map (fn [p] (get-in flow [:process p :to])) process))]
        (recur (set/union covered found) to)))))

(defn update-data
  [flow data key value]
  (let [expiring (find-descendants flow key)
        expired (apply dissoc data expiring)
        next (assoc expired key value)]
    next))
