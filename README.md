# clj-mdns

Clojure wrapper around [jmdns](https://github.com/jmdns/jmdns) for mDNS/DNS-SD service discovery.

## Dependency

```clojure
;; deps.edn
energy.grid-coordination/clj-mdns {:mvn/version "0.0.2-SNAPSHOT"}
```

## Usage

```clojure
(require '[mdns.core :as mdns])
```

### Create an instance

Bind to a specific network interface to discover services on the LAN.
The no-arg constructor binds to loopback and won't see LAN traffic.

```clojure
(def jmdns (mdns/create (java.net.InetAddress/getByName "192.168.1.100")))
```

### Listen for services (async)

`listen` registers a callback that receives event maps asynchronously.
With no service type, it listens for all discovered types:

```clojure
(mdns/listen jmdns
  (fn [event]
    (println (:mdns.event/type event)
             (:mdns.service/name (:mdns.event/service event)))))

;; :mdns.event/added "my-host"
;; :mdns.event/resolved "my-host"
```

Listen for a specific service type:

```clojure
(mdns/listen jmdns "_http._tcp.local."
  (fn [event]
    (println event)))
```

### List services (sync)

Query for services of a given type, blocking until timeout:

```clojure
(mdns/list-services jmdns "_ssh._tcp.local.")
;; => [{:mdns.service/name "my-host"
;;      :mdns.service/type "_ssh._tcp.local."
;;      :mdns.service/ipv4 "192.168.1.100"
;;      :mdns.service/port 22
;;      ...}]

(mdns/list-services jmdns "_http._tcp.local." 10000)  ; custom timeout ms
```

### Service map

Coerced services use namespaced keywords and carry the original
`ServiceInfo` bean as metadata:

```clojure
{:mdns.service/name           "my-host"
 :mdns.service/type           "_companion-link._tcp.local."
 :mdns.service/qualified-name "my-host._companion-link._tcp.local."
 :mdns.service/application    "companion-link"
 :mdns.service/protocol       :tcp
 :mdns.service/domain         "local"
 :mdns.service/port           63632
 :mdns.service/server         "my-host.local."
 :mdns.service/ipv4           "192.168.1.100"
 :mdns.service/ipv6           "fe80:0:0:0:..."
 :mdns.service/addresses      ["192.168.1.100" "[fe80:...]"]
 :mdns.service/urls           ["http://192.168.1.100:63632"]
 :mdns.service/priority       0
 :mdns.service/weight         0
 :mdns.service/txt            {"key1" "value1" "key2" "value2"}}

;; Access the raw ServiceInfo bean via metadata:
(:mdns/raw (meta service))
```

### Cleanup

```clojure
(mdns/close jmdns)
```

## Development

```bash
clojure -M:nrepl    # Start nREPL server
```

## License

MIT License. Copyright (c) 2024-2026 Don Jackson. See [LICENSE](LICENSE) for details.
