(ns graphqlize.lacinia.input-object
  (:require [honeyeql.meta-data :as heql-md]))

(defn- order-by-field [attr-md]
  {(:attr.ident/camel-case attr-md) {:type :OrderBy}})

(defn- entity-meta-data->input-object [heql-meta-data entity-meta-data]
  (let [entity-name                          (name (:entity.ident/pascal-case entity-meta-data))
        {:entity/keys [req-attrs opt-attrs]} entity-meta-data
        attr-idents                          (concat req-attrs opt-attrs)]
    {(keyword (str entity-name "OrderBy")) {:fields (->>
                                                     (map #(heql-md/attr-meta-data heql-meta-data %) attr-idents)
                                                     (filter #(not= :attr.type/ref (:attr/type %)))
                                                     (map order-by-field)
                                                     (apply merge))}}))

(defn generate [heql-meta-data]
  (apply merge (map (fn [e-md]
                      (entity-meta-data->input-object heql-meta-data e-md))
                    (heql-md/entities heql-meta-data))))