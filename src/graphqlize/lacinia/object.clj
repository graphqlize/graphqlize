(ns graphqlize.lacinia.object
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.name :as l-name]
            [graphqlize.lacinia.field :as l-field]))

(defn- entity-meta-data->object [heql-meta-data entity-meta-data]
  (let [{:entity/keys [ident req-attrs opt-attrs]} entity-meta-data
        attr-idents                                (concat req-attrs opt-attrs)]
    {(l-name/object ident) {:fields (apply merge (map #(l-field/generate heql-meta-data %) attr-idents))}}))

(defn generate [heql-meta-data]
  (apply merge (map (fn [e-md]
                      (entity-meta-data->object heql-meta-data e-md))
                    (heql-md/entities heql-meta-data))))