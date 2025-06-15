(ns clj-chess-bot.game
  (:require [clojure.tools.logging :as log])
  (:import [java.util Random]))

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

(defn select-move [board move-strategy]
  "Select a move based on the given strategy"
  (case move-strategy
    :random (make-random-move board)
    (make-random-move board)))