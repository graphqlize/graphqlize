(ns graphqlize.lacinia.ast
  (:require [inflections.core :as inf]
            [clojure.string :as c-str]
            [honeyeql.debug :refer [trace>>]]))

(defn- eql-root-attr-ns [namespaces selection-tree]
  (let [raw-namespaces   (map name namespaces)
        raw-entity-ident (-> (ffirst selection-tree)
                             namespace
                             inf/hyphenate)]
    (if-let [raw-ns (first (filter #(c-str/starts-with? raw-entity-ident %) raw-namespaces))]
      (let [x (c-str/replace-first raw-entity-ident (str raw-ns "-") "")]
        (if (= x raw-ns)
          x
          (str raw-ns "." x)))
      raw-entity-ident)))

(defn- ident [root-attr-ns args]
  (->> (map (fn [[k v]]
              [(keyword root-attr-ns (inf/hyphenate (name k))) v]) args)
       (apply concat)
       vec))

(declare properties)

(defn- field->prop [namespaces selection-tree field]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        prop         (->> (name field)
                          inf/hyphenate
                          (keyword root-attr-ns))]
    (if-let [sub-selection (first (selection-tree field))]
      (let [sub-selection-tree (:selections sub-selection)]
        {prop (properties namespaces sub-selection-tree)})
      prop)))

(defn- properties [namespaces selection-tree]
  (vec (map #(field->prop namespaces selection-tree %) (keys selection-tree))))

(defn to-eql [namespaces selection-tree args]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        eql          [{(ident root-attr-ns args) (properties namespaces selection-tree)}]]
    (trace>> :eql eql)))