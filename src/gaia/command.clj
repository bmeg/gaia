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
  [{:keys [inputs outputs]} process]
  (let [pin (:inputs process)
        pout (:outputs process)
        pvars (:vars process)]
    (if-not (and
             (empty? (difference (map keyword inputs) (keys pin)))
             (empty? (difference (map keyword outputs) (keys pout))))
      (throw (Exception. (str (:key process) " - all inputs and outputs must be specified: " inputs outputs))))))

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
   "generated/"
   key "-"
   output "-"
   global "-"
   (uuid)))

(defn generate-outputs
  [process outputs step]
  (reduce
   (fn [all [key template]]
     (if (get-in process [:outputs (keyword template)])
       all
       (assoc all (keyword template) (generate-binding (:key process) (name key) template))))
   outputs (:outputs step)))

(declare apply-composite)

(defn apply-step
  [flow process vars inputs outputs {:keys [key command] :as step}]
  (let [ovars (substitute-values (:vars step) vars)
        oin (substitute-values (:inputs step) inputs)
        oout (substitute-values (:outputs step) outputs)
        inner {:key key
               :command command
               :vars ovars
               :inputs oin
               :outputs oout}
        exec (get-in flow [:commands (keyword command)])]
    (if (= (:type exec) "composite")
      (apply-composite flow exec inner)
      [inner])))

(defn apply-composite
  [flow {:keys [vars inputs outputs steps] :as command} process]
  (validate-apply-composite! command process)
  (let [generated (reduce (partial generate-outputs process) {} steps)
        apply-partial (partial
                       apply-step
                       flow
                       process
                       (:vars process)
                       (merge (:inputs process) generated)
                       (merge (:outputs process) generated))
        asteps (mapcat apply-partial steps)]
    (mapv identity asteps)))
