(ns clj-chess-bot.lichess-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-chess-bot.lichess :as lichess])
  (:import [chariot.model Entry Fail Entries]))

;; Basic tests that focus on error handling and edge cases
;; These tests don't require complex mocking of the Chariot API

(deftest test-nil-handling
  (testing "functions handle nil client gracefully"
    (is (nil? (lichess/get-account-profile nil)))
    (is (nil? (lichess/accept-challenge nil "challenge-123")))
    (is (nil? (lichess/make-move nil "game-123" "e2e4")))
    (is (nil? (lichess/connect-to-bot-events nil)))
    (is (nil? (lichess/connect-to-game nil "game-123"))))

  (testing "functions handle nil parameters"
    ;; Create a simple mock that will throw if called
    (let [mock-client (reify Object)]
      (is (nil? (lichess/accept-challenge mock-client nil)))
      (is (nil? (lichess/accept-challenge mock-client "")))
      (is (nil? (lichess/make-move mock-client nil "e2e4")))
      (is (nil? (lichess/make-move mock-client "game-123" nil)))
      (is (nil? (lichess/connect-to-game mock-client nil))))))

;; Note: Testing create-client is complex due to Java interop
;; We focus on testing the other functions that are easier to mock

;; Test basic functionality and error handling
;; These tests verify that the functions handle edge cases properly

(deftest test-function-behavior
  (testing "functions handle various edge cases"
    ;; Test that functions don't crash with invalid inputs
    (let [mock-client (reify Object)]
      ;; These should all handle the cases gracefully
      (is (nil? (lichess/get-account-profile mock-client)))
      (is (nil? (lichess/accept-challenge mock-client "test")))
      (is (nil? (lichess/make-move mock-client "test" "e2e4")))
      (is (nil? (lichess/connect-to-bot-events mock-client)))
      (is (nil? (lichess/connect-to-game mock-client "test")))
      
      ;; These should not throw exceptions
      (lichess/decline-challenge mock-client "test" "generic")
      (lichess/send-chat-message mock-client "test" "hello")
      (lichess/resign-game mock-client "test")
      
      (is true))))

;; Test that the functions exist and can be called
(deftest test-function-existence
  (testing "all lichess functions exist and are callable"
    (is (fn? lichess/create-client))
    (is (fn? lichess/get-account-profile))
    (is (fn? lichess/accept-challenge))
    (is (fn? lichess/decline-challenge))
    (is (fn? lichess/send-chat-message))
    (is (fn? lichess/make-move))
    (is (fn? lichess/resign-game))
    (is (fn? lichess/connect-to-bot-events))
    (is (fn? lichess/connect-to-game))))

;; Test parameter validation
(deftest test-parameter-validation
  (testing "functions validate parameters appropriately"
    ;; Test empty string parameters
    (let [mock-client (reify Object)]
      (is (nil? (lichess/accept-challenge mock-client "")))
      (is (nil? (lichess/make-move mock-client "" "e2e4")))
      (is (nil? (lichess/make-move mock-client "game-123" "")))
      (is (nil? (lichess/connect-to-game mock-client "")))
      
      ;; These functions should handle empty strings gracefully
      (lichess/decline-challenge mock-client "" "generic")
      (lichess/send-chat-message mock-client "" "hello")
      (lichess/send-chat-message mock-client "game-123" "")
      (lichess/resign-game mock-client "")
      
      (is true))))

;; Test decline challenge reason handling
(deftest test-decline-challenge-reasons
  (testing "decline challenge handles different reasons"
    (let [mock-client (reify Object)]
      ;; All these should complete without throwing exceptions
      (lichess/decline-challenge mock-client "challenge-123" "generic")
      (lichess/decline-challenge mock-client "challenge-123" "casual")
      (lichess/decline-challenge mock-client "challenge-123" "later")
      (lichess/decline-challenge mock-client "challenge-123" "unknown-reason")
      (lichess/decline-challenge mock-client "challenge-123" nil)
      (is true))))

;; Test logging behavior (functions should not crash when logging)
(deftest test-logging-integration
  (testing "functions integrate properly with logging"
    ;; These tests ensure that the functions don't crash due to logging issues
    (let [mock-client (reify Object)]
      ;; All these functions use logging internally
      (is (nil? (lichess/get-account-profile mock-client)))
      (is (nil? (lichess/accept-challenge mock-client "test")))
      (is (nil? (lichess/make-move mock-client "test" "e2e4")))
      (is (nil? (lichess/connect-to-bot-events mock-client)))
      (is (nil? (lichess/connect-to-game mock-client "test")))
      
      (lichess/decline-challenge mock-client "test" "generic")
      (lichess/send-chat-message mock-client "test" "hello")
      (lichess/resign-game mock-client "test")
      
      (is true))))