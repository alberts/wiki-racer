(ns wiki-racer.state-test
  (:require [wiki-racer.state :refer :all]
            [midje.sweet :refer :all]))



(fact "state should update visited appropriately"
      (reset-state)
      (update-visited! "/wiki/path1")
      (update-visited! "/wiki/path1")
      (update-visited! "/wiki/path2")
      (:visited-pages @application-state) => #{"/wiki/path1" "/wiki/path2"})

(fact "state should update tracker appropriately"
      (reset-state)
      (update-tracker! {:links [{:href "/wiki/path1" :title "title1"}]} "/wiki/source")
      (update-tracker! {:links [{:href "/wiki/path2" :title "title1"}]} "/wiki/source")
      (:tracked-links @application-state) => {"/wiki/path1" "/wiki/source"
                                              "/wiki/path2" "/wiki/source"})

(fact "worker queue should update correctly"
      (reset-state)
      (update-worker-queue! {:header "title" :links[{:href "/wiki/path1"} {:href "/wiki/path2"}]} 0)
      @(:worker-queue @application-state) => {1 ["/wiki/path1" "/wiki/path2"]})

(fact "worker queue should return jobs correctly"
      (reset-state)
      (update-worker-queue! {:header "title" :links[{:href "/wiki/path1"} {:href "/wiki/path2"}]} 0)
      (get-work! 1) => [1 ["/wiki/path1"]]
      @(:worker-queue @application-state) => {1 #{"/wiki/path2"}})

(fact "state should provide a path between source and destination"
      (reset-state)
      (update-tracker! {:links [{:href "/wiki/dest1" :title "title1"}]} "/wiki/source")
      (update-tracker! {:links [{:href "/wiki/dest2" :title "title1"}]} "/wiki/dest1")
      (update-tracker! {:links [{:href "/wiki/dest3" :title "title1"}]} "/wiki/dest1")
      (update-tracker! {:links [{:href "/wiki/final" :title "title1"}]} "/wiki/dest3")
      (source->dest "/wiki/source" "/wiki/final") => ["/wiki/source" "/wiki/dest1" "/wiki/dest3" "/wiki/final"])

