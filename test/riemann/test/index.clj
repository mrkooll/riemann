(ns riemann.test.index
  (:use [riemann.index])
  (:use [riemann.query])
  (:use [riemann.common])
  (:use [clojure.test]))

(deftest nbhm-update
         (let [i (nbhm-index)]
           (update i {:host 1})
           (update i {:host 2})
           (update i {:host 1 :service 3 :state :ok})
           (update i {:host 1 :service 3 :description "new"})

           (is (= (set i)
                  #{{:host 1}
                    {:host 2}
                    {:host 1 :service 3 :description "new"}}))))

(deftest nhbm-delete
         (let [i (nbhm-index)]
           (update i {:host 1})
           (update i {:host 2})
           (delete i {:host 1 :service 1})
           (delete i {:host 2 :state :ok})
           (is (= (set i)
                  #{{:host 1}}))))

(deftest nhbm-search
         (let [i (nbhm-index)]
           (update i {:host 1})
           (update i {:host 2 :service "meow"})
           (update i {:host 3 :service "mrrrow"})
           (is (= (set (search i (ast "host >= 2 and not service =~ \"%r%\"")))
                  #{{:host 2 :service "meow"}}))))

(deftest nhbm-expire
         (let [i (nbhm-index)]
           (update i {:host 1 :ttl 0 :time (unix-time)})
           (update i {:host 2 :ttl 10 :time (unix-time)})
           (update i {:host 3 :ttl 20 :time (- (unix-time) 21)})

           (let [expired (expire i)]
             (is (= (set (map (fn [e] (:host e)) 
                              expired))
                    #{1 3})))

           (is (= (map (fn [e] (:host e)) i)
                  [2]))))

(defn random-event
  [& {:as event}]
  (merge {:host    (rand-int 100)
          :service (rand-int 100)
          :ttl     (rand-int 500)
          :time    (- (unix-time) (rand-int 30))}
         event))

(deftest ^:bench indexing-nbhm-time
  (let [_        (println "building events, this might take some time")
        not-much (doall (repeatedly 100 random-event))
        a-few    (doall (repeatedly 100000 random-event))
        a-lot    (doall (repeatedly 1000000 random-event))
        i        (nbhm-index)]
    (println "updating and expiring the same 100 events 10000 times:")
    (time (dotimes [iter 10000]
            (do (doseq [event not-much]
                  (update i event)))))
    (println "expiring")
    (time (dotimes [iter 10000] (doall (expire i))))
    (clear i)

    (println "updating and expiring the same 100000 events 100 times:")
    (time (dotimes [iter 100]
            (do (doseq [event a-few]
                  (update i event)))))
    (println "expiring")
    (time (dotimes [iter 100] (doall (expire i))))
    (clear i)

    (println "updating and expiring the same 10000000 events 10 times:")
    (time (dotimes [iter 10]
            (do (doseq [event a-lot]
                  (update i event)))))
    (println "expiring")
    (time (dotimes [iter 10] (doall (expire i))))))