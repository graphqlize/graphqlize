(ns graphqlize.lacinia.arg
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.type :as l-type]))

(defn- primary-key-attrs->args [heql-meta-data primary-key-attrs]
  (reduce (fn [args attr-ident]
            (let [attr-meta-data    (heql-md/attr-meta-data heql-meta-data attr-ident)
                  attr-type         (:attr/type attr-meta-data)
                  attr-ref-type     (:attr.ref/type attr-meta-data)
                  attr-lacinia-type (l-type/lacinia-type attr-type attr-ref-type)]
              (assoc args (:attr.ident/camel-case attr-meta-data)
                     {:type (list 'non-null attr-lacinia-type)})))
          {} primary-key-attrs))

(defn- query-by-primary-key-args [heql-meta-data entity-meta-data]
  (primary-key-attrs->args heql-meta-data
                           (get-in entity-meta-data [:entity.relation/primary-key
                                                     :entity.relation.primary-key/attrs])))

(def ^:private pagination-args
  {:limit  {:type :Int}
   :offset {:type :Int}})

(defn- order-by-arg [e-md]
  {:orderBy {:type (-> (:entity.ident/pascal-case e-md)
                       name
                       (str "OrderBy")
                       keyword)}})

(defn- group-by-arg [e-md]
  {:groupBy {:type (list 'list
                         (list 'non-null
                          (-> (:entity.ident/pascal-case e-md)
                              name
                              (str "GroupByEnum")
                              keyword)))}})

(defn- where-predicate-arg [e-md]
  {:where {:type (-> (:entity.ident/pascal-case e-md)
                     name
                     (str "Predicate")
                     keyword)}})

(defn many-field-args [heql-meta-data entity-meta-data]
  (let [default-args (merge pagination-args
                            (where-predicate-arg entity-meta-data)
                            (group-by-arg entity-meta-data))]
    (if (not= "MySQL" (heql-md/db-product-name heql-meta-data))
      (merge default-args
             (order-by-arg entity-meta-data))
      default-args)))

(defn query-args [heql-meta-data entity-meta-data query-type]
  (cond-> {}
    (= :graphqlize/query-by-primary-key query-type) (merge (query-by-primary-key-args heql-meta-data entity-meta-data))
    (= :graphqlize/collection-query query-type) (merge
                                                 pagination-args
                                                 (order-by-arg entity-meta-data)
                                                 (where-predicate-arg entity-meta-data)
                                                 (group-by-arg entity-meta-data))
    :else identity))