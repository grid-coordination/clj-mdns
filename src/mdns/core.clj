(ns mdns.core
  (:require [clojure.string :as string])
  (:import [javax.jmdns JmDNS ServiceInfo ServiceListener ServiceTypeListener]
           [java.net InetAddress]))

;;; --- Schemas ---

(def Service
  [:map
   [:mdns.service/name :string]
   [:mdns.service/type :string]
   [:mdns.service/qualified-name :string]
   [:mdns.service/application :string]
   [:mdns.service/protocol [:enum :tcp :udp]]
   [:mdns.service/domain :string]
   [:mdns.service/subtype [:maybe :string]]
   [:mdns.service/port :int]
   [:mdns.service/server :string]
   [:mdns.service/ipv4 [:maybe :string]]
   [:mdns.service/ipv6 [:maybe :string]]
   [:mdns.service/addresses [:vector :string]]
   [:mdns.service/urls [:vector :string]]
   [:mdns.service/priority :int]
   [:mdns.service/weight :int]
   [:mdns.service/txt [:map-of :string :string]]])

(def Event
  [:map
   [:mdns.event/type [:enum :mdns.event/added :mdns.event/removed :mdns.event/resolved]]
   [:mdns.event/service Service]])

;;; --- Coercion ---

(defn- ip->str [ip]
  (when ip
    (let [s (str ip)]
      (when-not (string/blank? s)
        (subs s 1)))))

(defn- txt-properties
  "Extract TXT record key-value pairs via ServiceInfo property API.
   Leaves keys as strings — TXT keys are case-insensitive per RFC 6763
   and service-specific, so keywordizing would create false precision."
  [^ServiceInfo info]
  (let [props (enumeration-seq (.getPropertyNames info))]
    (into {} (map (fn [k] [k (.getPropertyString info k)])) props)))

(defn ->service
  "Coerce a javax.jmdns.ServiceInfo to a Clojure service map.
   Attaches the original bean map as {:mdns/raw ...} metadata."
  [^ServiceInfo info]
  (let [raw (bean info)]
    (with-meta
      {:mdns.service/name           (.getName info)
       :mdns.service/type           (.getType info)
       :mdns.service/qualified-name (.getQualifiedName info)
       :mdns.service/application    (.getApplication info)
       :mdns.service/protocol       (keyword (.getProtocol info))
       :mdns.service/domain         (.getDomain info)
       :mdns.service/subtype        (let [s (.getSubtype info)]
                                      (when-not (string/blank? s) s))
       :mdns.service/port           (.getPort info)
       :mdns.service/server         (.getServer info)
       :mdns.service/ipv4           (ip->str (.getInet4Address info))
       :mdns.service/ipv6           (ip->str (.getInet6Address info))
       :mdns.service/addresses      (vec (.getHostAddresses info))
       :mdns.service/urls           (vec (.getURLs info))
       :mdns.service/priority       (.getPriority info)
       :mdns.service/weight         (.getWeight info)
       :mdns.service/txt            (txt-properties info)}
      {:mdns/raw raw})))

(defn- ->event
  "Coerce a javax.jmdns.ServiceEvent to an event map."
  [event-kw event]
  {:mdns.event/type    event-kw
   :mdns.event/service (->service (.getInfo event))})

;;; --- Instance lifecycle ---

(defn create
  "Create a JmDNS instance. Bind to a specific InetAddress to discover
   services on that interface. The no-arg version binds to loopback
   and will not see LAN traffic."
  ([]
   (JmDNS/create))
  ([^InetAddress addr]
   (JmDNS/create addr))
  ([^InetAddress addr ^String name]
   (JmDNS/create addr name)))

(defn close
  "Shut down a JmDNS instance."
  [^JmDNS instance]
  (.close instance))

;;; --- Discovery ---

(defn listen
  "Register a callback for mDNS service events.

   callback receives a single event map:
     {:mdns.event/type    :mdns.event/resolved
      :mdns.event/service {:mdns.service/name \"my-host\" ...}}

   With no service-type, listens for all discovered service types.
   With service-type, listens for that specific type only."
  ([^JmDNS instance callback]
   (let [type-listener (reify ServiceTypeListener
                         (serviceTypeAdded [_ event]
                           (listen instance (.getType event) callback)))]
     (.addServiceTypeListener instance type-listener)))
  ([^JmDNS instance ^String service-type callback]
   (let [listener (reify ServiceListener
                    (serviceAdded [_ e]
                      (callback (->event :mdns.event/added e)))
                    (serviceRemoved [_ e]
                      (callback (->event :mdns.event/removed e)))
                    (serviceResolved [_ e]
                      (callback (->event :mdns.event/resolved e))))]
     (.addServiceListener instance service-type listener))))

(defn list-services
  "Synchronously query for services of the given type. Blocks for
   timeout-ms (default 5000) while collecting responses.
   Returns a vector of coerced service maps."
  ([^JmDNS instance ^String service-type]
   (list-services instance service-type 5000))
  ([^JmDNS instance ^String service-type ^long timeout-ms]
   (mapv ->service (.list instance service-type timeout-ms))))
