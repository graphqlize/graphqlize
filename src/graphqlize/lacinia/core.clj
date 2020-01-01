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

(defn- query-by-primary-key-resolver [db-adapter]
  ^{:tag lacinia-resolve/ResolverResult}
  (fn [context args _]
    (let [sel-tree    (executor/selections-tree context)
          eql         (-> (heql/meta-data db-adapter)
                          heql-md/namespace-idents
                          (l-eql/generate sel-tree args))]
      (trace>> :lacinia-resolver {:selections-tree sel-tree
                                  :args            args})
      (try
        (->> (heql/query-single db-adapter eql)
             (trace>> :resolved-value)
             lacinia-resolve/resolve-as)
        (catch Throwable e
          (trace>> :heql-error e)
          (lacinia-resolve/resolve-as nil (lacinia-util/as-error-map e)))))))

(defn- resolvers [db-adapter]
  {:graphqlize/query-by-primary-key (query-by-primary-key-resolver db-adapter)})

(defn schema [db-adapter]
  (let [heql-config    {:attribute {:return-as :unqualified-camel-case}}
        new-db-adapter (heql/merge-config db-adapter heql-config)
        heql-meta-data (heql/meta-data new-db-adapter)
        gql-schema     {:objects (l-obj/generate heql-meta-data)
                        :queries (l-query/generate heql-meta-data)}]
    (trace>> :gql-schema gql-schema)
    (lacinia-schema/compile
     (lacinia-util/attach-resolvers gql-schema (resolvers new-db-adapter)))))