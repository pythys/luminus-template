(ns <<project-ns>>.core
  (:require [<<project-ns>>.handler :refer [app init destroy]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.reload :as reload]<% if database-profiles %>
            [ragtime.main]<% endif %>
            [environ.core :refer [env]])
  (:gen-class))

(defonce server (atom nil))

(defn parse-port [port]
  (Integer/parseInt (or port (env :port) "3000")))

(defn start-server [port]
  (init)
  (reset! server
          (run-jetty
            (if (env :dev) (reload/wrap-reload #'app) app)
            {:port port
             :join? false})))

(defn stop-server []
  (when @server
    (destroy)
    (.stop @server)
    (reset! server nil)))

(defn migrate [args]
  (ragtime.main/-main
    "-r" "ragtime.sql.database"
    "-d" (env :database-url)
    "-m" "ragtime.sql.files/migrations"
    (clojure.string/join args)))

(defn -main [& args]
  <% if database-profiles %>(case (first args)
    "migrate" (migrate args)
    "rollback" (migrate args)
    (let [port (parse-port (first args))]
      (.addShutdownHook (Runtime/getRuntime) (Thread. stop-server))
      (start-server port))))
  <% else %>(let [port (parse-port (first args))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. stop-server))
    (start-server port)))
  <% endif %>