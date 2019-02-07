(ns pharmacist.result
  "Functions and specs to support construction and processing of data source
  results."
  (:require #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])))

(s/def ::success? boolean?)
(s/def ::path (s/coll-of keyword?))
(s/def ::data any?)
(s/def ::raw-data any?)
(s/def ::attempts number?)
(s/def ::retryable? boolean?)
(s/def :pharmacist.cache/cached-at number?)

(s/def ::result (s/keys :req [::success?]
                        :opt [::raw-data ::data ::attempts ::retryable? :pharmacist.cache/cached-at]))

(s/def ::success?-args (s/cat :result ::result))

(defn success?
  "Returns true if this particular result was a success, as indicated by
  the `:pharmacist.result/success?` key"
  [result]
  (::success? result))

(s/fdef success?
  :args ::success?-args
  :ret boolean?)

(s/def ::success-args (s/or :unary (s/cat :data any?)
                            :binary (s/cat :data any?
                                           :config map?)))

(defn success
  "Create a successful result with data"
  [data & [config]]
  (merge
   {::success? true ::data data}
   config))

(s/fdef success
  :args ::success-args
  :ret ::result)

(s/def ::failure-args (s/or :nullary (s/cat)
                            :unary (s/cat :data any?)
                            :binary (s/cat :data any?
                                           :config (s/keys :opt [::retryable?]))))

(defn failure
  "Create a failed result, optionally with data and additional keys for the result.

```clojure
(require '[pharmacist.result :as result])

(result/failure {:message \"Oops!\"} {::result/retryable? true})
```"
  [& [data config]]
  (merge {::success? false}
         (when data {::data data})
         config))

(s/fdef failure
  :args ::failure-args
  :ret ::result)

(defn error
  "Create a failed result that caused an unexpected error - e.g. throwing an
  exception, not fulfilling fetch's contract, etc. `result` is whatever the
  original result was - possibly even an exception, and `error` is a map of:

| `:message`  | A helpful message trying to help the developer understand the problem |
| `:type`     | A keyword indicating the type of error |
| `:reason`   | An optional reason - one type of error may occur for different reasons |
| `:upstream` | The exception that caused this error, if any |"
  [result error]
  {::success? false
   ::original-result result
   ::error error})
