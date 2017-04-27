(ns wiki-racer.state
  (:require [clojure.set :as set]))


(def application-state (atom {:visited-pages #{}
                              :tracked-links {}
                              :worker-queue  (ref {})}))

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
  (dosync
    (alter queue update depth concat urls)
    #_(println "added " (count urls))
    (:worker-queue @application-state)))

(defn update-worker-queue!
  [page current-depth]
  (when page
    #_(println "updating worker que with " (count (:links page)))
    #_(dosync
      (let [new-urls (unvisited-urls page)]
        (alter worker-queue update (inc current-depth) concat new-urls))))
  (swap! application-state update :worker-queue update-worker-queue* (inc current-depth) (unvisited-urls page))

  #_(swap! application-state update :worker-queue update (inc current-depth) concat (unvisited-urls page)))

(defn- update-on-work-requested
  [queue level links]
  (let [diff (set/difference (into #{} (get @queue level)) (into #{} links))]
    (if (empty? diff)
      (alter queue dissoc level)
      (alter queue assoc level diff))
    queue))


#_(defn get-work!
  []
  (dosync
    (let [[level links] (when (not-empty @worker-queue) (apply min-key (fn [[k _]] k) @worker-queue))
          diff (set/difference (into #{} links) (into #{} (get @worker-queue level)))]
      (if (empty? diff)
        (alter worker-queue dissoc level)
        (alter worker-queue assoc level diff))
      [level (into #{} links)])))

(defn get-work!
    [batch-size]
    (if (empty? @(:worker-queue @application-state))
      []
      (dosync
        (let [[level links] (apply min-key (fn [[k _]] k) @(:worker-queue @application-state))]
          (swap! application-state update :worker-queue update-on-work-requested level (take batch-size links))
          (println (count links) (count @(:worker-queue @application-state)) level)
          [level (take batch-size links)]))))


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

