(ns graphqlize.lacinia.scalar
  (:import [java.util UUID]
           [java.time LocalDate LocalTime OffsetTime LocalDateTime OffsetDateTime]))

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
  (merge (scalar :UUID string? 
                 #(UUID/fromString %) str "UUID")
         (scalar :Date string? 
                 #(LocalDate/parse %) str 
                 "A date without a time-zone in the ISO-8601 calendar system, such as 2007-12-03 (java.time.LocalDate).")
         (scalar :Time string?
                 #(LocalTime/parse %) str
                 "A time without a time-zone in the ISO-8601 calendar system, such as 10:15:30 (java.time.LocalTime).")
         (scalar :TimeWithTimeZone string?
                 #(OffsetTime/parse %) str
                 "A time with an offset from UTC/Greenwich in the ISO-8601 calendar system, such as 10:15:30+01:00 (java.time.OffsetTime).")
         (scalar :DateTime string? 
                 #(LocalDateTime/parse %) str 
                 "A date-time without a time-zone in the ISO-8601 calendar system, such as 2007-12-03T10:15:30 (java.time.LocalDateTime).")
         (scalar :DateTimeWithTimeZone string? 
                 #(OffsetDateTime/parse %) str 
                 "A date-time with an offset from UTC/Greenwich in the ISO-8601 calendar system, such as 2007-12-03T10:15:30+01:00 (java.time.OffsetDateTime).")
         (scalar :BigDecimal number? 
                 bigdec identity 
                 "An arbitrary-precision signed decimal number (java.math.BigDecimal)")
         (scalar :Long number? 
                 long identity 
                 "The long data type is a 64-bit two's complement integer (java.lang.Long).")
         (scalar :BigInteger number? 
                 biginteger identity 
                 "An arbitrary-precision integer (java.math.BigInteger)")))
