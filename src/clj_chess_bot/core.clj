(ns clj-chess-bot.core
  (:require [clojure.tools.logging :as log])
  (:import [chariot Client]
           [java.time Duration]
           [java.util Random Collections]
           [java.util.concurrent ConcurrentHashMap])
  (:gen-class))

(def bot-token (System/getenv "BOT_TOKEN"))
(def games (ConcurrentHashMap.))

(defn sleep-duration [duration]
  (try
    (Thread/sleep (.toMillis duration))
    (catch InterruptedException _ nil)))

(defn decline-challenge? [event]
  (let [challenge (.challenge event)
        game-type (.gameType challenge)
        players (.players challenge)
        challenger-opt (.challengerOpt players)]
    (cond
      (not (.isPresent challenger-opt))
      {:reason "generic" :msg "No challenger"}
      
      (.rated game-type)
      {:reason "casual" :msg "Rated"}
      
      (> (.size games) 8)
      {:reason "later" :msg "Too many games"}
      
      :else nil)))

(defn handle-challenge [event client]
  (log/info "=== HANDLING CHALLENGE ===")
  (log/info (str "Challenge event: " event))
  (log/info (str "Challenge ID: " (.id event)))
  
  (if-let [decline-info (decline-challenge? event)]
    (do
      (log/info (str "DECLINING challenge - reason: " (:msg decline-info)))
      (try
        (-> client .challenges (.declineChallenge (.id event) 
                                 (fn [d] (case (:reason decline-info)
                                          "generic" (.generic d)
                                          "casual" (.casual d)
                                          "later" (.later d)
                                          (.generic d)))))
        (log/info "Challenge declined successfully")
        (catch Exception e
          (log/error e "Failed to decline challenge"))))
    (do
      (log/info "ACCEPTING challenge")
      (try
        (let [accept-result (-> client .challenges (.acceptChallenge (.id event)))]
          (log/info (str "Accept result type: " (type accept-result)))
          (log/info (str "Accept result: " accept-result))
          
          (let [challenger-opt (-> event .challenge .players .challengerOpt)
                opponent (if (.isPresent challenger-opt)
                          (.name (.user (.get challenger-opt)))
                          "Opponent")]
            (log/info (str "Challenge accepted from " opponent))
            (sleep-duration (Duration/ofSeconds 1))
            (let [greeting (if (.isRematch event)
                            "Again!"
                            (str "Hi " opponent "!\nI wish you a good game!"))]
              (log/info (str "Sending greeting: " greeting))
              (-> client .bot (.chat (.id event) greeting)))))
        (catch Exception e
          (log/error e "Error handling challenge accept"))))))

(defn make-random-move [board]
  (try
    (let [valid-moves (vec (.validMoves board))]
      (when (seq valid-moves)
        (let [shuffled-moves (shuffle valid-moves)]
          (-> shuffled-moves first .uci))))
    (catch Exception e
      (log/error e "Error making move")
      nil)))

(defn process-moves [game-id color fen-at-start moves client]
  (try
    (log/info (str "=== PROCESSING MOVES ==="))
    (log/info (str "Game ID: " game-id))
    (log/info (str "Color: " color))
    (log/info (str "FEN at start: " fen-at-start))
    (log/info (str "Moves: '" moves "'"))
    
    (let [board-class (Class/forName "chariot.util.Board")
          from-fen-method (.getMethod board-class "fromFEN" (into-array Class [String]))
          board (if (clojure.string/blank? moves)
                  (.invoke from-fen-method nil (into-array Object [fen-at-start]))
                  (let [initial-board (.invoke from-fen-method nil (into-array Object [fen-at-start]))]
                    (.play initial-board moves)))
          white-to-move? (.whiteToMove board)
          my-turn? (if (= (str color) "white")
                     white-to-move?
                     (not white-to-move?))]
      
      (log/info (str "White to move: " white-to-move?))
      (log/info (str "My turn: " my-turn?))
      
      (if my-turn?
        (do
          (log/info "It's my turn - trying to make a move")
          (if-let [move (make-random-move board)]
            (do
              (log/info (str "Generated move: " move))
              (let [result (-> client .bot (.move game-id move))]
                (log/info (str "Move result type: " (type result)))
                (log/info (str "Move result: " result))
                (let [result-type (str (type result))]
                  (if (= result-type "class chariot.model.Fail")
                    (do
                      (log/warn (str "Move failed: " result " - resigning"))
                      (-> client .bot (.resign game-id)))
                    (log/info "Move submitted successfully")))))
            (do
              (log/warn "No move available - resigning")
              (-> client .bot (.resign game-id)))))
        (log/info "Not my turn - waiting")))
    (catch Exception e
      (log/error e "Error processing moves"))))

(defn handle-game-state-event [event game client fen-at-start moves-since-start]
  (try
    (let [event-type (-> event .getClass .getSimpleName)]
      (case event-type
        "Full"
        (do
          (log/info (str "FULL game state: " event))
          (reset! moves-since-start (-> event .state .moveList .size))
          (process-moves (.gameId game) (.color game) fen-at-start "" client))
        
        "State"
        (let [state event
              move-list (vec (.moveList state))
              moves-to-process (subvec move-list @moves-since-start)]
          (when (> (count moves-to-process) 0)
            (log/info (str "New moves: " moves-to-process)))
          
          (let [status-str (str (.status state))]
            (cond
              (not= status-str "started")
              (do
                (-> client .bot (.chat (.gameId game) "Thanks for the game!"))
                (log/info (str "Game ended with status: " status-str)))
              
              :else
              (process-moves (.gameId game) (.color game) fen-at-start 
                            (clojure.string/join " " moves-to-process) client))))
        
        "OpponentGone"
        (log/info (str "Opponent gone: " event))
        
        "Chat"
        (log/info (str "Chat message: " event))
        
        (log/info (str "Unknown event type: " event-type))))
    (catch Exception e
      (log/error e "Error handling game state event"))))

(defn handle-game [game client]
  (let [game-id (.gameId game)
        opponent-id (-> game .opponent .id)]
    (log/info (str "=== HANDLING GAME ==="))
    (log/info (str "Game ID: " game-id))
    (log/info (str "Color: " (.color game)))
    (log/info (str "Is my turn: " (.isMyTurn game)))
    (log/info (str "FEN: " (.fen game)))
    (.put games opponent-id game-id)
    (try
      (let [fen-at-start (.fen game)
            moves-since-start (atom 0)
            connect-result (-> client .bot (.connectToGame game-id))]
        (log/info (str "Game connect result type: " (type connect-result)))
        (if (= (str (type connect-result)) "class chariot.model.Entries")
          (with-open [stream (.stream connect-result)]
            (log/info "Game event stream opened")
            (let [iterator (.iterator stream)]
              (loop [event-count 0]
                (if (.hasNext iterator)
                  (let [event (.next iterator)]
                    (log/info (str "Game event #" (inc event-count) ": " (-> event .getClass .getSimpleName)))
                    (handle-game-state-event event game client fen-at-start moves-since-start)
                    (recur (inc event-count)))
                  (log/warn "Game event stream ended")))))
          (do
            (log/warn (str "Failed to connect to game " game-id ": " connect-result))
            (-> client .bot (.resign game-id)))))
      (catch Exception e
        (log/error e "Error in game handler"))
      (finally
        (.remove games opponent-id game-id)))))

(defn run-bot [client]
  (try
    (let [events (-> client .bot .connect)]
      (log/info "Connected to Lichess bot API")
      (log/info (str "Events result type: " (type events)))
      (if (= (str (type events)) "class chariot.model.Fail")
        (log/warn (str "Failed to connect: " events))
        (with-open [stream (.stream events)]
          (log/info "Event stream opened, waiting for events...")
          (let [start-time (System/currentTimeMillis)
                iterator (.iterator stream)]
            (loop [event-count 0]
              (if (.hasNext iterator)
                (let [event (.next iterator)
                      event-type (-> event .getClass .getSimpleName)
                      current-time (System/currentTimeMillis)]
                  (log/info (str "=== RECEIVED EVENT #" (inc event-count) " (after " 
                                (/ (- current-time start-time) 1000.0) "s): " event-type " ==="))
                  (log/info (str "Event details: " event))
                  (case event-type
                    "ChallengeCreatedEvent"
                    (do
                      (log/info "Processing ChallengeCreatedEvent")
                      (future (handle-challenge event client)))
                    
                    "GameStartEvent"
                    (do
                      (log/info "Processing GameStartEvent")
                      (future (handle-game (.game event) client)))
                    
                    ("GameStopEvent" "ChallengeCanceledEvent" "ChallengeDeclinedEvent")
                    (log/info (str "Game/Challenge event: " event))
                    
                    (log/info (str "Unknown event type: " event-type " - " event)))
                  (recur (inc event-count)))
                (do
                  (log/warn "Event stream ended or no more events")
                  (log/info (str "Total events processed: " event-count)))))))))
    (catch Exception e
      (log/error e "Error in bot runner"))))

(defn initialize-client []
  (try
    (if-not bot-token
      (do
        (log/error "BOT_TOKEN environment variable not set")
        nil)
      (let [lichess-api (or (System/getenv "LICHESS_API") "https://lichess.org")
            config-fn (reify java.util.function.Consumer
                        (accept [_ builder]
                          (.api builder (java.net.URI/create lichess-api))))
            client (Client/auth config-fn bot-token)]
        (log/info "Client created with token")
        client))
    (catch Exception e
      (log/error e "Failed to initialize client")
      nil)))

(defn initialize-account [client]
  (try
    (let [profile-result (-> client .account .profile)]
      (if (= (str (type profile-result)) "class chariot.model.Entry")
        (let [account (.entry profile-result)]
          (log/info (str "Account: " (.name account) " (title: " (.title account) ")"))
          account)
        (do
          (log/error (str "Failed to get account profile: " profile-result))
          nil)))
    (catch Exception e
      (log/error e "Failed to initialize account")
      nil)))

(defn -main [& args]
  (log/info "Starting Clojure Chess Bot...")
  (loop []
    (try
      (when-let [client (initialize-client)]
        (when-let [account (initialize-account client)]
          (log/info (str "Bot initialized for account: " (.name account)))
          (run-bot client)))
      (catch Exception e
        (log/error e "Error in main bot loop"))
      (finally
        (let [duration (Duration/ofSeconds 60)]
          (log/info (str "Retrying in " (.toSeconds duration) " seconds..."))
          (sleep-duration duration))))
    (recur)))