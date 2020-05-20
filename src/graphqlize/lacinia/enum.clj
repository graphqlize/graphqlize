(ns graphqlize.lacinia.enum
  (:require [honeyeql.meta-data :as heql-md]))

(defn- entity-group-by-enum [entity-meta-data attrs-md]
  (let [entity-name    (name (:entity.ident/pascal-case entity-meta-data))
        group-by-attrs (remove #(#{:attr.column.ref.type/one-to-many
                                   :attr.column.ref.type/many-to-many}
                                 (:attr.column.ref/type %)) attrs-md)]
    (when (seq group-by-attrs)
      {(keyword (str entity-name "GroupByEnum")) {:values (map #(keyword (:attr.ident/camel-case %)) group-by-attrs)}})))

(defn- enitity-have-enum [entity-meta-data attrs-md]
  (let [entity-name                (name (:entity.ident/pascal-case entity-meta-data))
        list-relationship-attrs-md (filter #(#{:attr.column.ref.type/one-to-many
                                               :attr.column.ref.type/many-to-many}
                                             (:attr.column.ref/type %)) attrs-md)]
    (when (seq list-relationship-attrs-md)
      {(keyword (str entity-name "HaveEnum")) {:values (map #(keyword (:attr.ident/camel-case %)) list-relationship-attrs-md)}})))

(defn generate [heql-meta-data]
  (merge {:OrderBy {:values [:ASC :DESC]}}
         (apply merge (map (fn [e-md]
                             (let [attrs-md (map #(heql-md/attr-meta-data heql-meta-data %) (heql-md/attr-idents e-md))]
                               (merge
                                (enitity-have-enum e-md attrs-md)
                                (entity-group-by-enum e-md attrs-md))))
                           (heql-md/entities heql-meta-data)))))

