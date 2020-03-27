(ns graphqlize.lacinia.arg
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.field :as l-field]))

(defn- primary-key-attrs->args [heql-meta-data primary-key-attrs]
  (reduce (fn [args attr-ident]
            (let [attr-meta-data    (heql-md/attr-meta-data heql-meta-data attr-ident)
                  attr-type         (:attr/type attr-meta-data)
                  attr-ref-type     (:attr.ref/type attr-meta-data)
                  attr-lacinia-type (l-field/lacinia-type attr-type attr-ref-type)]
              (assoc args (:attr.ident/camel-case attr-meta-data)
                     {:type (list 'non-null attr-lacinia-type)})))
          {} primary-key-attrs))

(defn- query-by-primary-key-args [heql-meta-data entity-meta-data]
  (primary-key-attrs->args heql-meta-data
                           (get-in entity-meta-data [:entity.relation/primary-key
                                                     :entity.relation.primary-key/attrs])))

(defn find-args [heql-meta-data entity-meta-data query-type]
  (let [args (if (= :graphqlize/query-by-primary-key query-type)
               (query-by-primary-key-args heql-meta-data entity-meta-data)
               {})]
    args))