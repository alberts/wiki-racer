(ns wiki-racer.core
  (:require
    [wiki-racer.engine :as engine]
    [clojure.set :as set]
    [clojure.string :as str]
    [clojure.tools.cli :as cli])
  (:gen-class))

(def cli-options
  [["-n" "--workers WORKERS" "number of workers default 5" :default 5 :parse-fn #(Integer/parseInt %)]
   ["-s" "--start START" "the start wiki header"]
   ["-e" "--end END" "the end wiki header"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["This program finds a path between two wiki pages"
        ""
        "Usage: java -jar wiki-racer-0.1.0-SNAPSHOT-standalone.jar [options]"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (nil? args)    {:exit-message (usage summary) :ok? true}
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
      (or (empty (:start options)) (empty (:end options))) {:exit-message (error-msg "start and end must be specified") :ok? false}
      :else {:options options :ok? true})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (println (engine/run-engine
                 (:start options)
                 (:end options)
                 (:workers options))))))

