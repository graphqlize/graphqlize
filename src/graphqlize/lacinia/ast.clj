(ns graphqlize.lacinia.ast
  (:require [inflections.core :as inf]
            [clojure.string :as c-str]))

(declare to-eql)

(defn- root-entity-ident [namespaces selection-tree]
  (let [raw-namespaces   (map name namespaces)
        raw-entity-ident (-> (ffirst selection-tree)
                             namespace
                             inf/hyphenate)]
    (if-let [raw-ns (first (filter #(c-str/starts-with? raw-entity-ident %) raw-namespaces))]
      (keyword raw-ns (c-str/replace-first raw-entity-ident (str raw-ns "-") ""))
      (keyword raw-entity-ident))))

(defn to-eql [namespaces selection-tree args]
  (let [root-entity-ident (root-entity-ident namespaces selection-tree)
        eql [{[:actor/actor-id 1] [:actor/first-name
                                   :actor/last-name]}]]
    (tap> {:eql eql})
    eql))