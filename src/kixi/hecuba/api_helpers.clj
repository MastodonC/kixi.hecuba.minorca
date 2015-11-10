(ns kixi.hecuba.api-helpers
  (:require [clj-http.client       :as client]
            [clojure.data.csv      :as csv]
            [clojure.data.json     :as json]
            [clojure.java.io       :as io]
            [clojure.tools.logging :as log]
            [clojure.string        :as str]))

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

(comment (create-new-device  "yyy-yyy-yyy"
                             {:readings [{:unit "deg C" :period "INSTANT" :type "temperature"}
                                         {:unit "kWh" :period "CUMULATIVE"
                                          :type "electricityConsumption"}
                                         {:unit "m^3" :period "CUMULATIVE"
                                          :type "gasConsumption"}]
                              :description "New device"
                              :entity_id "yyy-yyy-yyy"}
                             "https://www.api-url/v1/"
                             "me@user.com" "p4ssw0rd"))


(defn format-measurements [measurements] ;; May need more formatting later
  (json/write-str measurements))

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
                               {:measurements [{:value "19" :timestamp "2015-11-10T10:30:00Z"
                                                :type "temperature"}]}
                               "https://www.api-url/v1/"
                               "me@user.com" "p4ssw0rd"))
