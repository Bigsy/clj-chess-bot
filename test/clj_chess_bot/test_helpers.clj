(ns clj-chess-bot.test-helpers
  (:import [chariot.model ChallengeInfo ChallengeInfo$Players ChallengeInfo$From 
            ChallengeInfo$OpenEnded ChallengeInfo$Player UserInfo GameType TimeControl
            Event Event$ChallengeCreatedEvent Challenge Variant$Basic RealTime Clock Unlimited
            Enums$Speed Opt ChallengeInfo$ColorRequest Enums$ColorPref]
           [java.util Optional]
           [java.time Duration]
           [java.net URI]))

(defn make-user-info [name]
  (UserInfo/of (str "id-" name) name))

(defn make-player [user-name]
  (ChallengeInfo$Player. 
    (make-user-info user-name)
    1500
    false
    true
    nil))

(defn make-time-control []
  (RealTime. 
    (Clock. (Duration/ofMinutes 5) (Duration/ofSeconds 0))
    "5+0"
    Enums$Speed/blitz))

(defn make-variant []
  Variant$Basic/standard)

(defn make-game-type [rated]
  (GameType. rated (make-variant) (make-time-control)))

(defn make-challenge-info 
  ([id rated]
   (make-challenge-info id rated true))
  ([id rated has-challenger?]
   (let [players (if has-challenger?
                   (ChallengeInfo$From. (make-player "test-challenger"))
                   (ChallengeInfo$OpenEnded.))]
     (ChallengeInfo. 
       id
       (Opt/empty)
       (Opt/empty)
       (URI/create (str "https://lichess.org/" id))
       players
       (make-game-type rated)
       (ChallengeInfo$ColorRequest. Enums$ColorPref/random)
       []))))

(defn make-challenge-event 
  ([id rated]
   (make-challenge-event id rated true false))
  ([id rated has-challenger? is-rematch?]
   (Event$ChallengeCreatedEvent.
     (make-challenge-info id rated has-challenger?)
     (if is-rematch? (Opt/of "rematch-id") (Opt/empty))
     nil)))

(defn make-mock-client []
  (let [calls (atom [])]
    {:client nil ; We'll use individual mocks in tests
     :calls calls}))