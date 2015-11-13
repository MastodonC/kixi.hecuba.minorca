(ns kixi.hecuba.api-helpers-test
  (:require [clojure.test :refer :all]
            [kixi.hecuba.api-helpers :refer :all]))

(def vec-measurements
  [{:house_id "001" :device_timestamp "1433096100" :energy "63746"
    :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}
   {:house_id "001" :device_timestamp "1433097000" :energy "63800"
    :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}])

(deftest format-device-info-test
  (testing "The json payload is formatted properly"))

(deftest format-epoch-timestamp-test
  (testing "Epoch gets formatted into a string"
    (is (= "2015-05-31T18:30:00.000Z"
           (format-epoch-timestamp "1433097000")))))

(deftest format-measurements-test
  (testing "The json payload is formatted properly"
    (is (= "{\"measurements\":[{\"timestamp\":\"2015-05-31T18:15:00.000Z\",\"value\":\"63746\",\"type\":\"electricityConsumption\"},{\"timestamp\":\"2015-05-31T18:30:00.000Z\",\"value\":\"63800\",\"type\":\"electricityConsumption\"}]}"
           (format-measurements vec-measurements)))))
