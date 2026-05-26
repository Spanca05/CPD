#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

TRUSTSTORE_PATH="assign2/keystore.jks"
STOREPASS="changeit"

if [ ! -f "$TRUSTSTORE_PATH" ]; then
  echo "Truststore not found. Generating (creates keystore and uses it as truststore)..."
  bash scripts/create_keystore.sh
fi

echo "Compiling Java sources (if needed)..."
rm -rf assign2/out || true
mkdir -p assign2/out
javac -d assign2/out $(find assign2/src -name "*.java")

echo "Starting client with truststore $TRUSTSTORE_PATH"
java -Djavax.net.ssl.trustStore=$TRUSTSTORE_PATH \
     -Djavax.net.ssl.trustStorePassword=$STOREPASS \
     -cp assign2/out client.ChatClient
