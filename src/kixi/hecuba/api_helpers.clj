(ns kixi.hecuba.api-helpers
  (:require [clj-http.client       :as client]
            [clojure.data.csv      :as csv]
            [clojure.data.json     :as json]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [clojure.string        :as str]
            [clj-time.core         :as t]
            [clj-time.coerce       :as c]))

(defn create-new-property
  "Use property info (project_id and property_code)
  to create a new property and return its entity_id"
  [property-info base-url username password]
  (let [post-url (format "%sentities/" base-url)
        json-payload (json/write-str property-info)]
    (-> (try (client/post post-url
                          {:basic-auth [username password]
                           :body json-payload
                           :content-type "application/json"
                           :accept "application/json"})
             (catch Exception e (str "Caught exception " (.getMessage e))))
        :body
        (json/read-str :key-fn keyword)
        (get-in [:headers :Location])
        (str/split #"/")
        last)))

(comment (create-new-property {:project_id "xxx-xxx-xxx" :property_code "house001"}
                              "https://www.api-url/v1/"
                              "me@user.com" "p4ssw0rd"))


(defn format-device-info [device-info] ;; May need more formatting later
  (json/write-str device-info))

(defn create-new-device
  "Use device info (name, entity_id, unit, period and type)
  to create a new device and return its device_id"
  [entity-id device-info base-url username password]
  (let [post-url (format "%sentities/%s/devices/" base-url entity-id)
        json-payload (format-device-info device-info)]
    (-> (try (client/post post-url
                          {:basic-auth [username password]
                           :body json-payload
                           :content-type "application/json"
                           :accept "application/json"})
             (catch Exception e (str "Caught exception " (.getMessage e))))
        :body
        (json/read-str :key-fn keyword)
        :location
        (str/split #"/")
        last)))

;; To be moved to a config file later
(def readings [{:unit "deg C" :period "INSTANT" :type "temperature"}
               {:unit "kWh" :period "CUMULATIVE"
                :type "electricityConsumption"}
               {:unit "m^3" :period "CUMULATIVE"
                :type "gasConsumption"}])

(comment (create-new-device  "yyy-yyy-yyy"
                             {:readings readings
                              :description "New device"
                              :entity_id "yyy-yyy-yyy"}
                             "https://www.api-url/v1/"
                             "me@user.com" "p4ssw0rd"))

(defn create-new-entities
  "Create a new property + a new device. Return the entity_id
  and device_id in a vector."
  [property-info device_name base-url username password]
  (let [entity_id (create-new-property property-info base-url username password)
        device_id (create-new-device entity_id {:readings readings
                                                :description device_name
                                                :entity_id entity_id}
                                     base-url username password)]
    [entity_id device_id]))

(defn format-epoch-timestamp [epoch-timestamp]
  (c/to-string (* (read-string epoch-timestamp) 1000)))

(defn format-measurements
  "Input is a vector of maps with measurements and property 
  info to format into embed measurements."
  [measurements]
  (->> measurements
       (map (fn [m]
              (-> (select-keys m [:device_timestamp :energy])
                  (clojure.set/rename-keys 
                   {:device_timestamp :timestamp
                    :energy :value})
                  (assoc :type "electricityConsumption"))))
       (mapv (fn [m] (update m :timestamp format-epoch-timestamp)))
       (assoc {} :measurements)
       json/write-str))

(defn upload-measurements
  [entity-id device-id measurements base-url username password]
  (let [post-url (format "%sentities/%s/devices/%s/measurements/"
                         base-url entity-id device-id)
        json-payload (format-measurements measurements)]
    (try (client/post post-url
                      {:basic-auth [username password]
                       :body json-payload
                       :content-type "application/json"
                       :accept "application/json"})
         (catch Exception e (str "Caught exception " (.getMessage e))))))

(comment (upload-measurements  "yyy-yyy-yyy" "zzz-zzz-zzz"
                               {:measurements [{:value "19"
                                                :timestamp "2015-11-10T10:30:00Z"
                                                :type "temperature"}]}
                               "https://www.api-url/v1/"
                               "me@user.com" "p4ssw0rd"))
