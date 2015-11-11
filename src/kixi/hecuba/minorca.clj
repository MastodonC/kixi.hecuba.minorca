(ns kixi.hecuba.minorca
  (:gen-class :main true)
  (:require [clj-http.client         :as client]
            [clojure.data.csv        :as csv]
            [clojure.data.json       :as json]
            [clojure.java.io         :as io]
            [clojure.tools.logging   :as log]
            [kixi.hecuba.api-helpers :as api]
            [clojure.set             :as set]))

(def houses-mapping "resources/houses-mapping.csv")

(defn open-input-file
  "Read a csv and output a seq of maps containing the file data."
  [input-file]
  (let [data-seq (with-open [in-file (io/reader input-file)]
                   (doall (csv/read-csv in-file)))]
    (map #(zipmap (mapv keyword (first data-seq)) %)
         (rest data-seq))))

(defn select-identifiers
  "Take a seq of maps and return a set of values 
  for the key passed in."
  [input-data key-to-select]
  (set (map key-to-select input-data)))

(defn look-up
  "Get two sets of ids and return the ids that
  are in the first set and aren't in the second set."
  [houses-ids ids-mapping]
  (set/difference houses-ids ids-mapping))

(defn pre-processing
  [input-file mapping-file project_id base-url username password]
  (let [input-data (open-input-file input-file)
        houses-ids (select-identifiers input-data :house_id)
        mapping-content (with-open [in-file (io/reader mapping-file)]
                          (doall (csv/read-csv in-file)))
        mapping-data (map #(zipmap (mapv keyword (first mapping-content)) %)
                          (rest mapping-content))
        map-houses-ids (select-identifiers mapping-data :house_id)
        new-houses (look-up houses-ids map-houses-ids)]
    (if (empty? new-houses) ;; no new house
      (println "NO new house")
      ;; New houses to be created + overwrite mapping csv
      (let [new-mapping (->> (pmap (fn [house]
                                     (vec (cons house
                                                (api/create-new-entities
                                                 {:project_id project_id :property_code house}
                                                 (str "Device " house)
                                                 base-url username password))))
                                   new-houses)
                             (concat mapping-content)
                             vec)]
        (with-open [out-file (io/writer mapping-file)]
          (csv/write-csv out-file new-mapping))))))

(comment (pre-processing "resources/measurements-elec.csv"
                         "resources/mapping.csv"
                         "xxx-xxx-xxx"
                         "https://www.api-url/v1/"
                         "me@user.com" "p4ssw0rd"))


(defn main
  "To do all the things."
  [x]
  x)
