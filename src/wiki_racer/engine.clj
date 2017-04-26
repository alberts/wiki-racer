(ns wiki-racer.engine
  (:require [clojure.core.async :refer [<! >! go go-loop chan <!! >!! alts! chan]]
            [wiki-racer.state :as state]
            [wiki-racer.scraper :as scraper]))

(defn worker
  [response-channel end-link]
  (let [channel (chan 1024)]
    (go-loop []
      (let [[level wiki] (<! channel)
            page (scraper/scrape-page wiki)]
        (println "received work" level wiki)
        (state/update-visited! wiki)
        (state/update-tracker! page wiki)
        (when-not (state/found-destination? end-link)
          (go (>!! response-channel [page level]))
          (recur))))
    channel))

(defn create-work-packets
  [level unvisited-urls]
  (mapv (fn [url] [level url]) unvisited-urls))

(defn distribute-work
  [level unvisited-urls workers]
  (let [packets (create-work-packets level unvisited-urls)]
    (go (doall (map-indexed (fn [i packet]
                              (>!! (nth workers (mod i (count workers))) packet))
                            packets)))))


(defn dispatcher
  [workers new-work-channel initial-link end-link]
  (state/update-worker-queue! {:header "Start page" :links [{:href initial-link}]} -1)
  (state/update-tracker! {:header "Start page" :links [{:href initial-link}]} initial-link)
  (go-loop
    [_ ""]
    (let [[level unvisited-urls] (state/get-work!)]
      (cond
        (state/found-destination? end-link) (println (state/display-path initial-link end-link))
        (empty? unvisited-urls) (->> (<! new-work-channel)
                                      (apply state/update-worker-queue!)
                                      recur)
        :else (do (distribute-work level unvisited-urls workers)
                  (recur ""))))))


#_(def work-channel (chan 8192))
#_(def workers (mapv (fn [_] (worker work-channel "Segment")) (range 0 20)))
#_(dispatcher workers work-channel "/wiki/Mike_Tyson" "/wiki/Fruit_anatomy")

