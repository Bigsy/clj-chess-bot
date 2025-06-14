# Clojure Chess Bot

A basic chess bot implementation in Clojure that runs on Lichess.

Come challange him here https://lichess.org/@/clj-bot but he's pretty rubbish atm.

## Running the Bot yourself

Set your Lichess bot token and run:

```bash
export BOT_TOKEN=your_lichess_bot_token
clj -M:run
```

The bot will connect to Lichess and wait for challenges. It plays by making random legal moves for now.

## Requirements

- Clojure CLI tools
- Lichess BOT account with API token

## Features

- Accepts challenges automatically
- Makes random legal moves
- Handles basic game events
- Auto-reconnects on failures