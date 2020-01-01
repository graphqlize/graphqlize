(ns graphqlize.lacinia.core
  (:require [honeyeql.meta-data :as heql-md]
            [com.walmartlabs.lacinia.schema :as lacinia-schema]
            [com.walmartlabs.lacinia.resolve :as lacinia-resolve]
            [com.walmartlabs.lacinia.executor :as executor]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [graphqlize.lacinia.object :as l-obj]
            [graphqlize.lacinia.query :as l-query]
            [graphqlize.lacinia.eql :as l-eql]
            [honeyeql.core :as heql]
            [honeyeql.debug :refer [trace>>]]))

(defn- query-by-primary-key-resolver [db-spec heql-meta-data]
  ^{:tag lacinia-resolve/ResolverResult}
  (fn [context args _]
    (let [sel-tree    (executor/selections-tree context)
          eql         (l-eql/generate (heql-md/namespace-idents heql-meta-data) sel-tree args)
          heql-config {:attribute {:return-as :unqualified-camel-case}}]
      (trace>> :lacinia-resolver {:selections-tree sel-tree
                                  :args            args})
      (try
        (->> (heql/query-single db-spec heql-config heql-meta-data eql)
             (trace>> :resolved-value)
             lacinia-resolve/resolve-as)
        (catch Throwable e
          (trace>> :heql-error e)
          (lacinia-resolve/resolve-as nil (lacinia-util/as-error-map e)))))))

(defn- resolvers [db-spec heql-meta-data]
  {:graphqlize/query-by-primary-key (query-by-primary-key-resolver db-spec heql-meta-data)})

(defn schema [db-spec]
  (let [heql-meta-data (heql-md/fetch db-spec)
        gql-schema     {:objects (l-obj/generate heql-meta-data)
                        :queries (l-query/generate heql-meta-data)}]
    (trace>> :gql-schema gql-schema)
    (lacinia-schema/compile
     (lacinia-util/attach-resolvers gql-schema (resolvers db-spec heql-meta-data)))))