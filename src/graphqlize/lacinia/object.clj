(ns graphqlize.lacinia.object
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.field :as l-field]))

(defn- entity-meta-data->object [heql-meta-data entity-meta-data]
  (let [attr-idents                          (heql-md/attr-idents entity-meta-data)]
    {(:entity.ident/pascal-case entity-meta-data)
     {:fields (apply merge (map #(l-field/generate heql-meta-data %) attr-idents))}}))

(defn generate [heql-meta-data]
  (apply merge (map (fn [e-md]
                      (entity-meta-data->object heql-meta-data e-md))
                    (heql-md/entities heql-meta-data))))