(ns graphqlize.lacinia.object
  (:require [inflections.core :as inf]
            [honeyeql.meta-data :as heql-md]))

(defn- object-type-name [heql-attr-ref-type]
  (if-let [schema (namespace heql-attr-ref-type)]
    (keyword (inf/camel-case (str schema "-" (name heql-attr-ref-type))))
    (keyword (inf/camel-case (name heql-attr-ref-type)))))

(defn- field-name [attr-ident]
  (keyword (inf/camel-case (name attr-ident) :lower)))

(defn- heql-attr-type->gql-field-type [heql-attr-type heql-attr-ref-type]
  (case heql-attr-type
    :attr.type/ref (object-type-name heql-attr-ref-type)
    (:attr.type/big-integer :attr.type/integer) 'Int
    (:attr.type/float :attr.type/double :attr.type/decimal) 'Float
    :attr.type/boolean 'Boolean
    (:attr.type/string :attr.type/Unknown) 'String
    nil))

(defn- to-list-type [gql-field-type]
  (list 'list (list 'not-null gql-field-type)))

(defn- generate-field [heql-meta-data attr-ident]
  (let [attr-meta-data                     (heql-md/attr-meta-data heql-meta-data attr-ident)
        {:attr/keys [ident nullable type]} attr-meta-data
        field-type                         (heql-attr-type->gql-field-type type (:attr.ref/type attr-meta-data))
        gql-field-type                     (cond->> field-type
                                             (= :attr.ref.cardinality/many
                                                (:attr.ref/cardinality attr-meta-data)) to-list-type
                                             (not nullable) (list 'non-null))]
    (when field-type
      {(field-name ident) {:type gql-field-type}})))

(defn- entity-meta-data->object [heql-meta-data entity-meta-data]
  (let [{:entity/keys [ident req-attrs opt-attrs]} entity-meta-data
        attr-idents                                (concat req-attrs opt-attrs)]
    {(object-type-name ident) {:fields (apply merge (map #(generate-field heql-meta-data %) attr-idents))}}))

(defn generate [heql-meta-data]
  (apply merge (map (fn [[_ e-md]]
                      (entity-meta-data->object heql-meta-data e-md))
                    (heql-md/entities heql-meta-data))))