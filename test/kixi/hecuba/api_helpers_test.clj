(ns kixi.hecuba.api-helpers-test
  (:require [clojure.test :refer :all]
            [kixi.hecuba.api-helpers :refer :all]))

(def vec-measurements
  [{:house_id "001" :device_timestamp "1433096100" :energy "63746"
    :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}
   {:house_id "001" :device_timestamp "1433097000" :energy "63800"
    :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}])

(def vec-measurements2
  [{:house_id "001" :device_timestamp "1433096100" :energy "63746"
    :temperature "17" :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}
   {:house_id "001" :device_timestamp "1433097000" :energy "63800"
    :temperature "18" :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}])

(def elec-measurements
  [{:device_timestamp "1433096100", :energy "63746"}
   {:device_timestamp "1433097000", :energy "63800"}
   {:device_timestamp "1433097900", :energy "63855"}
   {:device_timestamp "1433098800", :energy "63909"}])

(def gas-measurements
  [{:device_timestamp "1436875200", :energy "8.49", :temperature "17"}
   {:device_timestamp "1436876100", :energy "8.49", :temperature "17"}
   {:device_timestamp "1436877000", :energy "8.49", :temperature "17"}
   {:device_timestamp "1436877900", :energy "8.49", :temperature "17"}
   {:device_timestamp "1436878800", :energy "8.49", :temperature "17"}
   {:device_timestamp "1436879700", :energy "8.49", :temperature "17"}])

(deftest format-device-info-test
  (testing "The json payload is formatted properly"))

(deftest format-epoch-timestamp-test
  (testing "Epoch gets formatted into a string"
    (is (= "2015-05-31T18:30:00.000Z"
           (format-epoch-timestamp "1433097000")))))

(deftest contains-temperature?-test
  (testing "We can differentiate between elec and gas measurements"
    (is (true? (contains-temperature? gas-measurements)))
    (is (false? (contains-temperature? elec-measurements)))))

(deftest format-map-test
  (testing "A map of measurements is formatted into Embed payload"
    (is (= {:timestamp "1436875200", :value "17", :type "temperature"}
           (format-map
            {:device_timestamp "1436875200", :energy "8.49", :temperature "17"}
            :temperature
            "temperature")))
    (is (= {:timestamp "1436875200", :value "8.49", :type "gasConsumption"}
           (format-map
            {:device_timestamp "1436875200", :energy "8.49", :temperature "17"}
            :energy
            "gasConsumption")))))

(deftest format-measurements-test
  (testing "The json payload is formatted properly"
    (is (= "{\"measurements\":[{\"timestamp\":\"2015-05-31T18:15:00.000Z\",\"value\":\"63746\",\"type\":\"electricityConsumption\"},{\"timestamp\":\"2015-05-31T18:30:00.000Z\",\"value\":\"63800\",\"type\":\"electricityConsumption\"}]}"
           (format-measurements vec-measurements :energy "electricityConsumption")))))
