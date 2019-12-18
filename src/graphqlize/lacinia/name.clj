(ns graphqlize.lacinia.name
  (:require [inflections.core :as inf]))

(defn object [entity-ident]
  (if-let [schema (namespace entity-ident)]
    (keyword (inf/camel-case (str schema "-" (name entity-ident))))
    (keyword (inf/camel-case (name entity-ident)))))

(defn field [attr-ident]
  (keyword (inf/camel-case (name attr-ident) :lower)))