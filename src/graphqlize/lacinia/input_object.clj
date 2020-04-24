(ns graphqlize.lacinia.input-object
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.type :as l-type]))

(defn- comparison-input-object [lacinia-type]
  (let [between-op    (keyword (str lacinia-type "BetweenOp"))
        comparison-op (keyword (str lacinia-type "ComparisonOp"))
        all-ops       {between-op    {:fields {:from {:type (list 'non-null lacinia-type)}
                                               :to   {:type (list 'non-null lacinia-type)}}}
                       comparison-op {:fields {:eq        {:type lacinia-type}
                                               :neq       {:type lacinia-type}
                                               :lt        {:type lacinia-type}
                                               :lte       {:type lacinia-type}
                                               :gt        {:type lacinia-type}
                                               :gte       {:type lacinia-type}
                                               :in        {:type (list 'list (list 'non-null lacinia-type))}
                                               :notIn     {:type (list 'list (list 'non-null lacinia-type))}
                                               :isNull    {:type 'Boolean}
                                               :isNotNull {:type 'Boolean}
                                               :between   {:type between-op}}}}]
    (case (name lacinia-type)
      "String"  (-> (dissoc all-ops between-op)
                    (update-in [comparison-op :fields] #(dissoc % :between :lt :lte :gt :gte))
                    (update-in [comparison-op :fields] #(assoc %
                                                               :like {:type 'String} :notLike {:type 'String})))
      "Boolean" (update-in (dissoc all-ops between-op)
                           [comparison-op :fields] #(dissoc % :between :lt :lte :gt :gte :in :notIn))
      "UUID" (update-in (dissoc all-ops between-op)
                        [comparison-op :fields] #(dissoc % :between :lt :lte :gt :gte))
      all-ops)))

(def ^:private comparison-input-objects
  (merge
   (comparison-input-object 'Int)
   (comparison-input-object 'Long)
   (comparison-input-object 'BigInteger)
   (comparison-input-object 'Float)
   (comparison-input-object 'BigDecimal)
   (comparison-input-object 'UUID)
   (comparison-input-object 'String)
   (comparison-input-object 'Boolean)
   (comparison-input-object 'Date)
   (comparison-input-object 'Time)
   (comparison-input-object 'TimeWithTimeZone)
   (comparison-input-object 'DateTime)
   (comparison-input-object 'DateTimeWithTimeZone)))

(defn- order-by-field [attr-md]
  {(:attr.ident/camel-case attr-md) {:type :OrderBy}})

(defn- where-predicate-field [attr-md]
  (let [lacinia-type (l-type/lacinia-type (:attr/type attr-md))]
    {(:attr.ident/camel-case attr-md) {:type (-> (name lacinia-type)
                                                 (str "ComparisonOp")
                                                 keyword)}}))

(defn- entity-meta-data->input-object [heql-meta-data entity-meta-data]
  (let [entity-name               (name (:entity.ident/pascal-case entity-meta-data))
        attr-idents               (heql-md/attr-idents entity-meta-data)
        predicate-type            (keyword (str entity-name "Predicate"))
        non-relationship-attrs-md (filter
                                   #(not= :attr.type/ref (:attr/type %)) (map #(heql-md/attr-meta-data heql-meta-data %) attr-idents))]
    {(keyword (str entity-name "OrderBy")) {:fields (apply merge (map order-by-field non-relationship-attrs-md))}
     predicate-type                        {:fields (apply merge
                                                           (cons
                                                            {:and {:type (list 'list (list 'non-null predicate-type))}
                                                             :or  {:type (list 'list (list 'non-null predicate-type))}
                                                             :not {:type predicate-type}}
                                                            (map where-predicate-field non-relationship-attrs-md)))}}))

(defn generate [heql-meta-data]
  (->> (heql-md/entities heql-meta-data)
       (map #(entity-meta-data->input-object heql-meta-data %))
       (cons comparison-input-objects)
       (apply merge)))