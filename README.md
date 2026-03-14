# clj-mdns

Clojure wrapper around [jmdns](https://github.com/jmdns/jmdns) for mDNS/DNS-SD service discovery.

## Dependency

```clojure
;; deps.edn
energy.grid-coordination/clj-mdns {:mvn/version "0.0.2-SNAPSHOT"}
```

## Usage

### Create a JmDNS instance

Bind to a specific network interface to discover services on the LAN.
The no-arg constructor binds to loopback and won't see LAN traffic.

```clojure
(require '[mdns.instance :as instance])
(require '[mdns.client :as client])

;; Bind to a specific interface
(def jmdns (instance/create (java.net.InetAddress/getByName "192.168.1.100")))
```

### Discover services

`add-service-listener` takes a callback that receives an event type
(`:added`, `:removed`, `:resolved`) and a parsed event map:

```clojure
(def events (atom []))

(client/add-service-listener jmdns
  (fn [event-type parsed]
    (println event-type (:name parsed) (:service-type parsed))
    (swap! events conj {:event event-type :data parsed})))

;; Events arrive asynchronously as services are discovered:
;; :added "my-host" "_ssh._tcp.local."
;; :resolved "my-host" "_ssh._tcp.local."
```

### Listen for a specific service type

```clojure
(client/add-service-listener jmdns "_http._tcp.local."
  (fn [event-type parsed]
    (println event-type parsed)))
```

### Parsed event map

Resolved events include address and TXT record data:

```clojure
{:name "my-host"
 :service-type "_companion-link._tcp.local."
 :ipv4 "192.168.1.100"
 :ipv6 "fe80:0:0:0:..."
 :txt {:key1 "value1" :key2 "value2"}}
```

### Cleanup

```clojure
(.close jmdns)
```

## Development

```bash
clojure -M:nrepl    # Start nREPL server
```

## License

MIT License. Copyright (c) 2024-2026 Don Jackson. See [LICENSE](LICENSE) for details.
