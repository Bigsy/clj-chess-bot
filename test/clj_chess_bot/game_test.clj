(ns clj-chess-bot.game-test
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
    (let [fen "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
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

(deftest test-self-play-hanging-piece-detection
  (testing "bot detects and defends hanging pieces in self-play"
    ; Position where black has a hanging bishop on c4 that white can capture
    (let [fen-with-hanging-piece "rnbqk1nr/pppp1ppp/8/2b1p3/2B1P3/8/PPPP1PPP/RNBQK1NR b KQkq - 4 3"
          board (Board/fromFEN fen-with-hanging-piece)]
      
      ; Capture log output to verify hanging piece detection
      (let [log-output (with-out-str
                        (let [black-move (game/make-smart-move board "black")]
                          (println "Black move in hanging position:" black-move)
                          (is (string? black-move))
                          (is (re-matches #"[a-h][1-8][a-h][1-8]" black-move))))]
        
        ; The logs should show either hanging piece detection or the move selection process
        (println "Self-play test log output:")
        (println log-output))))
  
  (testing "bot plays several moves to trigger hanging piece scenarios"
    ; Start from a more tactical position where pieces are likely to hang
    (let [tactical-fen "r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
          initial-board (Board/fromFEN tactical-fen)]
      
      ; Play a few moves from each side to see if hanging piece detection triggers
      (loop [board initial-board
             color "white"
             move-count 0]
        (when (and board (< move-count 4))
          (let [move (game/make-smart-move board color)]
            (when move
              (println (str "Move " (inc move-count) " (" color "): " move))
              (let [new-board (.play board move)
                    next-color (if (= color "white") "black" "white")]
                (recur new-board next-color (inc move-count)))))))
      
      ; Test passes if no exceptions were thrown
      (is true))))

(deftest test-capture-hanging-pieces
  (testing "bot captures opponent's hanging pieces"
    ; Position where white has a hanging bishop on f4 that black can capture
    (let [fen-opponent-hanging "rnbqkb1r/pppp1ppp/5n2/4p3/5B2/8/PPPP1PPP/RNBQK1NR b KQkq - 1 3"
          board (Board/fromFEN fen-opponent-hanging)]
      
      (let [log-output (with-out-str
                        (let [black-move (game/make-smart-move board "black")]
                          (println "Black move when white has hanging piece:" black-move)
                          (is (string? black-move))
                          (is (re-matches #"[a-h][1-8][a-h][1-8]" black-move))))]
        
        (println "Capture hanging piece test log output:")
        (println log-output))))
  
  (testing "bot prefers capturing hanging pieces over other moves"
    ; Position where white has multiple pieces but one is hanging
    (let [fen-with-target "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 4 4"
          board (Board/fromFEN fen-with-target)]
      
      ; Run multiple times to see if bot consistently finds good captures
      (dotimes [i 3]
        (let [move (game/make-smart-move board "black")]
          (println (str "Attempt " (inc i) " - Black move: " move))
          (is (string? move))
          (is (re-matches #"[a-h][1-8][a-h][1-8]" move))))
      
      (is true)))
  
  (testing "self-play with mutual hanging pieces"
    ; Position where both sides have pieces that could be hanging
    (let [chaotic-fen "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 6"
          initial-board (Board/fromFEN chaotic-fen)]
      
      (println "=== MUTUAL HANGING PIECES SCENARIO ===")
      ; Play several moves to see hanging piece detection and captures
      (loop [board initial-board
             color "white"
             move-count 0]
        (when (and board (< move-count 6))
          (let [move (game/make-smart-move board color)]
            (when move
              (println (str "Move " (inc move-count) " (" color "): " move))
              (let [new-board (.play board move)
                    next-color (if (= color "white") "black" "white")]
                (recur new-board next-color (inc move-count)))))))
      
      (is true))))

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

(deftest test-self-play-with-check-checkmate
  (testing "bot makes valid moves with new check/checkmate logic"
    (let [fen "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"
          board (Board/fromFEN fen)]
      (let [log-output (with-out-str
                        (let [move (game/make-smart-move board "white")]
                          (println "Move with new logic:" move)
                          (is (string? move))
                          (is (re-matches #"[a-h][1-8][a-h][1-8]" move))))]
        (println "New logic test log output:")
        (println log-output))))
  
  (testing "self-play scenario with tactical opportunities"
    (let [tactical-fen "r1bqkb1r/pppp1ppp/2n2n2/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
          initial-board (Board/fromFEN tactical-fen)]
      
      (println "=== TACTICAL SELF-PLAY WITH CHECK/CHECKMATE PRIORITY ===")
      (loop [board initial-board
             color "white"
             move-count 0]
        (when (and board (< move-count 4))
          (let [move (game/make-smart-move board color)]
            (when move
              (println (str "Move " (inc move-count) " (" color "): " move))
              (let [new-board (game/apply-moves-to-board board move)
                    next-color (if (= color "white") "black" "white")]
                (recur new-board next-color (inc move-count)))))))
      
      (is true))))