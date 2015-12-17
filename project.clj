(def slf4j-version "1.7.12")
(defproject kixi.hecuba.minorca "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure   "1.7.0"]
                 [clj-http              "2.0.0"]
                 [clj-time              "0.11.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.csv  "0.1.3"]
                 [amazonica             "0.3.39"]
                 [org.clojure/tools.cli "0.3.1"]
                 ;; Logging
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.slf4j/jul-to-slf4j         ~slf4j-version]
                 [org.slf4j/jcl-over-slf4j       ~slf4j-version]
                 [org.slf4j/log4j-over-slf4j     ~slf4j-version]]
  :plugins [[lein-cljfmt     "0.1.11"]
            [jonase/eastwood "0.2.1"]
            [lein-kibit      "0.1.2"]]
  :main kixi.hecuba.minorca
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :repositories [["releases" {:url "s3p://mc-maven-repo/releases"
                              :username :env/mc_aws_username
                              :passphrase :env/mc_aws_passphrase
                              :sign-releases false}]
                 ["snapshots" {:url "s3p://mc-maven-repo/snapshots"
                               :username :env/mc_aws_username
                               :passphrase :env/mc_aws_passphrase
                               :sign-releases false}]])
