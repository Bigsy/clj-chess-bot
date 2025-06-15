(ns clj-chess-bot.game
  (:require [clojure.tools.logging :as log])
  (:import [java.util Random]))

(def piece-values
  {"p" 1 "n" 3 "b" 3 "r" 5 "q" 9 "k" 0
   "P" 1 "N" 3 "B" 3 "R" 5 "Q" 9 "K" 0})

(def chess-squares
  ["a1" "b1" "c1" "d1" "e1" "f1" "g1" "h1"
   "a2" "b2" "c2" "d2" "e2" "f2" "g2" "h2"
   "a3" "b3" "c3" "d3" "e3" "f3" "g3" "h3"
   "a4" "b4" "c4" "d4" "e4" "f4" "g4" "h4"
   "a5" "b5" "c5" "d5" "e5" "f5" "g5" "h5"
   "a6" "b6" "c6" "d6" "e6" "f6" "g6" "h6"
   "a7" "b7" "c7" "d7" "e7" "f7" "g7" "h7"
   "a8" "b8" "c8" "d8" "e8" "f8" "g8" "h8"])

(defn get-piece-at [board square]
  "Get the piece at a given square"
  (try
    (when-let [piece (.get board square)]
      (.letter piece))
    (catch Exception e nil)))

(defn get-our-pieces [board color]
  "Get all squares containing our pieces"
  (try
    (let [our-color-is-white? (= (str color) "white")]
      (filter (fn [square]
                (when-let [piece (get-piece-at board square)]
                  (and (not= piece "null")
                       (not= piece "")
                       (if our-color-is-white?
                         (Character/isUpperCase ^char (first piece))
                         (Character/isLowerCase ^char (first piece))))))
              chess-squares))
    (catch Exception e [])))

(defn can-attack-square? [board from-square to-square]
  "Check if a piece on from-square can attack to-square"
  (try
    (let [valid-moves (vec (.validMoves board))]
      (some (fn [move] 
              (and (= (str (.from move)) from-square)
                   (= (str (.to move)) to-square)))
            valid-moves))
    (catch Exception e false)))

(defn is-piece-attacked? [board square attacking-color]
  "Check if a piece at a square is attacked by the attacking color"
  (try
    (let [attacking-pieces (get-our-pieces board attacking-color)
          current-turn-is-white (.whiteToMove board)
          attacking-is-white (= (str attacking-color) "white")]
      
      (if (= current-turn-is-white attacking-is-white)
        (some (fn [piece-square]
                (can-attack-square? board piece-square square))
              attacking-pieces)
        (let [valid-moves (vec (.validMoves board))
              any-legal-move (first valid-moves)]
          (if any-legal-move
            (let [test-board (.play board (.uci any-legal-move))
                  attacking-pieces-after-move (get-our-pieces test-board attacking-color)]
              (some (fn [piece-square]
                      (can-attack-square? test-board piece-square square))
                    attacking-pieces-after-move))
            false))))
    (catch Exception e 
      (log/error e (str "Error in is-piece-attacked? for square " square " attacking-color " attacking-color))
      false)))

(defn find-hanging-pieces [board color]
  "Find pieces that are hanging (can be captured without adequate defense)"
  (try
    (let [our-pieces (get-our-pieces board color)
          opponent-color (if (= (str color) "white") "black" "white")]
      (filter (fn [square]
                (let [piece (get-piece-at board square)
                      piece-value (get piece-values piece 0)]
                  (and (> piece-value 0)
                       (is-piece-attacked? board square opponent-color))))
              our-pieces))
    (catch Exception e [])))

(defn find-defending-moves [board hanging-square color]
  "Find moves that can defend a hanging piece"
  (try
    (let [valid-moves (vec (.validMoves board))
          opponent-color (if (= (str color) "white") "black" "white")]
      (filter (fn [move]
                (try
                  (let [test-board (.play board (.uci move))]
                    (not (is-piece-attacked? test-board hanging-square opponent-color)))
                  (catch Exception e false)))
              valid-moves))
    (catch Exception e [])))

(defn find-king-square [board color]
  "Find the square where the king of the given color is located"
  (try
    (let [target-king (if (= (str color) "white") "K" "k")]
      (first (filter (fn [square]
                       (= (get-piece-at board square) target-king))
                     chess-squares)))
    (catch Exception e nil)))

(defn move-gives-check? [board move color]
  "Check if making this move puts the opponent king in check"
  (try
    (let [test-board (.play board (.uci move))
          opponent-color (if (= (str color) "white") "black" "white")
          opponent-king-square (find-king-square test-board opponent-color)]
      (if opponent-king-square
        (is-piece-attacked? test-board opponent-king-square color)
        false))
    (catch Exception e false)))

(defn move-gives-checkmate? [board move color]
  "Check if making this move results in checkmate"
  (try
    (let [test-board (.play board (.uci move))
          opponent-moves (vec (.validMoves test-board))]
      (and (move-gives-check? board move color)
           (empty? opponent-moves)))
    (catch Exception e false)))

(defn find-checkmate-moves [board color]
  "Find all moves that result in checkmate"
  (try
    (let [valid-moves (vec (.validMoves board))]
      (filter #(move-gives-checkmate? board % color) valid-moves))
    (catch Exception e [])))

(defn find-check-moves [board color]
  "Find all moves that put the opponent in check (but not checkmate)"
  (try
    (let [valid-moves (vec (.validMoves board))]
      (filter #(and (move-gives-check? board % color)
                    (not (move-gives-checkmate? board % color))) valid-moves))
    (catch Exception e [])))

(defn evaluate-capture [board move]
  "Evaluate a capture move, return positive if good, negative if bad"
  (try
    (let [from-square (str (.from move))
          to-square (str (.to move))
          moving-piece (get-piece-at board from-square)
          captured-piece (get-piece-at board to-square)]
      (if (and moving-piece captured-piece (not= captured-piece "null"))
        (- (get piece-values captured-piece 0)
           (get piece-values moving-piece 0))
        0))
    (catch Exception e 0)))

(defn select-best-capture [board valid-moves]
  "Select the best capture move from valid moves"
  (let [moves-with-scores (map (fn [move]
                                {:move move 
                                 :score (evaluate-capture board move)})
                              valid-moves)
        good-moves (filter #(>= (:score %) 0) moves-with-scores)]
    (if (seq good-moves)
      (-> good-moves shuffle first :move .uci)
      (-> valid-moves shuffle first .uci))))

(defn try-defend-hanging-pieces [board color valid-moves hanging-pieces]
  "Try to defend hanging pieces, fallback to best capture"
  (log/info (str "Found hanging pieces at: " hanging-pieces))
  (let [defending-moves (filter some? (mapcat #(find-defending-moves board % color) hanging-pieces))]
    (if (seq defending-moves)
      (let [selected-move (-> defending-moves shuffle first)]
        (if selected-move
          (do
            (log/info "Found defending moves, selecting one")
            (.uci selected-move))
          (do
            (log/info "Selected defending move was nil, falling back to capture evaluation")
            (select-best-capture board valid-moves))))
      (do
        (log/info "No defending moves found, falling back to capture evaluation")
        (select-best-capture board valid-moves)))))

(defn make-smart-move [board color]
  "Generate a move prioritizing checkmate, check, defending pieces, and good captures"
  (try
    (let [valid-moves (vec (.validMoves board))]
      (when (seq valid-moves)
        (let [checkmate-moves (find-checkmate-moves board color)]
          (if (seq checkmate-moves)
            (do
              (log/info "Found checkmate move!")
              (-> checkmate-moves shuffle first .uci))
            (let [check-moves (find-check-moves board color)]
              (if (seq check-moves)
                (do
                  (log/info "Found check move!")
                  (-> check-moves shuffle first .uci))
                (let [hanging-pieces (find-hanging-pieces board color)]
                  (if (seq hanging-pieces)
                    (try-defend-hanging-pieces board color valid-moves hanging-pieces)
                    (select-best-capture board valid-moves)))))))))
    (catch Exception e
      (log/error e "Error making smart move")
      nil)))

(defn make-random-move [board]
  "Generate a random legal move from the current board position"
  (try
    (let [valid-moves (vec (.validMoves board))]
      (when (seq valid-moves)
        (let [shuffled-moves (shuffle valid-moves)]
          (-> shuffled-moves first .uci))))
    (catch Exception e
      (log/error e "Error making move")
      nil)))

(defn create-board-from-fen [fen-string]
  "Create a board instance from FEN notation"
  (try
    (let [board-class (Class/forName "chariot.util.Board")
          from-fen-method (.getMethod board-class "fromFEN" (into-array Class [String]))]
      (.invoke from-fen-method nil (into-array Object [fen-string])))
    (catch Exception e
      (log/error e "Error creating board from FEN")
      nil)))

(defn apply-moves-to-board [board moves-string]
  "Apply a string of moves to a board and return the resulting position"
  (try
    (if (clojure.string/blank? moves-string)
      board
      (.play board moves-string))
    (catch Exception e
      (log/error e "Error applying moves to board")
      nil)))

(defn is-my-turn? [board color]
  "Check if it's the bot's turn to move based on board state and bot color"
  (let [white-to-move? (.whiteToMove board)]
    (if (= (str color) "white")
      white-to-move?
      (not white-to-move?))))

(defn select-move [board color move-strategy]
  "Select a move based on the given strategy"
  (case move-strategy
    :smart (make-smart-move board color)
    :random (make-random-move board)
    (make-smart-move board color)))