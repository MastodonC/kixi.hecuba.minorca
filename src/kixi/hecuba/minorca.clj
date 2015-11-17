(ns kixi.hecuba.minorca
  (:gen-class :main true)
  (:require [clojure.data.csv        :as csv]
            [clojure.edn             :as edn]
            [clojure.java.io         :as io]
            [clojure.tools.logging   :as log]
            [kixi.hecuba.api-helpers :as api]
            [clojure.set             :as set]
            [aws.sdk.s3              :as s3]))


;; Get all information from the configuration file
(defn get-config
  "Gets info from a config file."
  [file-path]
  (edn/read-string (slurp file-path)))

(def config-info
  (let [f (io/file (System/getProperty "user.home")
                   ".k.h.minorca-config.edn")]
    (get-config f)))


;; Get all the files in the S3 bucket
(defn list-files-in-bucket
  "List the files contained in the s3 bucket.
  Ordered by last modification date."
  [name cred]
  (log/info "Looking for the files in s3 bucket...")
  (if (s3/bucket-exists? cred name)
    (let [s3-objects (:objects (s3/list-objects cred name))]
      (->> (sort-by #(get-in % [:metadata :last-modified])
                    s3-objects)
           (map :key)))
    (log/debug "This bucket doesn't exist!")))


;; Helper functions for the pre-processing step
(defn open-input-file
  "Read a csv and output a seq of maps containing the file data."
  [input-file]
  (let [data-seq (with-open [in-file (io/reader input-file)]
                   (vec (csv/read-csv in-file)))]
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

(defn write-to-file [data mapping-file]
  (with-open [out-file (io/writer mapping-file)]
    (csv/write-csv out-file data)))


;; Frist step: check if there are new houses to be created
(defn pre-processing
  "Check if houses are new. If so then create new instances
  in hecuba and add them to the mapping file."
  [input-data mapping-file project_id base-url username password]
  (let [houses-ids (select-identifiers input-data :house_id)
        mapping-content (with-open [in-file (io/reader mapping-file)]
                          (vec (csv/read-csv in-file)))
        mapping-data (map #(zipmap (mapv keyword (first mapping-content)) %)
                          (rest mapping-content))
        map-houses-ids (select-identifiers mapping-data :house_id)
        new-houses (look-up houses-ids map-houses-ids)]
    (if (empty? new-houses) ;; No new house
      (println "NO new house")
      (write-to-file ;; Create new houses + write mapping csv
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
  (let [formatted-input (format-input-data input-data)
        mapping-data (open-input-file mapping-file)
        mapping-ids (format-mapping-data mapping-data)]
    (merge-data-ids formatted-input mapping-ids)))


;; Second step: processing and uploading the data
(defn upload-measurement-data
  "Use the previous function to pre-format the data
  before using helper functions to format more and
  send the POST request."
  [input-data mapping-file base-url username password]
  (->> (prepare-measurements-for-upload
        input-data mapping-file)
       (map (fn [[map-ids vec-measure]]
              (api/decide-upload (:entity_id map-ids) (:device_id map-ids)
                                 vec-measure
                                 base-url username password)))))



;; Bring all the steps together:
;; Step 0: get the data from the S3 bucket
;; Step 1: pre-process the data (+ update houses mapping file)
;; Step 2: process + upload the data
;; Note: create a file to store the data files processed
(defn main
  "To do all the things."
  [project_id base-url username password]
  (let [{:keys [bucket-name bucket-cred mapping-file processed-file]} config-info
        s3-files (list-files-in-bucket bucket-name bucket-cred)]
    (map (fn [f]
           (let [file-info (s3/get-object bucket-cred bucket-name f)
                 input-data (with-open [r (->> (s3/get-object bucket-cred bucket-name f)
                                               :content
                                               io/reader)]
                              (open-input-file r))
                 processed-content (with-open [in-file (io/reader processed-file)]
                                     (doall (csv/read-csv in-file)))
                 data-to-save (conj (vec processed-content)
                                    [(:key file-info) (:metadata file-info)
                                     (:bucket file-info) (:content file-info)])]
             (write-to-file data-to-save processed-file)
             (pre-processing input-data
                             mapping-file project_id
                             base-url username password)
             (upload-measurement-data input-data mapping-file
                                      base-url username password)))
         s3-files)))

