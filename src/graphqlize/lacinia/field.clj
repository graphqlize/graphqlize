(ns graphqlize.lacinia.field
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.arg :as l-arg]
            [graphqlize.lacinia.type :as l-type]
            [inflections.core :as inf]))

#_ (inf/camel-case :firstName)

(defn- aggregate-field-prefixes [attr-type]
  (case attr-type
    (:attr.type/integer :attr.type/big-integer :attr.type/long
                        :attr.type/float :attr.type/double :attr.type/decimal) ["countOf" "avgOf" "sumOf" "minOf" "maxOf"]
    (:attr.type/date-time :attr.type/date-time-with-time-zone :attr.type/date
                          :attr.type/time :attr.type/time-with-time-zone) ["countOf" "minOf" "maxOf"]
    ["countOf"]))

(defn- aggregate-fields [{:attr/keys [type]
                          :as attr-md}]
  (when-not (= :attr.type/ref type)
    (let [field-name (-> (:attr.ident/camel-case attr-md)
                         inf/camel-case
                         name)
          field-type (l-type/lacinia-type type)
          prefixes (aggregate-field-prefixes type)
          int-type (list 'non-null 'Int)
          float-type 'Float
          decimal-type 'BigDecimal
          field-type field-type]
      (reduce (fn [m p]
                (assoc m (keyword (str p field-name))
                       (case p
                         "countOf" {:type int-type}
                         "avgOf" (if (:= :attr.type/decimal type) 
                                   {:type decimal-type}
                                   {:type float-type})
                         {:type field-type }))) {} prefixes))))
#_ (into {} [[:a 1] [:b 2]])
#_ (aggregate-fields {:attr/type :attr.type/integer
                      :attr.ident/camel-case :rating})

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
      (merge {field-name field-def}
             (aggregate-fields attr-meta-data)))))