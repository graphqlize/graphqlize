(ns demo
  (:require [graphqlize.lacinia.core :as l]
            [honeyeql-postgres.core :as heql-pg]))

(def db-spec {:dbtype   "postgresql"
              :dbname   "sakila"
              :user     "postgres"
              :password "postgres"})

(spit "./dev/lacinia-schema.edn" (l/schema db-spec))