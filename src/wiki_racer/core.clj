(ns wiki-racer.core
  (:require
    [clojure.core.async :refer [<! >! go go-loop chan <!! >!! alts!] :as async]
    [wiki-racer.scraper :as scraper]
    [clojure.set :as set])

  (:gen-class))

(def visited-pages (atom #{}))
(def tracker (atom {}))

(defn update-visited!
  [wiki-path]
  (swap! visited-pages conj wiki-path))

(defn update-tracker!
  [{:keys [header links]}]
  (doall
    (map #(swap! tracker assoc (:title %) header) links)))

(defn new-urls
  [visited-pages {:keys [links]}]
  (set/difference (into #{} (map :href links)) visited-pages))

(defn found-destination?
  [tracker final-page-title]
  (contains? tracker final-page-title))

(defn async-worker
  [response-channel final-page-title]
  (let [channel (async/chan 130890)]
    (go-loop []
      (let [wiki (<! channel)
            page (scraper/scrape-page wiki)]
        (update-visited! wiki)
        (update-tracker! page)
        (when-not (found-destination? @tracker final-page-title)
          (go (doall (map #(>!! response-channel %) (new-urls @visited-pages page))))
          (recur))))
    channel))

(defn display-path
  [tracker start-title end-title]
  (println (loop [acc []
                  current-end end-title]
             (if (= current-end start-title)
               (reverse (conj acc current-end))
               (recur (conj acc current-end) (get tracker current-end))))))


(defn coordinator
  [workers work-channel initial-link start-title end-title]
  (>!! work-channel initial-link)
  (go-loop
    [current-worker-index 0]
    (let [next-url (<! work-channel)
          worker (nth workers (mod current-worker-index (count workers)))]
      (if (found-destination? @tracker end-title)
        (display-path @tracker start-title end-title)
        (do
          (go (>!! worker next-url))
          (recur (inc current-worker-index)))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [work-channel (async/chan (* 50 65535))
        workers (map (fn [_] (async-worker work-channel "Segment")) (range 0 50))]
    (coordinator workers work-channel "/wiki/Mike_Tyson" "Mike Tyson" "Segment"))
  (while true (Thread/sleep 100000))

  (println "Hello, World!"))

#_(def work-channel (async/chan (* 6 65535)))
#_(def workers (map (fn [_] (async-worker work-channel "Segment")) [0 0 0 0 0 0 0]))
#_(coordinator workers work-channel "/wiki/Mike_Tyson" "Mike Tyson" "Segment")

