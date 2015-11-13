(ns kixi.hecuba.minorca
  (:gen-class :main true)
  (:require [clj-http.client         :as client]
            [clojure.data.csv        :as csv]
            [clojure.data.json       :as json]
            [clojure.java.io         :as io]
            [clojure.tools.logging   :as log]
            [kixi.hecuba.api-helpers :as api]
            [clojure.set             :as set]))

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

(defn overwrite-mapping [data mapping-file]
  (with-open [out-file (io/writer mapping-file)]
    (csv/write-csv out-file data)))

(defn pre-processing
  "Check if houses are new. If so then create new instances
  in hecuba and add them to the mapping file."
  [input-file mapping-file project_id base-url username password]
  (let [input-data (open-input-file input-file)
        houses-ids (select-identifiers input-data :house_id)
        mapping-content (with-open [in-file (io/reader mapping-file)]
                          (doall (csv/read-csv in-file)))
        mapping-data (map #(zipmap (mapv keyword (first mapping-content)) %)
                          (rest mapping-content))
        map-houses-ids (select-identifiers mapping-data :house_id)
        new-houses (look-up houses-ids map-houses-ids)]
    (if (empty? new-houses) ;; No new house
      (println "NO new house")
      (overwrite-mapping ;; Create new houses + write mapping csv
       (->> (pmap
             (fn [house] (vec (cons house
                                    (api/create-new-entities
                                     {:project_id project_id :property_code house}
                                     (str "Device " house)
                                     base-url username password))))
             new-houses)
            (concat mapping-content)
            vec)
       mapping-file))))

(comment (pre-processing "resources/measurements-elec.csv"
                         "resources/mapping.csv"
                         "xxx-xxx-xxx"
                         "https://www.api-url/v1/"
                         "me@user.com" "p4ssw0rd"))

(defn format-mapping-data
  [mapping-data]
  (->> (map (fn [m] {(:house_id m)
                     {:entity_id (:entity_id m)
                      :device_id (:device_id m)}})
            mapping-data)
       (reduce merge)))

(defn format-input-data
  [input-data]
  (->> input-data
       (map #(select-keys % [:house_id :device_timestamp
                             :energy :temperature]))
       (group-by :house_id)))

(defn merge-data-ids [data-input data-map]
  (->> (map (fn [[k v]] {{:entity_id (:entity_id (get data-map k))
                          :device_id (:device_id (get data-map k))} 
                         (mapv (fn [m]
                                 (select-keys m [:device_timestamp
                                                 :energy :temperature]))
                               v)})
            data-input)
       (reduce merge)))

(defn prepare-measurements-for-upload
  "Using the mapping file to add the measurements to the
  right embed properties."
  [input-file mapping-file]
  (let [input-data (open-input-file input-file)
        formatted-input (format-input-data input-data)
        mapping-data (open-input-file mapping-file)
        mapping-ids (format-mapping-data mapping-data)]
    (merge-data-ids formatted-input mapping-ids)))

(defn upload-measurement-data
  [input-file mapping-file base-url username password]
  (->> (prepare-measurements-for-upload
        input-file mapping-file)
       (map (fn [[map-ids vec-measure]]
              (api/upload-measurements (:entity_id map-ids) (:device_id map-ids)
                                       vec-measure
                                       base-url username password)))))


(defn main
  "To do all the things."
  [x]
  x)
