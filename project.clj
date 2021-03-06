(defproject todo-snap "0.1.0-SNAPSHOT"
  :description "An example todos REST api"
  :min-lein-version "2.0.0"

  :dependencies
  [[org.clojure/clojure "1.10.3"]

   [camel-snake-kebab "0.4.2"]
   [com.github.seancorfield/honeysql "2.1.818"]
   [duct/core "0.8.0"]
   [duct/module.ataraxy "0.3.0"]
   [duct/module.logging "0.5.0"]
   [duct/module.sql "0.6.1"]
   [duct/module.web "0.7.3"]
   [metosin/malli "0.6.2"]
   [org.babashka/sci "0.2.7"]
   [org.postgresql/postgresql "42.2.19"]]

  :plugins [[duct/lein-duct "0.12.3"]]
  :main ^:skip-aot todo-snap.main
  :uberjar-name  "todo-snap-standalone.jar"
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :middleware     [lein-duct.plugin/middleware]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.2"]
                                   [hawk "0.2.11"]
                                   [eftest "0.5.9"]
                                   [kerodon "0.9.1"]]}})
