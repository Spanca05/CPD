#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

TRUSTSTORE_PATH="$ROOT_DIR/keystore.jks"
STOREPASS="changeit"

if [ ! -f "$TRUSTSTORE_PATH" ]; then
  echo "Truststore not found. Generating (creates keystore and uses it as truststore)..."
  bash "$SCRIPT_DIR/create_keystore.sh"
fi

echo "Compiling Java sources (if needed)..."
rm -rf out || true
mkdir -p out
javac -d out $(find src -name "*.java")

echo "Starting client with truststore $TRUSTSTORE_PATH"
java -Djavax.net.ssl.trustStore="$TRUSTSTORE_PATH" \
  -Djavax.net.ssl.trustStorePassword="$STOREPASS" \
  -cp out client.ChatClient "$@"
