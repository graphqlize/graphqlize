(ns graphqlize.lacinia.core
  (:require [honeyeql.meta-data :as heql-md]
            [graphqlize.lacinia.object :as l-obj]))

(defn schema [db-spec]
  (let [heql-meta-data (heql-md/fetch db-spec)]
    {:objects (l-obj/generate heql-meta-data)}))