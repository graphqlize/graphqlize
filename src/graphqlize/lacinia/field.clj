(ns graphqlize.lacinia.field
  (:require [honeyeql.meta-data :as heql-md]))

(defn lacinia-type [heql-attr-type entity-ident-in-pascal-case]
  (case heql-attr-type
    :attr.type/ref entity-ident-in-pascal-case
    (:attr.type/big-integer :attr.type/integer) 'Int
    (:attr.type/float :attr.type/double :attr.type/decimal) 'Float
    :attr.type/boolean 'Boolean
    :attr.type/string 'String
    (:attr.type/data-time
     :attr.type/date :attr.type/time :attr.type/time-span
     :attr.type/offset-date-time :attr.type/offset-time
     :attr.type/ip-address :attr.type/json :attr.type/uuid :attr.type/unknown) 'String
    nil))

(defn generate [heql-meta-data attr-ident]
  (let [attr-meta-data               (heql-md/attr-meta-data heql-meta-data attr-ident)
        attr-ref-type                (:attr.ref/type attr-meta-data)
        entity-ident-in-pascal-case  (when attr-ref-type
                                       (:entity.ident/pascal-case (heql-md/entity-meta-data heql-meta-data (:attr.ref/type attr-meta-data))))
        {:attr/keys [nullable type]} attr-meta-data
        field-type                   (lacinia-type type entity-ident-in-pascal-case)
        gql-field-type               (cond->> field-type
                                       (= :attr.ref.cardinality/many
                                          (:attr.ref/cardinality attr-meta-data)) (list 'list)
                                       (not nullable) (list 'non-null))]
    (when field-type
      {(:attr.ident/camel-case attr-meta-data) {:type gql-field-type}})))