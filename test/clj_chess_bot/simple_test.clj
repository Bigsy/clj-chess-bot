(ns clj-chess-bot.simple-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-chess-bot.core :as bot]
            [clj-chess-bot.game :as game]
            [clj-chess-bot.test-helpers :as helpers])
  (:import [chariot.util Board]
           [java.time Duration]
           [java.util.concurrent ConcurrentHashMap]))

(deftest test-make-random-move
  (testing "makes valid move from starting position"
    (let [board (Board/fromFEN "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
          move (game/select-move board :random)]
      (is (string? move))
      (is (re-matches #"[a-h][1-8][a-h][1-8]" move))))

  (testing "returns nil when no valid moves"
    (let [board (Board/fromFEN "3k4/3P4/3K4/8/8/8/8/8 b - - 0 1")]
      (is (nil? (game/select-move board :random)))))

  (testing "handles exception gracefully"
    (is (nil? (game/select-move nil :random)))))

(deftest test-sleep-duration
  (testing "sleeps for specified duration"
    (let [start-time (System/currentTimeMillis)
          duration (Duration/ofMillis 50)]
      (bot/sleep-duration duration)
      (let [elapsed (- (System/currentTimeMillis) start-time)]
        (is (>= elapsed 45))))))

(deftest test-board-operations
  (testing "board creation from FEN"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)]
      (is (some? board))
      (is (.whiteToMove board))
      (is (not (.blackToMove board)))))

  (testing "valid moves from starting position"
    (let [board (Board/fromFEN "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
          moves (.validMoves board)]
      (is (= 20 (.size moves)))
      (is (every? #(re-matches #"[a-h][1-8][a-h][1-8]" (.uci %)) moves))))

  (testing "board after move"
    (let [board (Board/fromFEN "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
          new-board (.play board "e2e4")]
      (is (.blackToMove new-board))
      (is (not (.whiteToMove new-board))))))

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

(deftest test-color-logic
  (testing "turn detection works correctly"
    (let [white-to-move-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          black-to-move-fen "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
          white-board (Board/fromFEN white-to-move-fen)
          black-board (Board/fromFEN black-to-move-fen)]
      (is (.whiteToMove white-board))
      (is (.blackToMove black-board))
      (is (not (.blackToMove white-board)))
      (is (not (.whiteToMove black-board))))))

(deftest test-move-generation-edge-cases
  (testing "checkmate position has no valid moves"
    (let [checkmate-fen "rnb1kbnr/pppp1ppp/4p3/8/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
          board (Board/fromFEN checkmate-fen)
          move (game/select-move board :random)]
      (is (nil? move))))

  (testing "stalemate position has no valid moves"
    (let [stalemate-fen "5k2/5P2/5K2/8/8/8/8/8 b - - 0 1"
          board (Board/fromFEN stalemate-fen)
          move (game/select-move board :random)]
      (is (nil? move)))))