(ns kixi.hecuba.process-helpers
  (:require [clojure.data.csv        :as csv]
            [clojure.java.io         :as io]
            [clojure.tools.logging   :as log]
            [clojure.set             :as set]
            [amazonica.aws.s3        :as s3]))


;; Helper for listing objects in the s3 bucket
(defn list-objects-paged
  "Returns all results in a lazy seq taking into account the pagination."
  [cred options]
  (let [{:keys [object-summaries truncated? next-marker] :as resp}
        (s3/list-objects cred options)]
    (if truncated?
      (lazy-cat object-summaries
                (list-objects-paged cred
                                    (assoc options
                                           :marker next-marker)))
      object-summaries)))


;; Helper functions for the pre-processing step
(defn format-key
  "Handle keys that countain a space"
  [key]
  ((comp 
    keyword
    clojure.string/lower-case
    #(clojure.string/replace % #" " "_"))
   key))

(defn file->seq-of-maps
  "Read a csv and output a seq of maps containing the file data."
  [input-file] 
  (let [data-seq (with-open [in-file (io/reader input-file)]
                   (vec (csv/read-csv in-file)))]
    (map #(zipmap (mapv format-key                          
                        (first data-seq)) %)
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

(defn write-to-file [output-file data]
  (log/info "Writing file" output-file)
  (with-open [out-file (io/writer output-file)]
    (csv/write-csv out-file data)))


;; Helper functions for the processing step
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

(defn merge-data-ids
  "Use the two functions above to associate the measurements
  with the entity_id and device_id."
  [data-input data-map]
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
  [input-data mapping-file]
  (log/info "prepare-measurements-for-upload")
  (let [formatted-input (format-input-data input-data)
        mapping-data (file->seq-of-maps mapping-file)
        mapping-ids (format-mapping-data mapping-data)]
    (merge-data-ids formatted-input mapping-ids)))
