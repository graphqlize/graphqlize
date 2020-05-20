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

(def ^:private reserved-args #{:limit :offset :orderBy :where :groupBy})

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

(defn- eqlify-order-by-param [namespaces selection-tree param]
  (map (fn [[k v]]
         (let [root-ns (eql-root-attr-ns namespaces selection-tree)]
           [(->> (name k)
                 inf/hyphenate
                 (keyword root-ns))
            (-> (name v)
                string/lower-case
                keyword)])) param))

#_(eqlify-order-by-param [:public] {:City/city [nil]}
                         {:firstName :ASC
                          :lastName  :DESC})

(defn- eqlify-group-by-param [namespaces selection-tree value]
  (let [root-ns (eql-root-attr-ns namespaces selection-tree)]
    (map #(keyword root-ns (inf/hyphenate (name %))) value)))

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

(defn- eql-attr [root-ns field]
  (->> (name field)
       inf/hyphenate
       (keyword root-ns)))

(defn- where-predicate [root-ns field pred]
  (let [column (eql-attr root-ns field)
        [op v] (first pred)]
    (if-let [pred-fn (hql-predicate-fn op)]
      (pred-fn column v)
      (let [col    [column (-> (ffirst pred) name keyword)]
            [op v] (first v)]
        ((hql-predicate-fn op) col v)))))

#_(ffirst {:country {:eq "Algeria"}})
#_(where-predicate "city" :country {:country {:eq "Algeria"}})
#_(where-predicate "city" :cityId {:eq 1})

(defn- where-clause [root-ns xs]
  (mapcat
   #(map (fn [[k v]]
           (case k
             :and (if (seq v) (concat [:and] (where-clause root-ns v)) [])
             :or (if (seq v) (concat [:or] (where-clause root-ns v)) [])
             :not (if (seq v) (concat [:not] (where-clause root-ns [v])) [])
             :have (concat [:exists (eql-attr root-ns v)])
             (where-predicate root-ns k v))) %)
   xs))

#_(where-clause "payment" [{:not {:or [{:name {:eq "English"}} {:name {:eq "French"}}]}}])

#_(where-clause "city" [{:country {:country {:eq "Algeria"}}}])

#_(where-clause "author" [{:have :courses}])

(defn- eqlify-where-predicate [namespaces selection-tree param]
  (-> (eql-root-attr-ns namespaces selection-tree) 
      (where-clause [param]) 
      first))

#_(eqlify-where-predicate
   [:public]
   #:Payment{:paymentId [nil]
             :amount    [nil]}
   {:and [{:amount     {:gt 5.99M}
           :customerId {:eq 1}}]})

#_(eqlify-where-predicate
   [:public]
   {:Language/languageId [nil]
    :Language/name       [nil]}
   {:not {:or [{:name {:eq "English"}} {:name {:eq "French"}}]}})

#_(eqlify-where-predicate [:public]
                          {:Actor/firstName [nil]
                           :Actor/lastName  [nil]}
                          {:actorId {:eq 1}
                           :name    {:eq "foo"}})

(defn- to-eql-param [namespaces selection-tree [arg value]]
  (case arg
    :orderBy [:order-by (eqlify-order-by-param namespaces selection-tree value)]
    :where (when-let [pred (seq (eqlify-where-predicate namespaces selection-tree value))]
             [:where pred])
    :groupBy [:group-by (eqlify-group-by-param namespaces selection-tree value)]
    [arg value]))

(defn- parameters [namespaces selection-tree args]
  (->> (filter (fn [[k _]]
                 (reserved-args k)) args)
       (map #(to-eql-param namespaces selection-tree %))
       (into {})))

(declare properties)

(defn- resolve-aggregate-column [prop]
  (let [n (namespace prop)
        k (name prop)]
    (if-let [agg-prop (some (fn [prefix]
                     (when (string/starts-with? k (str prefix "-"))
                       [(keyword (string/replace-first prefix #"-of$" ""))
                        (keyword n (string/replace-first k (re-pattern (str "^" prefix "-")) ""))])) 
                   ["count-of" "avg-of" "sum-of" "min-of" "max-of"])]
      agg-prop
      prop)))

#_(resolve-aggregate-column :course/avg-of-rating)

(defn- field->prop [namespaces selection-tree field]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        prop         (->> (name field)
                          inf/hyphenate
                          (keyword root-attr-ns))]
    (if-let [{:keys [selections args]} (first (selection-tree field))]
      (let [parameters (parameters namespaces selections args)
            prop       (if (empty? parameters)
                         prop
                         (list prop parameters))]
        {prop (properties namespaces selections)})
      (resolve-aggregate-column prop))))

#_(field->prop [:public] {:Course/countOfRating [nil]
                          :Course/maxOfRating   [nil]
                          :Course/sumOfRating   [nil]} :Course/countOfRating)

(defn- properties [namespaces selection-tree]
  (vec (map #(field->prop namespaces selection-tree %) (keys selection-tree))))

(defn generate [namespaces selection-tree args]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        ident        (ident root-attr-ns args)
        parameters   (parameters namespaces selection-tree args)
        ident        (if (empty? parameters)
                       ident
                       (list ident parameters))
        properties   (properties namespaces selection-tree)
        eql          [{ident properties}]]
    (trace>> :eql eql)))

#_(generate [:public] {:Course/countOfRating [nil]
                       :Course/maxOfRating   [nil]
                       :Course/sumOfRating   [nil]} nil)