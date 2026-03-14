# clj-mdns API Sketch

## Entity: Service

A discovered mDNS service, coerced from `javax.jmdns.ServiceInfo`.

### Namespaced keywords

```clojure
;; Core identity
:mdns.service/name           ; "dcj-mbp"
:mdns.service/type           ; "_companion-link._tcp.local."
:mdns.service/qualified-name ; "dcj-mbp._companion-link._tcp.local."

;; Parsed from type
:mdns.service/application    ; "companion-link"
:mdns.service/protocol       ; :tcp | :udp
:mdns.service/domain         ; "local"
:mdns.service/subtype        ; "sub" or nil

;; Network
:mdns.service/port           ; 63632
:mdns.service/server         ; "dcj-mbp.local."
:mdns.service/ipv4           ; "172.16.13.111" (first, or nil)
:mdns.service/ipv6           ; "fe80:0:0:0:..." (first, or nil)
:mdns.service/addresses      ; ["172.16.13.111" "[fe80:...]"] (all)
:mdns.service/urls           ; ["http://172.16.13.111:63632" ...]

;; DNS-SD
:mdns.service/priority       ; 0
:mdns.service/weight         ; 0

;; TXT record (key-value properties from DNS-SD TXT record)
:mdns.service/txt            ; {"rpMac" "0", "rpVr" "715.2", ...}
```

### Malli schema

```clojure
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
```

### Raw metadata

```clojure
;; Every coerced service carries the original bean map as metadata
(meta service) ; => {:mdns/raw {:name "dcj-mbp" :port 63632 :type "..." ...}}
```

## Entity: Event

A service discovery event (wraps `ServiceEvent`).

```clojure
:mdns.event/type    ; :mdns.event/added | :mdns.event/removed | :mdns.event/resolved
:mdns.event/service ; the coerced Service map (see above)
```

```clojure
(def Event
  [:map
   [:mdns.event/type [:enum :mdns.event/added :mdns.event/removed :mdns.event/resolved]]
   [:mdns.event/service Service]])
```

## Namespace: `mdns.core`

Single namespace combining instance lifecycle, discovery, and coercion.

```clojure
(ns mdns.core
  (:require [clojure.string :as string]
            [malli.core :as m])
  (:import [javax.jmdns JmDNS ServiceInfo ServiceListener ServiceTypeListener]
           [java.net InetAddress]))

;;; --- Coercion (ServiceInfo → Service) ---

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
  ([addr]
   (JmDNS/create addr))
  ([addr name]
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

  ([^JmDNS instance service-type callback]
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
  ([^JmDNS instance service-type]
   (list-services instance service-type 5000))
  ([^JmDNS instance service-type timeout-ms]
   (mapv ->service (.list instance service-type timeout-ms))))
```

## Key changes from current API

1. **Callback signature changes**: `(fn [event-map])` instead of `(fn [event-type parsed-map])`
   - Event type moves inside the map — single arg is easier to put on a channel, atom, etc.

2. **TXT parsing fixed**: Use `ServiceInfo` property API instead of parsing `niceTextString`
   - Current approach breaks on values containing escape sequences or separators

3. **More fields surfaced**: port, priority, weight, server, urls, subtype, addresses
   - Current API drops most of these

4. **Malli schema**: Consumers can validate, generate test data, document

5. **Raw metadata**: `(meta service)` gives the full bean for anything not surfaced

## Dependencies to add

```clojure
;; deps.edn
metosin/malli {:mvn/version "0.18.0"}
```

## Decisions

- **TXT keys as strings** — TXT record keys are case-insensitive per RFC 6763 and
  service-specific. Keywords would create false precision (`:rpMac` vs `:rpmac`) and
  intern untrusted data. Consumers can `(update-keys txt keyword)` if desired.
- **Single `mdns.core` namespace** — library is small; two namespaces was unnecessary
  indirection. If publishing/registration is added later, that warrants `mdns.publish`.
- **`list-services` included** — synchronous query wrapping `JmDNS.list()`, returns
  a vector of coerced service maps.
