(ns clj-chess-bot.lichess
  (:require [clojure.tools.logging :as log])
  (:import [chariot Client]
           [java.time Duration]))

(defn create-client [bot-token & [lichess-api]]
  "Create a Lichess API client with the given token"
  (try
    (let [api-url (or lichess-api "https://lichess.org")
          config-fn (reify java.util.function.Consumer
                      (accept [_ builder]
                        (.api builder (java.net.URI/create api-url))))
          client (Client/auth config-fn bot-token)]
      (log/info "Lichess client created")
      client)
    (catch Exception e
      (log/error e "Failed to create Lichess client")
      nil)))

(defn get-account-profile [client]
  "Get the bot account profile"
  (try
    (let [profile-result (-> client .account .profile)]
      (if (= (str (type profile-result)) "class chariot.model.Entry")
        (.entry profile-result)
        (do
          (log/error (str "Failed to get account profile: " profile-result))
          nil)))
    (catch Exception e
      (log/error e "Failed to get account profile")
      nil)))

(defn accept-challenge [client challenge-id]
  "Accept a challenge by ID"
  (try
    (let [accept-result (-> client .challenges (.acceptChallenge challenge-id))]
      (log/info (str "Challenge accepted: " accept-result))
      accept-result)
    (catch Exception e
      (log/error e "Failed to accept challenge")
      nil)))

(defn decline-challenge [client challenge-id reason]
  "Decline a challenge with the given reason"
  (try
    (-> client .challenges (.declineChallenge challenge-id 
                             (fn [d] (case reason
                                    "generic" (.generic d)
                                    "casual" (.casual d)
                                    "later" (.later d)
                                    (.generic d)))))
    (log/info "Challenge declined successfully")
    (catch Exception e
      (log/error e "Failed to decline challenge"))))

(defn send-chat-message [client game-id message]
  "Send a chat message in a game"
  (try
    (-> client .bot (.chat game-id message))
    (log/info (str "Chat message sent: " message))
    (catch Exception e
      (log/error e "Failed to send chat message"))))

(defn make-move [client game-id move-uci]
  "Make a move in a game using UCI notation"
  (try
    (let [result (-> client .bot (.move game-id move-uci))]
      (log/info (str "Move submitted: " move-uci " -> " result))
      result)
    (catch Exception e
      (log/error e "Failed to make move")
      nil)))

(defn resign-game [client game-id]
  "Resign from a game"
  (try
    (-> client .bot (.resign game-id))
    (log/info "Game resigned")
    (catch Exception e
      (log/error e "Failed to resign game"))))

(defn connect-to-bot-events [client]
  "Connect to the bot event stream"
  (try
    (let [events (-> client .bot .connect)]
      (log/info "Connected to Lichess bot API")
      (if (= (str (type events)) "class chariot.model.Fail")
        (do
          (log/warn (str "Failed to connect: " events))
          nil)
        events))
    (catch Exception e
      (log/error e "Failed to connect to bot events")
      nil)))

(defn connect-to-game [client game-id]
  "Connect to a specific game's event stream"
  (try
    (let [connect-result (-> client .bot (.connectToGame game-id))]
      (log/info (str "Connected to game: " game-id))
      (if (= (str (type connect-result)) "class chariot.model.Entries")
        connect-result
        (do
          (log/warn (str "Failed to connect to game " game-id ": " connect-result))
          nil)))
    (catch Exception e
      (log/error e "Failed to connect to game")
      nil)))