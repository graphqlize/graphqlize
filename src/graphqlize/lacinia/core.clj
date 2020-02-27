(ns graphqlize.lacinia.core
  (:require [honeyeql.meta-data :as heql-md]
            [com.walmartlabs.lacinia.schema :as lacinia-schema]
            [com.walmartlabs.lacinia.resolve :as lacinia-resolve]
            [com.walmartlabs.lacinia.executor :as executor]
            [com.walmartlabs.lacinia.util :as lacinia-util]
            [graphqlize.lacinia.object :as l-obj]
            [graphqlize.lacinia.query :as l-query]
            [graphqlize.lacinia.scalar :as l-scalar]
            [graphqlize.lacinia.eql :as l-eql]
            [honeyeql.core :as heql]
            [honeyeql.db :as heql-db]
            [honeyeql.debug :refer [trace>>]]))

(defn- hql-resolver [db-adapter heql-query-fn]
  ^{:tag lacinia-resolve/ResolverResult}
  (fn [context args _]
    (let [sel-tree (executor/selections-tree context)
          eql      (-> (heql/meta-data db-adapter)
                       heql-md/namespace-idents
                       (l-eql/generate sel-tree args))]
      (trace>> :lacinia-resolver {:selections-tree sel-tree
                                  :args            args})
      (try
        (->> (heql-query-fn db-adapter eql)
             (trace>> :resolved-value)
             lacinia-resolve/resolve-as)
        (catch Throwable e
          (trace>> :heql-error e)
          (lacinia-resolve/resolve-as nil (lacinia-util/as-error-map e)))))))

(defn- resolvers [db-adapter]
  {:graphqlize/query-by-primary-key (hql-resolver db-adapter heql/query-single)
   :graphqlize/collection-query     (hql-resolver db-adapter heql/query)})

(defn schema [db-spec]
  (let [db-adapter     (heql-db/initialize db-spec {:field/naming-convention :unqualified-camel-case})
        heql-meta-data (heql/meta-data db-adapter)
        gql-schema     {:objects (l-obj/generate heql-meta-data)
                        :queries (l-query/generate heql-meta-data)
                        :scalars (l-scalar/generate)}]
    (trace>> :gql-schema gql-schema)
    (lacinia-schema/compile
     (lacinia-util/attach-resolvers gql-schema (resolvers db-adapter)))))