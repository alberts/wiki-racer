(ns wiki-racer.engine
  (:require [clojure.core.async :refer [<! >! go go-loop chan <!! >!! alts! chan close! thread]]
            [wiki-racer.state :as state]
            [wiki-racer.scraper :as scraper]
            [clojure.string :as str]))

(defn worker
  [worker-index]
  "The worker is a channel that listens to incoming requests for scraping wiki pages for links
  Once a page is scraped the worker updates the state of the application(new links found, pages-tracked)"
  (let [channel (chan 32)]
    (go-loop []
      (let [[level wiki] (<!! channel)
            page (scraper/scrape-page wiki)]
        (println wiki)
        (when (some? level)
          (state/update-visited! wiki)
          (state/update-tracker! page wiki)
          (state/update-worker-queue! page level)
          #_(println "finished work")
          (recur))))

    (println "workerend")
    channel))

(defn create-work-packets
  [level unvisited-urls]
  "Create packets of the form [depth, wiki-link] to pass on to workers"
  (mapv (fn [url] [level url]) unvisited-urls))

(defn distribute-work
  [level unvisited-urls workers]
  "Distribute work in a round robin fashion amongst the workers"
#_  (println "distirbuting work")
  (let [packets (create-work-packets level unvisited-urls)]
    (println packets)
    (doall (map-indexed (fn [i packet]
                          (println "sending" i packet)
                          (>!! (nth workers (mod i (count workers))) packet)
                          (println "sent"))
                        packets))))

(defn dispatcher
  [workers terminator initial-link end-link]
  "Dispatch pages to workers to scrape until an end link is found
  It runs in its own thread so that during tests I can inspect the repl for the current state of the app
  On finding a result it pushes the result out to a listening channel"
  (thread
    (while (not (state/found-destination? end-link))
      (let [[level unvisited-urls] (state/get-work! (count workers))]
        (when (nil? level) (Thread/sleep 2000))
        (if (state/found-destination? end-link)
          (do
            (doall (map #(close! %) workers))
            (>!! terminator (state/display-path initial-link end-link)))
          (distribute-work level unvisited-urls workers))))))

(defn run-engine
  [start-header end-header num-workers]
  "The engine runner that creates workers(channels) and a dispatcher and delegates to the dispatcher to
  find a path"
  (let [initial-link (str "/wiki/" (str/join "_" (str/split start-header #" ")))
        end-link (str "/wiki/" (str/join "_" (str/split end-header #" ")))
        terminator (chan)
        workers (mapv (fn [i] (worker i)) (range 0 7))]
    (state/update-worker-queue! {:header start-header :links [{:href initial-link}]} -1)
    (state/update-tracker! {:header start-header :links [{:href initial-link}]} initial-link)
    (dispatcher workers terminator initial-link end-link)
    (<!! terminator) ))

;;Tests during dev
#_ (def initial-link "/wiki/Mike_Tyson")
#_(def end-link "/wiki/Fruit_anatomy")
#_ (def terminator (chan))
#_(state/update-worker-queue! {:header "Start page" :links [{:href initial-link}]} -1)
#_(state/update-tracker! {:header "Start page" :links [{:href initial-link}]} initial-link)

#_(def workers (mapv (fn [i] (worker i)) (range 0 20)))
#_(dispatcher workers terminator initial-link end-link)
#_(close! work-channel)
#_(doall (map #(close! %) workers))




