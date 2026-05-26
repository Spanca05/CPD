#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

KEYSTORE_PATH="$ROOT_DIR/assign2/keystore.jks"
STOREPASS="changeit"
KEYPASS="changeit"

if [ -f "$KEYSTORE_PATH" ]; then
  echo "Keystore already exists at $KEYSTORE_PATH"
  exit 0
fi

mkdir -p "$(dirname "$KEYSTORE_PATH")"

echo "Generating self-signed keystore at $KEYSTORE_PATH"

keytool -genkeypair \
  -alias server \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -keystore "$KEYSTORE_PATH" \
  -storepass "$STOREPASS" \
  -keypass "$KEYPASS" \
  -dname "CN=localhost, OU=CPD, O=University, L=City, ST=State, C=GB" \
  -storetype JKS

echo "Keystore created: $KEYSTORE_PATH (password: $STOREPASS)"
