(ns graphqlize.lacinia.eql
  (:require [inflections.core :as inf]
            [clojure.string :as string]
            [honeyeql.debug :refer [trace>>]]))

(defn- eql-root-attr-ns [namespaces selection-tree]
  (let [raw-namespaces   (map name namespaces)
        raw-entity-ident (-> (ffirst selection-tree)
                             namespace
                             inf/hyphenate)]
    (if-let [raw-ns (first (filter #(string/starts-with? raw-entity-ident %) raw-namespaces))]
      (let [x (string/replace-first raw-entity-ident (str raw-ns "-") "")]
        (if (= x raw-ns)
          x
          (str raw-ns "." x)))
      raw-entity-ident)))

#_(eql-root-attr-ns [:public] {:Language/languageId [nil]})
#_(eql-root-attr-ns [:person] {:PersonStateProvince/languageId [nil]})

(def ^:private reserved-args #{:limit :offset :orderBy :where})

(defn- ident [root-attr-ns args]
  (->> (remove (fn [[k _]]
                 (reserved-args k)) args)
       (mapcat (fn [[k v]]
                 [(keyword root-attr-ns (inf/hyphenate (name k))) v]))
       vec))

#_(ident "language" {:first 1})
#_(ident "language" {:language-id 1})
#_(ident "film-actor" {:film-id  1
                       :actor-id 1})

(defn- eqlify-order-by-param [selection-tree param]
  (map (fn [[k v]]
         (let [root-ns (-> (ffirst selection-tree)
                           namespace
                           inf/hyphenate)]
           [(->> (name k)
                 inf/hyphenate
                 (keyword root-ns))
            (-> (name v)
                string/lower-case
                keyword)])) param))

#_(eqlify-order-by-param {:City/city [nil]}
                         {:firstName :ASC
                          :lastName  :DESC})

(defn- hql-predicate [op]
  (fn [col v]
    (case op
      :isNull (if v [:= col nil] [:<> col nil])
      :isNotNull (if v [:<> col nil] [:= col nil])
      :between [:between col (:from v) (:to v)]
      [op col v])))

(def ^:private hql-predicate-fn
  {:eq        (hql-predicate :=)
   :lt        (hql-predicate :<)
   :lte       (hql-predicate :<=)
   :gt        (hql-predicate :>)
   :gte       (hql-predicate :>=)
   :neq       (hql-predicate :<>)
   :in        (hql-predicate :in)
   :notIn     (hql-predicate :not-in)
   :notLike   (hql-predicate :not-like)
   :like      (hql-predicate :like)
   :isNull    (hql-predicate :isNull)
   :isNotNull (hql-predicate :isNotNull)
   :between   (hql-predicate :between)})

(defn- where-predicate [root-ns field pred]
  (let [column (->> (name field)
                    inf/hyphenate
                    (keyword root-ns))
        [op v] (first pred)]
    ((hql-predicate-fn op) column v)))

(defn- where-clause [root-ns xs]
  (mapcat 
   #(map (fn [[k v]]
           (case k
             :and (concat [:and] (where-clause root-ns v))
             :or (concat [:or] (where-clause root-ns v))
             :not (concat [:not] (where-clause root-ns [v]))
             (where-predicate root-ns k v))) %)
   xs))

#_ (where-clause "payment" [{:not {:or [{:name {:eq "English"}} {:name {:eq "French"}}]}}])

(defn- eqlify-where-predicate [selection-tree param]
  (let [root-ns (-> (ffirst selection-tree)
                    namespace
                    inf/hyphenate)]
    (first (where-clause root-ns [param]))))

#_(eqlify-where-predicate
   #:Payment{:paymentId [nil]
             :amount    [nil]}
   {:and [{:amount     {:gt 5.99M}
           :customerId {:eq 1}}]})

#_(eqlify-where-predicate
   {:Language/languageId [nil]
    :Language/name       [nil]}
   {:not {:or [{:name {:eq "English"}} {:name {:eq "French"}}]} })

#_(eqlify-where-predicate {:Actor/firstName [nil]
                           :Actor/lastName  [nil]}
                          {:actorId {:eq 1}
                           :name    {:eq "foo"}})

(defn- to-eql-param [selection-tree [arg value]]
  (case arg
    :orderBy [:order-by (eqlify-order-by-param selection-tree value)]
    :where [:where (eqlify-where-predicate selection-tree value)]
    [arg value]))

(defn- parameters [selection-tree args]
  (->> (filter (fn [[k _]]
                 (reserved-args k)) args)
       (map #(to-eql-param selection-tree %))
       (into {})))

(declare properties)

(defn- field->prop [namespaces selection-tree field]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        prop         (->> (name field)
                          inf/hyphenate
                          (keyword root-attr-ns))]
    (if-let [{:keys [selections args]} (first (selection-tree field))]
      (let [parameters (parameters selections args)
            prop       (if (empty? parameters)
                         prop
                         (list prop parameters))]
        {prop (properties namespaces selections)})
      prop)))

(defn- properties [namespaces selection-tree]
  (vec (map #(field->prop namespaces selection-tree %) (keys selection-tree))))

(defn generate [namespaces selection-tree args]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        ident        (ident root-attr-ns args)
        parameters   (parameters selection-tree args)
        ident        (if (empty? parameters)
                       ident
                       (list ident parameters))
        properties   (properties namespaces selection-tree)
        eql          [{ident properties}]]
    (trace>> :eql eql)))