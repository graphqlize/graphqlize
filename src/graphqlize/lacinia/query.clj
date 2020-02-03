(ns graphqlize.lacinia.query
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.field :as l-field]
            [inflections.core :as inf]))

(defn- primary-key-attrs->query-name [entity-ident-in-camel-case primary-key-attrs]
  (->> (map (comp inf/camel-case name) primary-key-attrs)
       sort
       (clojure.string/join "And")
       (str (name entity-ident-in-camel-case) "By")
       keyword))

(defn- primary-key-attrs->args [heql-meta-data primary-key-attrs]
  (reduce (fn [args attr-ident]
            (let [attr-meta-data    (heql-md/attr-meta-data heql-meta-data attr-ident)
                  attr-type         (:attr/type attr-meta-data)
                  attr-ref-type     (:attr.ref/type attr-meta-data)
                  attr-lacinia-type (l-field/lacinia-type attr-type attr-ref-type)]
              (assoc args (:attr.ident/camel-case attr-meta-data)
                     {:type (list 'non-null attr-lacinia-type)})))
          {} primary-key-attrs))

(defn- entity-meta-data->query-by-primary-key [heql-meta-data entity-meta-data]
  (when-let [pk (:entity.relation/primary-key entity-meta-data)]
    (let [{:entity.ident/keys [camel-case pascal-case]} entity-meta-data
          pk-attrs                                      (:entity.relation.primary-key/attrs pk)]
      {(primary-key-attrs->query-name camel-case pk-attrs) {:type    pascal-case
                                                            :args    (primary-key-attrs->args heql-meta-data pk-attrs)
                                                            :resolve :graphqlize/query-by-primary-key}})))

(defn- entity-meta-data->simple-query [entity-meta-data]
  (let [{:entity.ident/keys [pascal-case plural]} entity-meta-data]
    {plural {:type    (list 'non-null (list 'list pascal-case))
             :resolve :graphqlize/collection-query}}))

(defn generate [heql-meta-data]
  (apply merge (map (fn [e-md]
                      (merge
                       (entity-meta-data->query-by-primary-key heql-meta-data e-md)
                       (entity-meta-data->simple-query e-md)))
                    (heql-md/entities heql-meta-data))))