(ns graphqlize.lacinia.query
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.arg :as l-arg]
            [clojure.string :as string]
            [inflections.core :as inf]))

(defn- primary-key-attrs->query-name [entity-ident-in-camel-case primary-key-attrs]
  (->> (map (comp inf/camel-case name) primary-key-attrs)
       sort
       (string/join "And")
       (str (name entity-ident-in-camel-case) "By")
       keyword))

(defn- entity-meta-data->query-by-primary-key [heql-meta-data entity-meta-data]
  (when-let [pk (:entity.relation/primary-key entity-meta-data)]
    (let [{:entity.ident/keys [camel-case pascal-case]} entity-meta-data
          pk-attrs                                      (:entity.relation.primary-key/attrs pk)]
      {(primary-key-attrs->query-name camel-case pk-attrs) {:type    pascal-case
                                                            :args    (l-arg/query-args heql-meta-data entity-meta-data :graphqlize/query-by-primary-key)
                                                            :resolve :graphqlize/query-by-primary-key}})))

(defn- entity-meta-data->collection-query [heql-meta-data entity-meta-data]
  (let [{:entity.ident/keys [pascal-case plural]} entity-meta-data]
    {plural {:type    (list 'non-null (list 'list pascal-case))
             :args    (l-arg/query-args heql-meta-data entity-meta-data :graphqlize/collection-query)
             :resolve :graphqlize/collection-query}}))

(defn generate [heql-meta-data]
  (apply merge (map (fn [e-md]
                      (merge
                       (entity-meta-data->query-by-primary-key heql-meta-data e-md)
                       (entity-meta-data->collection-query heql-meta-data e-md)))
                    (heql-md/entities heql-meta-data))))