(ns wiki-racer.scraper
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as str])
  (:import (java.net URL)))

(def ^:dynamic *base-url* "https://en.wikipedia.org")

(declare fetch-wiki-page)
(declare get-links)
(declare get-title)
(declare fetch-url)
(declare distinct-by)

(defn scrape-page
  [wiki]
  "scrape a wiki page to get a map containing the the following
  :header -> the title of the page
  :links -> the links each of which contain the title(header) and the relative url within wikipedia"
  #_(println wiki)
  (let [page (fetch-wiki-page wiki)]
    {:header (get-title page)
     :links  (get-links page)}))

(defn fetch-wiki-page
  [wiki]
  (-> (str *base-url* wiki)
      (fetch-url)))

(defn get-title
  [page]
  (->> (html/select page [:h1#firstHeading])
       first
       html/text))

(defn get-links
  [page]
  (->> (html/select page [(html/attr? :href :title)])
       (map (fn [element] [(get-in element [:attrs :title]) (first (html/attr-values element :href))]))
       (map #(zipmap [:title :href] %1))
       (filter #(.startsWith (:href %) "/wiki"))
       (distinct-by :href)))

(defn- fetch-url
  [url]
  (try
    (with-open [inputstream (-> (URL. url)
                                .openConnection
                                (doto (.setInstanceFollowRedirects true)
                                      (.setConnectTimeout 15000)
                                      (.setReadTimeout 15000))
                                .getContent)]
      (html/html-resource inputstream))
    (catch java.io.FileNotFoundException e (prn url " is not a valid page") [])))

(defn- distinct-by
  [keyfn coll]
  (let [step (fn step [xs seen]
               (lazy-seq
                 ((fn [[v :as xs] seen]
                    (when-let [s (seq xs)]
                      (let [v* (keyfn v)]
                        (if (contains? seen v*)
                          (recur (rest s) seen)
                          (cons v (step (rest s) (conj seen v*)))))))
                   xs seen)))]
    (step coll #{})))



