(ns graphqlize.lacinia.ast
  (:require [inflections.core :as inf]))

(declare to-eql)

(defn to-eql [selection-tree args]
  (let [eql [{[:actor/actor-id 1] [:actor/first-name
                                   :actor/last-name]}]]
    (tap> {:eql eql})
    eql))