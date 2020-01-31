(ns graphqlize.lacinia.scalar)

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
  (scalar :UUID string? #(str (java.util.UUID/fromString %)) str))
