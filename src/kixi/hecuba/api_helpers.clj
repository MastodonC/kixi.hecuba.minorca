(ns kixi.hecuba.api-helpers
  (:require [clj-http.client       :as client]
            [clojure.data.csv      :as csv]
            [clojure.data.json     :as json]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]))

(defn create-new-property
  [project-id property-code base-url username password]
  (let [get-url (format "%s/entities/" base-url)]
    (try (client/get get-url
                     {:basic-auth [username password]
                      :accept :json})
         (catch Exception e (str "Caught exception " (.getMessage e))))))

(defn format-device-info [device-info])

(defn create-new-device
  [entity-id device-info base-url username password]
  (let [post-url (format "%s/entities/%s/devices/" base-url entity-id)
        json-payload (format-device-info device-info)]
    (try (client/post post-url
                      {:basic-auth [username password]
                       :body json-payload
                       :content-type "application/json"
                       :accept "application/json"})
         (catch Exception e (str "Caught exception " (.getMessage e))))))

(defn format-measurements [measurements])

(defn upload-measurements
  [entity-id device-id measurements base-url username password]
  (let [post-url (format "%s/entities/%s/devices/%s/measurements/"
                         base-url entity-id device-id)
        json-payload (format-measurements measurements)]
    (try (client/post post-url
                      {:basic-auth [username password]
                       :body json-payload
                       :content-type "application/json"
                       :accept "application/json"})
         (catch Exception e (str "Caught exception " (.getMessage e))))))
