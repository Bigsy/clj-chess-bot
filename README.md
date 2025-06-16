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

## Running with Docker

### Pull from GitHub Container Registry

```bash
docker pull ghcr.io/[your-username]/clj-chess-bot:latest
docker run -e BOT_TOKEN=your_lichess_bot_token ghcr.io/[your-username]/clj-chess-bot:latest
```

### Build locally

```bash
docker build -t clj-chess-bot .
docker run -e BOT_TOKEN=your_lichess_bot_token clj-chess-bot
```

### GitHub Actions

The repository includes a GitHub Actions workflow that automatically builds and pushes Docker images to GitHub Container Registry (ghcr.io) on:
- Push to main branch
- Git tags (v*)
- Pull requests (build only, no push)

To enable this:
1. Go to Settings → Actions → General → Workflow permissions
2. Select "Read and write permissions"
3. Check "Allow GitHub Actions to create and approve pull requests"
