(ns clj-chess-bot.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-chess-bot.core :as bot]
            [clj-chess-bot.test-helpers :as helpers])
  (:import [java.time Duration]
           [java.util.concurrent ConcurrentHashMap]))

(deftest test-sleep-duration
  (testing "sleeps for specified duration"
    (let [start-time (System/currentTimeMillis)
          duration (Duration/ofMillis 50)]
      (bot/sleep-duration duration)
      (let [elapsed (- (System/currentTimeMillis) start-time)]
        (is (>= elapsed 45))))))

(deftest test-decline-challenge-logic
  (testing "decline challenge when no challenger"
    (let [event (helpers/make-challenge-event "challenge-1" false false false)]
      (is (= {:reason "generic" :msg "No challenger"}
             (bot/decline-challenge? event)))))

  (testing "decline rated games"
    (let [event (helpers/make-challenge-event "challenge-2" true)]
      (is (= {:reason "casual" :msg "Rated"}
             (bot/decline-challenge? event)))))

  (testing "decline when too many games"
    (let [event (helpers/make-challenge-event "challenge-3" false)]
      (with-redefs [bot/games (let [g (ConcurrentHashMap.)]
                                (dotimes [i 10]
                                  (.put g (str "player" i) (str "game" i)))
                                g)]
        (is (= {:reason "later" :msg "Too many games"}
               (bot/decline-challenge? event))))))

  (testing "accept valid challenge"
    (let [event (helpers/make-challenge-event "challenge-4" false)]
      (is (nil? (bot/decline-challenge? event))))))

(deftest test-game-management
  (testing "games map operations"
    (let [games (ConcurrentHashMap.)]
      (.put games "opponent1" "game1")
      (.put games "opponent2" "game2")
      (is (= 2 (.size games)))
      (is (= "game1" (.get games "opponent1")))
      (.remove games "opponent1" "game1")
      (is (= 1 (.size games))))))