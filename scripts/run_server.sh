#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

KEYSTORE_PATH="assign2/keystore.jks"
STOREPASS="changeit"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "Keystore not found. Generating..."
  bash scripts/create_keystore.sh
fi

echo "Compiling Java sources (if needed)..."
rm -rf assign2/out || true
mkdir -p assign2/out
javac -d assign2/out $(find assign2/src -name "*.java")

echo "Starting server with keystore $KEYSTORE_PATH"
java -Djavax.net.ssl.keyStore=$KEYSTORE_PATH \
     -Djavax.net.ssl.keyStorePassword=$STOREPASS \
     -cp assign2/out server.ChatServer
