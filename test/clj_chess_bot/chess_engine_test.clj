(ns clj-chess-bot.chess-engine-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-chess-bot.game :as game])
  (:import [chariot.util Board]))

(deftest test-piece-values
  (testing "piece values are defined correctly"
    (is (= 1 (get game/piece-values "p")))
    (is (= 1 (get game/piece-values "P")))
    (is (= 3 (get game/piece-values "n")))
    (is (= 3 (get game/piece-values "N")))
    (is (= 5 (get game/piece-values "r")))
    (is (= 5 (get game/piece-values "R")))
    (is (= 9 (get game/piece-values "q")))
    (is (= 9 (get game/piece-values "Q")))
    (is (= 0 (get game/piece-values "k")))
    (is (= 0 (get game/piece-values "K")))))

(deftest test-smart-move-selection
  (testing "prefers capturing higher value pieces"
    (let [fen "r1bqkb1r/ppp2ppp/2np1n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 5"
          board (Board/fromFEN fen)
          move (game/make-smart-move board "white")]
      (is (string? move))
      (is (re-matches #"[a-h][1-8][a-h][1-8]" move))))
  
  (testing "prioritizes defending pieces over bad captures"
    (let [fen "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 2 3"
          board (Board/fromFEN fen)]
      (dotimes [_ 5]
        (let [move (game/make-smart-move board "white")]
          (is (string? move))
          (is (re-matches #"[a-h][1-8][a-h][1-8]" move))))))
  
  (testing "handles positions with no captures available"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          move (game/make-smart-move board "white")]
      (is (string? move))
      (is (re-matches #"[a-h][1-8][a-h][1-8]" move))))
  
  (testing "returns nil for checkmate position"
    (let [fen "rnb1kbnr/pppp1ppp/4p3/8/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"
          board (Board/fromFEN fen)
          move (game/make-smart-move board "white")]
      (is (nil? move))))
  
  (testing "handles exception gracefully"
    (is (nil? (game/make-smart-move nil "white")))))

(deftest test-capture-evaluation
  (testing "queen takes pawn is good"
    (let [fen "rnbqkb1r/pppp1ppp/5n2/4p3/2B1P3/3Q4/PPPP1PPP/RNB1K1NR w KQkq - 2 3"
          board (Board/fromFEN fen)
          moves (.validMoves board)
          queen-takes-pawn (first (filter #(= "d3e4" (.uci %)) moves))]
      (when queen-takes-pawn
        (let [score (game/evaluate-capture board queen-takes-pawn "white")]
          (is (pos? score))))))
  
  (testing "pawn takes queen is very good"
    (let [fen "rnb1kb1r/pppp1ppp/5n2/4p3/2B1P3/3q4/PPPP1PPP/RNB1K1NR w KQkq - 2 3"
          board (Board/fromFEN fen)
          moves (.validMoves board)
          pawn-takes-queen (first (filter #(= "e4d3" (.uci %)) moves))]
      (when pawn-takes-queen
        (let [score (game/evaluate-capture board pawn-takes-queen "white")]
          (is (> score 5))))))
  
  (testing "non-capture moves have zero score"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          moves (.validMoves board)
          pawn-move (first (filter #(= "e2e4" (.uci %)) moves))]
      (when pawn-move
        (let [score (game/evaluate-capture board pawn-move "white")]
          (is (= 0 score)))))))

(deftest test-board-utilities
  (testing "create board from FEN"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (game/create-board-from-fen fen)]
      (is (some? board))
      (is (.whiteToMove board))))
  
  (testing "apply moves to board"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (game/create-board-from-fen fen)
          new-board (game/apply-moves-to-board board "e2e4")]
      (is (some? new-board))
      (is (.blackToMove new-board))))
  
  (testing "detect turn correctly"
    (let [white-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          black-fen "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
          white-board (game/create-board-from-fen white-fen)
          black-board (game/create-board-from-fen black-fen)]
      (is (game/is-my-turn? white-board "white"))
      (is (not (game/is-my-turn? white-board "black")))
      (is (game/is-my-turn? black-board "black"))
      (is (not (game/is-my-turn? black-board "white"))))))

(deftest test-move-selection-strategies
  (testing "select-move uses smart strategy by default"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (game/create-board-from-fen fen)
          move1 (game/select-move board "white" :smart)
          move2 (game/select-move board "white" nil)]
      (is (string? move1))
      (is (string? move2))
      (is (re-matches #"[a-h][1-8][a-h][1-8]" move1))
      (is (re-matches #"[a-h][1-8][a-h][1-8]" move2)))))

(deftest test-piece-identification
  (testing "gets our pieces correctly for white"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          white-pieces (game/get-our-pieces board "white")]
      (is (= 16 (count white-pieces)))
      (is (every? #(re-matches #"[a-h][1-2]" %) white-pieces))))
  
  (testing "gets our pieces correctly for black"
    (let [fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
          board (Board/fromFEN fen)
          black-pieces (game/get-our-pieces board "black")]
      (is (= 16 (count black-pieces)))
      (is (every? #(re-matches #"[a-h][7-8]" %) black-pieces)))))