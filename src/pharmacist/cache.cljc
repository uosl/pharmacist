(ns pharmacist.cache
  "Caching tools for use with Pharmacist prescriptions"
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [pharmacist.data-source :as data-source]))

(s/def ::path (s/coll-of keyword?))
(s/def ::prescription (s/keys :req [::data-source/id ::data-source/params]))
(s/def ::cache-path-args (s/cat :prescription ::prescription))

(defn cache-path
  "Given a path (keyword) and a data source, return the string cache key. Combines
  the path with parameters sorted into a query string."
  [prescription]
  (str (::data-source/id prescription)
       "/"
       (->> (::data-source/params prescription)
            (sort-by first)
            (map #(str/join "=" %))
            (str/join "&"))))

(s/fdef cache-path
  :args ::cache-path-args
  :ret string?)

(s/def ::cache #(instance? clojure.lang.IRef %))
(s/def ::cache-get-args (s/cat :cache ::cache :path ::path :prescription ::prescription))

(defn cache-get
  "Look up data source in the cache. Expects all parameters in `prescription` to
  be dependency resolved and fully realized."
  [cache path prescription]
  (get @cache (cache-path prescription)))

(s/fdef cache-get
  :args ::cache-get-args
  :ret (s/or :nil nil? :val any?))

(s/def ::cache-put-args (s/cat :cache ::cache
                               :path ::path
                               :prescription ::prescription
                               :value any?))

(defn cache-put
  "Put item in cache. Expects all parameters in `prescription` to be dependency
  resolved and fully realized."
  [cache path prescription value]
  (swap! cache assoc (cache-path prescription) value)
  nil)

(s/fdef cache-put
  :args ::cache-put-args
  :ret nil?)

(s/def ::atom-map-args (s/cat :cache ::cache))
(s/def ::cache-get fn?)
(s/def ::cache-put fn?)
(s/def ::params (s/keys :req-un [::cache-get ::cache-put]))

(defn atom-map
  "Given a ref to use as a cache, returns a map of parameters to pass to
  `pharmacist.prescription/fill` in order to look up and store loaded data in
  the cache."
  [ref]
  {:cache-get (partial cache-get ref)
   :cache-put (partial cache-put ref)})

(s/fdef atom-map
  :args ::atom-map-args
  :ret ::params)
