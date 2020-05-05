(ns graphqlize.lacinia.enum
  (:require [honeyeql.meta-data :as heql-md]))

(defn- enitity-have-enum [heql-meta-data entity-meta-data]
  (let [entity-name                (name (:entity.ident/pascal-case entity-meta-data))
        attr-idents                (heql-md/attr-idents entity-meta-data)
        attrs-md                   (map #(heql-md/attr-meta-data heql-meta-data %) attr-idents)
        list-relationship-attrs-md (filter #(#{:attr.column.ref.type/one-to-many
                                               :attr.column.ref.type/many-to-many}
                                             (:attr.column.ref/type %)) attrs-md)]
    (when (seq list-relationship-attrs-md)
      {(keyword (str entity-name "HaveEnum"))
       {:values (map #(keyword (:attr.ident/camel-case %)) list-relationship-attrs-md)}})))

(defn generate [heql-meta-data]
  (merge {:OrderBy {:values [:ASC :DESC]}}
         (apply merge (map (fn [e-md]
                             (enitity-have-enum heql-meta-data e-md))
                           (heql-md/entities heql-meta-data)))))