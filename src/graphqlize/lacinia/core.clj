(ns graphqlize.lacinia.core
  (:require [honeyeql.meta-data :as heql-md]
            [com.walmartlabs.lacinia.schema :as lacinia-schema]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [graphqlize.lacinia.object :as l-obj]
            [graphqlize.lacinia.query :as l-query]))

(defn- resolvers [db-spec]
  {:graphqlize/query-by-primary-key
   (fn [context args value]
     nil)})

(defn schema [db-spec]
  (let [heql-meta-data (heql-md/fetch db-spec)
        gql-schema     {:objects (l-obj/generate heql-meta-data)
                        :queries (l-query/generate heql-meta-data)}]
    (tap> {:gql-schema gql-schema})
    (lacinia-schema/compile
     (lacinia-util/attach-resolvers gql-schema (resolvers db-spec)))))