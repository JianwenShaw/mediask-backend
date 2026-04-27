#!/bin/sh

set -eu

echo "[mediask] resetting local postgres and redis volumes"
docker compose down -v

echo "[mediask] starting local infrastructure"
docker compose up -d

echo "[mediask] local database reset completed"
