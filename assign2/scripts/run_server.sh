#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

KEYSTORE_PATH="$ROOT_DIR/keystore.jks"
STOREPASS="changeit"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "Keystore not found. Generating..."
  bash "$SCRIPT_DIR/create_keystore.sh"
fi

echo "Compiling Java sources (if needed)..."
rm -rf out || true
mkdir -p out
javac -d out $(find src -name "*.java")

echo "Starting server with keystore $KEYSTORE_PATH"
java -Djavax.net.ssl.keyStore="$KEYSTORE_PATH" \
  -Djavax.net.ssl.keyStorePassword="$STOREPASS" \
  -cp out server.ChatServer
