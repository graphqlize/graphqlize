(ns graphqlize.lacinia.ast
  (:require [inflections.core :as inf]
            [clojure.string :as c-str]))

(declare to-eql)

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

(defn- args-to-ident-predicate [root-attr-ns args]
  (first (map (fn [[k v]]
                [(keyword root-attr-ns (inf/hyphenate (name k))) v]) args)))

(defn to-eql [namespaces selection-tree args]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        attrs (map (comp inf/hyphenate name) (keys selection-tree))
        eql [{(args-to-ident-predicate root-attr-ns args) 
              (vec (map #(keyword root-attr-ns %) attrs))}]]
    (tap> {:eql eql})
    eql))