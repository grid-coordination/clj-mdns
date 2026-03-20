(ns mdns.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [malli.core :as m]
            [mdns.core :as mdns])
  (:import [javax.jmdns ServiceInfo]))

(deftest service-schema-test
  (testing "Service schema validates a well-formed service map"
    (let [service {:mdns.service/name           "test-host"
                   :mdns.service/type           "_http._tcp.local."
                   :mdns.service/qualified-name "test-host._http._tcp.local."
                   :mdns.service/application    "_http"
                   :mdns.service/protocol       :tcp
                   :mdns.service/domain         "local."
                   :mdns.service/subtype        nil
                   :mdns.service/port           8080
                   :mdns.service/server         "test-host.local."
                   :mdns.service/ipv4           "192.168.1.1"
                   :mdns.service/ipv6           nil
                   :mdns.service/addresses      ["192.168.1.1"]
                   :mdns.service/urls           ["http://192.168.1.1:8080"]
                   :mdns.service/priority       0
                   :mdns.service/weight         0
                   :mdns.service/txt            {"path" "/"}}]
      (is (m/validate mdns/Service service))))

  (testing "Service schema rejects invalid protocol"
    (is (not (m/validate mdns/Service
                         {:mdns.service/name "x"
                          :mdns.service/type "t"
                          :mdns.service/qualified-name "q"
                          :mdns.service/application "a"
                          :mdns.service/protocol :invalid
                          :mdns.service/domain "d"
                          :mdns.service/subtype nil
                          :mdns.service/port 80
                          :mdns.service/server "s"
                          :mdns.service/ipv4 nil
                          :mdns.service/ipv6 nil
                          :mdns.service/addresses []
                          :mdns.service/urls []
                          :mdns.service/priority 0
                          :mdns.service/weight 0
                          :mdns.service/txt {}})))))

(deftest event-schema-test
  (testing "Event schema validates well-formed events"
    (let [service {:mdns.service/name           "test"
                   :mdns.service/type           "_http._tcp.local."
                   :mdns.service/qualified-name "test._http._tcp.local."
                   :mdns.service/application    "_http"
                   :mdns.service/protocol       :tcp
                   :mdns.service/domain         "local."
                   :mdns.service/subtype        nil
                   :mdns.service/port           80
                   :mdns.service/server         "test.local."
                   :mdns.service/ipv4           nil
                   :mdns.service/ipv6           nil
                   :mdns.service/addresses      []
                   :mdns.service/urls           []
                   :mdns.service/priority       0
                   :mdns.service/weight         0
                   :mdns.service/txt            {}}]
      (doseq [t [:mdns.event/added :mdns.event/removed :mdns.event/resolved]]
        (is (m/validate mdns/Event {:mdns.event/type t :mdns.event/service service})
            (str "Event type " t " should be valid"))))))

(deftest ->service-test
  (testing "Coerces a ServiceInfo to a service map"
    (let [info    (ServiceInfo/create "_http._tcp.local." "my-service" 8080 "path=/")
          service (mdns/->service info)]
      (is (= "my-service" (:mdns.service/name service)))
      (is (= "_http._tcp.local." (:mdns.service/type service)))
      (is (= :tcp (:mdns.service/protocol service)))
      (is (= 8080 (:mdns.service/port service)))
      (is (map? (:mdns.service/txt service)))
      (is (vector? (:mdns.service/addresses service)))
      (is (vector? (:mdns.service/urls service)))
      (is (m/validate mdns/Service service))))

  (testing "Raw metadata is attached"
    (let [info    (ServiceInfo/create "_http._tcp.local." "raw-test" 9090 "")
          service (mdns/->service info)]
      (is (map? (:mdns/raw (meta service)))))))

(deftest instance-lifecycle-test
  (testing "Create and close a JmDNS instance"
    (let [instance (mdns/create)]
      (is (some? instance))
      (mdns/close instance))))
