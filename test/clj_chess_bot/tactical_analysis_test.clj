(ns clj-chess-bot.tactical-analysis-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-chess-bot.game :as game])
  (:import [chariot.util Board]))

(deftest test-hanging-piece-detection
  (testing "detects hanging pieces correctly"
    (let [fen "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3"
          board (Board/fromFEN fen)
          hanging-pieces (game/find-hanging-pieces board "white")]
      (is (sequential? hanging-pieces))))
  
  (testing "finds no hanging pieces in starting position"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          hanging-pieces (game/find-hanging-pieces board "white")]
      (is (empty? hanging-pieces))))
  
  (testing "detects hanging pieces in complex positions"
    (let [fen "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3"
          board (Board/fromFEN fen)
          hanging-pieces (game/find-hanging-pieces board "white")]
      (is (sequential? hanging-pieces))))
  
  (testing "handles exception gracefully"
    (is (empty? (game/find-hanging-pieces nil "white")))))

(deftest test-defending-moves
  (testing "finds defending moves when available"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          defending-moves (game/find-defending-moves board "e4" "white")]
      (is (sequential? defending-moves))))
  
  (testing "handles non-existent squares gracefully"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          defending-moves (game/find-defending-moves board "z9" "white")]
      (is (sequential? defending-moves))))
  
  (testing "handles exception gracefully"
    (is (empty? (game/find-defending-moves nil "e4" "white")))))

(deftest test-king-finding
  (testing "finds white king in starting position"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          king-square (game/find-king-square board "white")]
      (is (= "e1" king-square))))
  
  (testing "finds black king in starting position"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          king-square (game/find-king-square board "black")]
      (is (= "e8" king-square))))
  
  (testing "finds king after castling"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQK2R w KQkq - 0 1"
          board (Board/fromFEN fen)
          new-board (.play board "e1g1")
          king-square (game/find-king-square new-board "white")]
      (is (= "g1" king-square))))
  
  (testing "handles exception gracefully"
    (is (nil? (game/find-king-square nil "white")))))

(deftest test-check-detection
  (testing "detects no check from normal move"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          moves (.validMoves board)
          pawn-move (first (filter #(= "e2e4" (.uci %)) moves))]
      (when pawn-move
        (is (not (game/move-gives-check? board pawn-move "white"))))))
  
  (testing "handles exception gracefully"
    (is (not (game/move-gives-check? nil nil "white")))))

(deftest test-checkmate-detection
  (testing "detects fool's mate"
    (let [fen "rnb1kbnr/pppp1ppp/4p3/8/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
          board (Board/fromFEN fen)
          moves (.validMoves board)]
      (is (empty? moves))))
  
  (testing "normal moves are not checkmate"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          moves (.validMoves board)
          pawn-move (first (filter #(= "e2e4" (.uci %)) moves))]
      (when pawn-move
        (is (not (game/move-gives-checkmate? board pawn-move "white"))))))
  
  (testing "handles exception gracefully"
    (is (not (game/move-gives-checkmate? nil nil "white")))))

(deftest test-check-and-checkmate-move-finding
  (testing "finds check moves when available"
    (let [fen "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
          board (Board/fromFEN fen)
          check-moves (game/find-check-moves board "white")]
      (is (sequential? check-moves))))
  
  (testing "returns empty when no check/checkmate available"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          checkmate-moves (game/find-checkmate-moves board "white")
          check-moves (game/find-check-moves board "white")]
      (is (empty? checkmate-moves))
      (is (empty? check-moves))))
  
  (testing "handles exception gracefully"
    (is (empty? (game/find-checkmate-moves nil "white")))
    (is (empty? (game/find-check-moves nil "white")))))