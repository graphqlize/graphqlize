(ns graphqlize.lacinia.field
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.arg :as l-arg]
            [graphqlize.lacinia.type :as l-type]))

(defn generate [heql-meta-data attr-ident]
  (let [attr-meta-data               (heql-md/attr-meta-data heql-meta-data attr-ident)
        attr-ref-type                (:attr.ref/type attr-meta-data)
        ref-entity-md                (when attr-ref-type
                                       (heql-md/entity-meta-data heql-meta-data attr-ref-type))
        entity-ident-in-pascal-case  (:entity.ident/pascal-case ref-entity-md)
        {:attr/keys [nullable type]} attr-meta-data
        has-many?                    (= :attr.ref.cardinality/many
                                        (:attr.ref/cardinality attr-meta-data))
        field-type                   (l-type/lacinia-type type entity-ident-in-pascal-case)
        gql-field-type               (cond->> field-type
                                       has-many? (list 'list)
                                       (not nullable) (list 'non-null))
        field-name                   (:attr.ident/camel-case attr-meta-data)
        field-def                    {:type gql-field-type}
        field-def                    (if has-many?
                                       (assoc field-def :args (l-arg/many-field-args heql-meta-data ref-entity-md))
                                       field-def)]
    (when field-type
      {field-name field-def})))