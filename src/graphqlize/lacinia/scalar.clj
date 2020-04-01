(ns graphqlize.lacinia.scalar
  (:import [java.util UUID]
           [java.time LocalDateTime]))

(defn- scalar [scalar-type data-type? parse-fn serialize-fn]
  {scalar-type {:parse     #(when (data-type? %)
                              (try
                                (parse-fn %)
                                (catch Throwable _
                                  nil)))
                :serialize #(try
                              (serialize-fn %)
                              (catch Throwable _
                                nil))}})

(defn generate []
  (merge (scalar :UUID string? #(UUID/fromString %) str)
         (scalar :DateTime string? #(LocalDateTime/parse %) str)))
