(ns wiki-racer.engine
  (:require [chime :as chime]
            [clj-time.core :as time]
            [clj-time.periodic :as periodic]
            [clojure.core.async :refer [<! >! go go-loop chan <!! >!! alts! chan close! thread]]
            [clojure.string :as str]
            [wiki-racer.state :as state]
            [wiki-racer.scraper :as scraper]))

(defn worker
  [worker-index worker-queue]
  "The worker is a channel that listens to incoming requests for scraping wiki pages for links
  Once a page is scraped the worker updates the state of the application(new links found, pages-tracked)"
  (let [channel (chan)]
    (go
      (loop []
        (let [[level wiki] (<! channel)
              page (scraper/scrape-page wiki)]
          (when (some? level)
            (state/update-visited! wiki)
            (state/update-tracker! page wiki)
            #_(state/update-worker-queue! page level)
            (doseq [{:keys [href _]} (:links page)]
              (go (>!! worker-queue [(inc level) href])))
            (recur)))))
    channel))

(defn create-work-packets
  [level unvisited-urls]
  "Create packets of the form [depth, wiki-link] to pass on to workers"
  (mapv (fn [url] [level url]) unvisited-urls))

(defn distribute-work
  [level unvisited-urls workers]
  "Distribute work in a round robin fashion amongst the workers"
  (let [packets (create-work-packets level unvisited-urls)]
    (doall (map-indexed (fn [i packet]
                          (>!! (nth workers (mod i (count workers))) packet))
                        packets))))

(defn dispatcher
  [workers terminator work-queue initial-link end-link]
  "Dispatch pages to workers to scrape until an end link is found
  It runs in its own thread so that during tests I can inspect the repl for the current state of the app
  On finding a result it pushes the result out to a listening channel"
  (go-loop
    [index 0]
    (if (state/found-destination? end-link)
      (>!! terminator (state/source->dest initial-link end-link))
      (when-let [packet (<! work-queue)]
        (go (>!! (nth workers (mod index (count workers))) packet))
        (recur (inc index)))))
  #_(thread
    (while (not (state/found-destination? end-link))
      (let [[level unvisited-urls] (state/get-work! (count workers))]
        (when (nil? level) (println "Waiting for work") (Thread/sleep 1000)) ;;undesirable but its a hack now
        (distribute-work level unvisited-urls workers)))
    (when (state/found-destination? end-link)
      #_(doall (map #(close! %) workers))                   ;;undesirable but it causes nils to to be printed
      (>!! terminator (state/source->dest initial-link end-link)))))

(defn run-engine
  [start-header end-header num-workers]
  "The engine runner that creates workers(channels) and a dispatcher and delegates to the dispatcher to
  find a path"
  (let [initial-link (str "/wiki/" (str/join "_" (str/split start-header #" ")))
        end-link     (str "/wiki/" (str/join "_" (str/split end-header #" ")))
        terminator   (chan)
        work-queue   (chan (* num-workers 2048))
        workers      (mapv (fn [i] (worker i work-queue)) (range 0 num-workers))
        chimes       (chime/chime-ch (periodic/periodic-seq (time/now) (time/millis 5000)))]
    #_(state/update-worker-queue! {:header start-header :links [{:href initial-link}]} -1)
    (>!! work-queue [0 initial-link])
    (state/update-tracker! {:header start-header :links [{:href initial-link}]} initial-link)
    (dispatcher workers terminator work-queue initial-link end-link)
    (go-loop []
      (when-let [_ (<! chimes)]
        (clojure.pprint/pprint (state/summary))
        (recur)))
    (<!! terminator)))

;;Tests during dev
#_(def initial-link "/wiki/Mike_Tyson")
#_(def end-link "/wiki/Fruit_anatomy")
#_(def terminator (chan))
#_(state/update-worker-queue! {:header "Start page" :links [{:href initial-link}]} -1)
#_(state/update-tracker! {:header "Start page" :links [{:href initial-link}]} initial-link)

#_(def workers (mapv (fn [i] (worker i)) (range 0 100)))
#_(dispatcher workers terminator initial-link end-link)
#_(close! work-channel)
#_(doall (map #(close! %) workers))
#_(state/found-destination? end-link)



