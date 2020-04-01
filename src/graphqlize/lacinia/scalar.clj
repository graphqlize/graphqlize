(ns graphqlize.lacinia.scalar
  (:import [java.util UUID]
           [java.time LocalDateTime OffsetDateTime]))

(defn- scalar [scalar-type data-type? parse-fn serialize-fn description]
  {scalar-type {:parse     #(when (data-type? %)
                              (try
                                (parse-fn %)
                                (catch Throwable _
                                  nil)))
                :description description
                :serialize #(try
                              (serialize-fn %)
                              (catch Throwable _
                                nil))}})

(defn generate []
  (merge (scalar :UUID string? #(UUID/fromString %) str "UUID")
         (scalar :DateTime string? #(LocalDateTime/parse %) str "A date-time without a time-zone in the ISO-8601 calendar system, such as 2007-12-03T10:15:30.")
         (scalar :DateTimeWithTimeZone string? #(OffsetDateTime/parse %) str "A date-time with an offset from UTC/Greenwich in the ISO-8601 calendar system, such as 2007-12-03T10:15:30+01:00.")))
