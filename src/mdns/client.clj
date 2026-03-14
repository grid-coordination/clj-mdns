(ns mdns.client
  (:require
   [clojure.string :as string]
   [medley.core :refer [assoc-some map-keys]])
  (:import java.net.Socket
           java.lang.StringBuilder
           [java.io File BufferedReader BufferedWriter IOException]
           (javax.jmdns JmDNS ServiceListener ServiceTypeListener)
           (java.net URI InetAddress NetworkInterface Inet4Address)
           (org.apache.commons.lang3 StringEscapeUtils)))

;; https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/StringEscapeUtils.html

(defn ^:private split-on-java-literals
  [s]
  (map #(StringEscapeUtils/unescapeJava %)
       (clojure.string/split s #"\\031")))


;; (def ntstr "\\026version=z/jd/matter/04\\020sn=nt-2026-c192x\\033wlan0_mac=3c:e1:a1:df:dd:e6\\032eth0_mac=00:19:b8:08:52:d9")

(defn ^:private split-nice-text-string
  [s]
  (mapcat split-on-java-literals
          (clojure.string/split s #"\\[0-9]{3}")))

(defn ^:private remove-blank-strings
  "Returns vector of strings with blank strings removed"
  [c]
  (filterv #(not (string/blank? %)) c))


(defn ^:private k=v->kv
  "Returns vector of keyword value from string key=value"
  [s]
  (string/split s #"\="))


(defn ^:private ip->str
  [ip]
  (let [ip-str (str ip)]
    (when-not (string/blank? ip-str)
      (subs ip-str 1))))

(defn ^:private nice-text-string->hash-map
  [s]
  (when (string? s)
    (try
      (->> s
           split-nice-text-string
           remove-blank-strings
           (map k=v->kv)
           (into {})
           clojure.walk/keywordize-keys)
      (catch Exception e
        nil))))

#_(defn ^:private parse-event
  [event]
  (let [{:keys [name inet6Address inet4Address niceTextString] :as event-bean} (bean (.getInfo event))
        service-type (.getType event)]
    (merge
     (assoc-some event-bean
                 :name name
                 :service-type service-type
                 :ipv6 (ip->str inet6Address)
                 :ipv4 (ip->str inet4Address))
     (nice-text-string->hash-map niceTextString))))

;; although not reference-able in Clojuredocs,
;; org.clojure/java.data provides a useful, alternative 'from-java' function
;; that works similarly to bean, but more customizable.
;; See https://github.com/clojure/java.data for more info.

(defn ^:private parse-event
  [event]
  (let [{:keys [name inet6Address inet4Address niceTextString] :as event-bean} (bean (.getInfo event))
        service-type (.getType event)]
    (assoc-some (hash-map)
                :name name
                :service-type service-type
                :ipv6 (ip->str inet6Address)
                :ipv4 (ip->str inet4Address)
                :txt  (nice-text-string->hash-map niceTextString))))


(defn add-service-listener
  "callback - fb of 2 args, first is keyword (:added, :removed, :resolved), second is parsed event"
  ([instance callback]
   (let [type-listener (reify ServiceTypeListener

                         (serviceTypeAdded [_ event]
                           (let [service-type (.getType event)]
                             (add-service-listener instance service-type callback))))]

     (.addServiceTypeListener instance type-listener)))

  ([instance service-type callback]
   (let [listener (reify ServiceListener

                    (serviceAdded [this added-event]
                      (callback :added (parse-event added-event)))

                    (serviceRemoved [this removed-event]
                      (callback :removed (parse-event removed-event)))

                    (serviceResolved [this resolved-event]
                      (callback :resolved (parse-event resolved-event))))]

     (.addServiceListener instance service-type listener))))


(defn create-instance
  [ip]
  (JmDNS/create ip))

;; https://www.javadoc.io/doc/org.jmdns/jmdns/latest/javax/jmdns/JmDNS.html
