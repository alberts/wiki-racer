(ns wiki-racer.worker)

(defprotocol Worker
  (acceptWork [this wikiPage]))

