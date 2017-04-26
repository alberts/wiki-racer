(ns wiki-racer.state
  (:require [clojure.set :as set]))


(def application-state (atom {:visited-pages #{}
                              :tracked-links {}
                              :worker-queue (ref {})}))

(def worker-queue (ref {}))

(defn update-visited!
  [wiki-path]
  (swap! application-state update :visited-pages conj wiki-path))

(defn update-tracker!
  [{:keys [links]} source-link]
  (doall
    (map #(swap! application-state update :tracked-links assoc (:href %) source-link) links)))

(declare unvisited-urls)
(defn- update-worker-queue*
  [queue depth urls]
  (println queue)
  (dosync
    (alter queue update depth concat urls)
    (:worker-queue @application-state)))

(defn update-worker-queue!
  [page current-depth]
  (println "updating worker queue" page)
  (swap! application-state update :worker-queue update-worker-queue* (inc current-depth) (unvisited-urls page))

  #_(swap! application-state update :worker-queue update (inc current-depth) concat (unvisited-urls page)))

(defn- update-on-work-requested
  [queue level links]
  (dosync
    (let [diff (set/difference (into #{} links) (into #{} (get @queue level)))]
      (if (empty? diff)
        (alter queue dissoc level)
        (alter queue assoc level diff))
      (swap! application-state assoc :worker-queue queue))))


(defn get-work!
  []
  (if (empty? (:worker-queue @application-state))
    []
    (let [[level links] (apply min-key (fn [[k _]] k) (:worker-queue @application-state))
          ]
      (swap! application-state update :worker-queue assoc level (set/difference (into #{} links) (into #{} (get-in @application-state [:worker-queue level]))))
      (swap! application-state update :worker-queue update-on-work-requested level links)
      (println "found work " (count links))
      [level links])))


(defn found-destination?
  [final-page-link]
  (contains? (get @application-state :tracked-links) final-page-link))

(defn unvisited-urls
  [{:keys [links]}]
  (set/difference (into #{} (map :href links)) (:visited-pages @application-state)))

(defn display-path
  [start-link end-link]
  (let [tracker (:tracked-links @application-state)]
    (loop [acc []
           current-end end-link]
      (if (= current-end start-link)
        (reverse (conj acc current-end))
        (recur (conj acc current-end) (get tracker current-end))))))

