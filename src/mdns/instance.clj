(ns mdns.instance
  (:require
   [clojure.string :as string]
   [medley.core :refer [assoc-some]])
  (:import java.net.Socket
           [java.io File BufferedReader BufferedWriter IOException]
           [javax.jmdns JmDNS]
           [java.net URI InetAddress NetworkInterface Inet4Address]))

(defn create
  ([]
   (JmDNS/create))
  ([addr-or-name]
   (JmDNS/create addr-or-name))
  ([addr name]
   (JmDNS/create addr name))
  ([addr name thread-sleep-duration-ms]
   (JmDNS/create addr name thread-sleep-duration-ms)))

(defn add-listener
  ([instance listener]
   (.addServiceTypeListener instance listener))
  ([instance type listener]
   (.addServiceListener instance type listener)))
