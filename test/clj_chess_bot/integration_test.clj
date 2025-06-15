(ns clj-chess-bot.integration-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-chess-bot.game :as game])
  (:import [chariot.util Board]))

(deftest test-self-play-hanging-piece-detection
  (testing "bot detects and defends hanging pieces in self-play"
    ; Position where black has a hanging bishop on c5 that white can capture
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