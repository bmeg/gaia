(ns gaia.command
  (:require
   [clojure.set :as set]))

(defn uuid
  []
  (str (java.util.UUID/randomUUID)))

(defn difference
  [a b]
  (set/difference (set a) (set b)))

(defn validate-apply-composite!
  [{:keys inputs outputs} process]
  (let [pin (:inputs process)
        pout (:outputs process)
        pvars (:vars process)]
    (if-not (and (empty? (difference inputs (keys pin)))
                 (empty? (difference outputs (keys pout))))
      (throw (Exception. (str (:key process) " - all inputs and outputs must be specified"))))))

(defn substitute-values
  [template values]
  (into
   {}
   (map
    (fn [[k t]]
      [k (get values (keyword t))])
    template)))

(defn generate-binding
  [key output global]
  (str
   key "-"
   output "-"
   global "-"
   (uuid)))

(defn apply-outputs
  [process template outputs]
  (reduce
   (fn [result [k t]]
     (if-let [value (get outputs (keyword t))]
       (assoc-in result [:final k] value)
       (assoc-in result [:inner t] (generate-binding (:key process) k t))))
   {:final {} :inner {}} template))

(defn generate-variables
  "we need some way to find out what the keys of the generated inputs are
   before we resolve the rest of the steps"
  [])

(defn apply-step
  [process vars inputs outputs {:keys [key command] :as step}]
  (let [ovars (substitute-values (:vars step) vars)
        oin (substitute-values (:inputs step) inputs)
        {:keys [final inner]} (apply-outputs process (:outputs step) outputs)]
    {:inner inner
     :step
     {:key key
      :command command
      :vars ovars
      :inputs oin
      :outputs (merge inner final)}}))

(defn apply-composite
  [flow {:keys [vars inputs outputs steps] :as command} process]
  (validate-apply-composite! command process)
  (let [pin (:inputs process)
        pout (:outputs process)
        pvars (:vars process)
        pkey (:key process)
        asteps (map (partial apply-steps process pvars pin pout) steps)]))

