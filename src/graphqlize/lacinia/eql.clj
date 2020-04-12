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

(def ^:private reserved-args #{:limit :offset :orderBy})

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

(defn- eqlify-order-by-param [root-attr-ns param]
  (map (fn [[k v]]
         [(->> (name k)
               inf/hyphenate
               (keyword (name root-attr-ns)))
          (-> (name v)
              string/lower-case
              keyword)]) param))

#_(eqlify-order-by-param "actor"
                         {:firstName :ASC
                          :lastName  :DESC})

(defn- to-eql-param [root-attr-ns [arg value]]
  (prn root-attr-ns arg value)
  (case arg
    :orderBy [:order-by (eqlify-order-by-param root-attr-ns value)]
    [arg value]))

(defn- parameters [root-attr-ns args]
  (->> (filter (fn [[k _]]
                 (reserved-args k)) args)
       (map #(to-eql-param root-attr-ns %))
       (into {})))

#_ (parameters "actor" {:limit 10 :offset 10})

(declare properties)

#_{:selections-tree #:Actor{:actorId   [nil]
                            :firstName [nil]
                            :films     [{:args       {:limit 2}
                                         :selections #:Film{:title [nil]}}]}
   :args            {:limit  2
                     :offset 10}}

#_{:selections-tree #:Actor{:actorId   [nil]
                            :firstName [nil]
                            :films     [{:selections #:Film{:title [nil]}}]}
   :args            {:limit  2
                     :offset 10}}

(defn- field->prop [namespaces selection-tree field]
  (let [root-attr-ns (eql-root-attr-ns namespaces selection-tree)
        prop         (->> (name field)
                          inf/hyphenate
                          (keyword root-attr-ns))]
    (if-let [{:keys [selections args]} (first (selection-tree field))]
      (let [parameters (parameters root-attr-ns args)
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
        parameters   (parameters root-attr-ns args)
        ident        (if (empty? parameters)
                       ident
                       (list ident parameters))
        properties   (properties namespaces selection-tree)
        eql          [{ident properties}]]
    (trace>> :eql eql)))