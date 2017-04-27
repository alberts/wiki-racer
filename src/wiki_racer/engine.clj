(ns wiki-racer.engine
  (:require [clojure.core.async :refer [<! >! go go-loop chan <!! >!! alts! chan close!]]
            [wiki-racer.state :as state]
            [wiki-racer.scraper :as scraper]))

(defn worker
  [worker-index]
  (let [channel (chan 2)]
    (go-loop []
      (let [[level wiki] (<!! channel)
            page (scraper/scrape-page wiki)]
        (when (some? level)
          (state/update-visited! wiki)
          (state/update-tracker! page wiki)
          (state/update-worker-queue! page level)
          (recur))))
    channel))

(defn create-work-packets
  [level unvisited-urls]
  (mapv (fn [url] [level url]) unvisited-urls))

(defn distribute-work
  [level unvisited-urls workers]
  (let [packets (create-work-packets level unvisited-urls)]
    (doall (map-indexed (fn [i packet]
                          (>!! (nth workers (mod i (count workers))) packet))
                        packets))))


(defn dispatcher
  [workers initial-link end-link]

  (go-loop
    []
    (let [[level unvisited-urls] (state/get-work! (count workers))]
      (cond
        (state/found-destination? end-link) (println (state/display-path initial-link end-link))
        :else (do (distribute-work level unvisited-urls workers)
                  (recur))))))

(def initial-link  "/wiki/Mike_Tyson")
(def end-link  "/wiki/Greek_language")
 #_(state/update-worker-queue! {:header "Start page" :links [{:href initial-link}]} -1)
 #_(state/update-tracker! {:header "Start page" :links [{:href initial-link}]} initial-link)

  #_(def workers (mapv (fn [i] (worker i)) (range 0 5)))
  #_(dispatcher workers initial-link end-link)
  #_(close! work-channel)
  #_(doall (map #(close! %) workers))

