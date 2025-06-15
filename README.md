# Clojure Chess Bot

A basic chess bot implementation in Clojure that runs on Lichess.

Challange him here https://lichess.org/@/clj-bot ...hes pretty bad for now...

## Running the Bot yourself

Set your Lichess bot token and run:

```bash
export BOT_TOKEN=your_lichess_bot_token
clj -M:run
```

The bot will connect to Lichess and wait for challenges.

## Requirements

- Clojure CLI tools
- Lichess BOT account with API token

## Features

- Accepts challenges automatically
- Handles basic game events
- Auto-reconnects on failures
