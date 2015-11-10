(ns kixi.hecuba.minorca
  (:gen-class :main true)
  (:require [clj-http.client       :as client]
            [clojure.data.csv      :as csv]
            [clojure.data.json     :as json]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [kixi.hecuba.api-helpers :as api]))

(defn open-input-file [input-file])

(defn get-houses-ids [input-data])

(defn look-up [houses-ids])

(defn pre-processing [input-file]
  (let [input-data (open-input-file input-file)
        houses-ids (get-houses-ids input-data)]
    (look-up houses-ids)))

(defn main
  "To do all the things."
  [x]
  x)
