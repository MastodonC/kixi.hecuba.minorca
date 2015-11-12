(ns kixi.hecuba.minorca-test
  (:require [clojure.test :refer :all]
            [kixi.hecuba.minorca :refer :all]))

;; Data for the tests:
(def mapping-data '({:house_id "001" :entity_id "aaa-aaa-aaa" :device_id "bbb-bbb-bbb"}
                    {:house_id "002" :entity_id "ccc-ccc-ccc" :device_id "ddd-ddd-ddd"}
                    {:house_id "003" :entity_id "eee-eee-eee" :device_id "fff-fff-fff"}))

(def formatted-mapping-data
  {"001" {:entity_id "aaa-aaa-aaa", :device_id "bbb-bbb-bbb-"}
   "002" {:entity_id "ccc-ccc-ccc", :device_id "ddd-ddd-ddd"}
   "003" {:entity_id "eee-eee-eee", :device_id "fff-fff-fff"}})

(def input-data '({:house_id "001" :received_timestamp "1435864740"
                   :device_timestamp "1433096100" :energy "63746"}
                  {:house_id "001" :received_timestamp "1435864740"
                   :device_timestamp "1433097000" :energy "63800"}
                  {:house_id "002" :received_timestamp "1436913007"
                   :device_timestamp "1436913000" :energy "2498"}
                  {:house_id "003" :received_timestamp "1436908574"
                   :device_timestamp "1436908500" :energy "1150"}
                  {:house_id "003" :received_timestamp "1436909407"
                   :device_timestamp "1436909400" :energy "1174"}
                  {:house_id "003" :received_timestamp "1436910364"
                   :device_timestamp "1436910300" :energy "1218"}
                  {:house_id "003" :received_timestamp "1436911232"
                   :device_timestamp "1436911200" :energy "1283"}))

(def formatted-input-data
  {"001" [{:house_id "001", :device_timestamp "1433096100", :energy "63746"}
          {:house_id "001", :device_timestamp "1433097000", :energy "63800"}]
   "002" [{:house_id "002", :device_timestamp "1436913000", :energy "2498"}]
   "003" [{:house_id "003", :device_timestamp "1436908500", :energy "1150"}
          {:house_id "003", :device_timestamp "1436909400", :energy "1174"}
          {:house_id "003", :device_timestamp "1436910300", :energy "1218"}
          {:house_id "003", :device_timestamp "1436911200", :energy "1283"}]})

;; Tests:
(deftest select-identifiers-test
  (testing "Output is as expected"
    (is (= #{"002" "003" "001"}
           (select-identifiers input-data :house_id)))))

(deftest look-up-test
  (testing "Output is as expected"
    (is (= #{} (look-up #{"002" "003" "001"}
                        #{"002" "003" "001"})))
    (is (= #{"004"} (look-up #{"002" "003" "001" "004"}
                             #{"002" "003" "001"})))))

(deftest format-mapping-data-test
  (testing "Output is as expected"
    (is (= {"001" {:entity_id "aaa-aaa-aaa", :device_id "bbb-bbb-bbb"}
            "002" {:entity_id "ccc-ccc-ccc", :device_id "ddd-ddd-ddd"}
            "003" {:entity_id "eee-eee-eee", :device_id "fff-fff-fff"}}
           (format-mapping-data mapping-data)))))

(deftest format-input-data-test
  (testing "Output is as expected"
    (is (= {"001" [{:house_id "001", :device_timestamp "1433096100", :energy "63746"}
                   {:house_id "001", :device_timestamp "1433097000", :energy "63800"}]
            "002" [{:house_id "002", :device_timestamp "1436913000", :energy "2498"}]
            "003" [{:house_id "003", :device_timestamp "1436908500", :energy "1150"}
                   {:house_id "003", :device_timestamp "1436909400", :energy "1174"}
                   {:house_id "003", :device_timestamp "1436910300", :energy "1218"}
                   {:house_id "003", :device_timestamp "1436911200", :energy "1283"}]}
           (format-input-data input-data)))))

(deftest merge-data-ids-test
  (testing "Output is as expected"
    (is (= {"001" [{:house_id "001" :device_timestamp "1433096100" :energy "63746"
                    :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}
                   {:house_id "001" :device_timestamp "1433097000" :energy "63800"
                    :entity_id "aaa-aaa-aaa" :device_id "aaa-aaa-aaa"}]
            "002" [{:house_id "002" :device_timestamp "1436913000" :energy "2498"
                    :entity_id "ccc-ccc-ccc" :device_id "ccc-ccc-ccc"}]
            "003" [{:house_id "003" :device_timestamp "1436908500" :energy "1150"
                    :entity_id "eee-eee-eee" :device_id "eee-eee-eee"}
                   {:house_id "003" :device_timestamp "1436909400" :energy "1174"
                    :entity_id "eee-eee-eee" :device_id "eee-eee-eee"}
                   {:house_id "003" :device_timestamp "1436910300" :energy "1218"
                    :entity_id "eee-eee-eee" :device_id "eee-eee-eee"}
                   {:house_id "003" :device_timestamp "1436911200" :energy "1283"
                    :entity_id "eee-eee-eee" :device_id "eee-eee-eee"}]}
           (merge-data-ids formatted-input-data
                           formatted-mapping-data)))))
