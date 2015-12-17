(ns kixi.hecuba.minorca
  (:gen-class :main true)
  (:require [clojure.data.csv        :as csv]
            [clojure.edn             :as edn]
            [clojure.java.io         :as io]
            [clojure.tools.logging   :as log]
            [amazonica.aws.s3        :as s3]
            [clj-time.core           :as t]
            [clj-time.format         :as f]
            [clojure.set             :as set]
            [kixi.hecuba.api-helpers :as api]
            [kixi.hecuba.process-helpers :as pro]
            [clojure.tools.cli :refer [parse-opts]]))


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
  [cred bucket]
  (println "Looking for the files in s3 bucket...")
  (try (let [s3-objects
             (pro/list-objects-paged cred {:bucket-name bucket})]
         (map :key s3-objects))
       (catch Throwable t
         (println "Got exception: " (.getMessage t))
         (throw t))))


;; Frist step: check if there are new houses to be created
(defn pre-processing
  "Check if houses are new. If so then create new instances
  in hecuba and add them to the mapping file."
  [input-data mapping-file project_id base-url username password]
  (log/info "Running the pre-processing step...")
  (let [houses-ids (pro/select-identifiers input-data :house_id)
        mapping-content (with-open [in-file (io/reader mapping-file)]
                          (vec (csv/read-csv in-file)))
        mapping-data (map #(zipmap (mapv keyword (first mapping-content)) %)
                          (rest mapping-content))
        map-houses-ids (pro/select-identifiers mapping-data :house_id)
        new-houses (set/difference houses-ids map-houses-ids)]
    (log/debug ">> map-houses-ids + new-houses: " map-houses-ids new-houses)
    (if (empty? new-houses) (log/info "NO new house")
        ;; Create new houses + write mapping csv
        (->> (pmap
              (fn [house] (vec (cons house
                                     (api/create-new-entities
                                      {:project_id project_id
                                       :property_code (str "House " house)}
                                      (str "Device " house)
                                      base-url username password))))
              new-houses)
             (concat mapping-content)
             vec
             (pro/write-to-file mapping-file)))))

(comment (pre-processing "resources/measurements-elec.csv"
                         "resources/mapping.csv"
                         "xxx-xxx-xxx"
                         "https://www.api-url/v1/"
                         "me@user.com" "p4ssw0rd"))


;; Second step: processing and uploading the data
(defn upload-measurement-data
  "Use the previous function to pre-format the data
  before using helper functions to format more and
  send the POST request."
  [input-data mapping-file base-url username password]
  (log/info "Running the processing+upload step...")
  (let [prepared (pro/prepare-measurements-for-upload
                  input-data mapping-file)]
    (doseq [[map-ids vec-measure] prepared]
      (api/decide-upload (:entity_id map-ids) (:device_id map-ids)
                         vec-measure
                         base-url username password) prepared)))

;; Deal with command-line options
(def cli-options
  [["-i" "--project-id ID" "Project_id for a getembed project"]
   ["-u" "--embed-url URL" "Url for endpoint for getembed"]
   ["-n" "--username USERNAME" "Username associated with a getembed account"]
   ["-p" "--password PASSWORD" "Password associated with a getembed account"]])

;; Datetime formatter
(def fmt (f/formatters :date-hour-minute-second))

;; Bring all the steps together:
;; Step 0: get the data from the S3 bucket
;; Step 1: pre-process the data (+ update houses mapping file)
;; Step 2: process + upload the data
;; Note: create a file to store the data files processed
(defn -main
  "Perform data processing and upload.
  Update the mapping and files list CSVs."
  [& args]
  (let [{:keys [project-id embed-url username password] :as opts}
        (:options (parse-opts args cli-options))
        {:keys [s3 mapping-file processed-file]} config-info
        {:keys [cred bucket]} s3
        processed-files (keep :file_name (pro/file->seq-of-maps processed-file)) 
        s3-files (list-files-in-bucket cred bucket)
        files-to-process (set/difference (set s3-files) (set processed-files))]
    (log/info "New S3 files:" files-to-process)
    (if (empty? files-to-process) (log/info "No new files on S3")
        (doseq [f files-to-process]
          (println "> FILE " f)
          (let [{:keys [key bucket-name object-metadata object-content]}
                (s3/get-object cred bucket f)
                input-data (with-open [r (->> (s3/get-object cred bucket f)
                                              :object-content
                                              io/reader)]
                             (pro/file->seq-of-maps r))
                processed-content (with-open [in-file (io/reader processed-file)]
                                    (vec (csv/read-csv in-file)))
                data-to-save (conj (vec processed-content)
                                   [(f/unparse fmt (t/now)) key bucket-name
                                    object-metadata object-content])]
            (println ">> Pre-processing... " f)
            (pro/write-to-file processed-file data-to-save)
            (pre-processing input-data
                            mapping-file project-id
                            embed-url username password)
            (println ">> Processing... " f)
            (upload-measurement-data input-data mapping-file
                                     embed-url username password))))))

(comment
  (-main "-ixxx-xxx-xxx"
         "-uhttps://www.api-url/v1/"
         "-nme@user.com"
         "-pp4ssw0rd"))

