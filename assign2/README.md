
# Secure Chat (assign2)

This folder contains the Secure Chat server and client for the CPD assignment.

Prerequisites
- Java 21 or newer installed and on `PATH`.
- `keytool` (part of the JDK) for keystore operations.

Quick start
1. Generate a self-signed keystore (script will create `keystore.jks`):

```bash
cd assign2
bash scripts/create_keystore.sh
```

2. Start the server (script compiles and runs with the keystore):

```bash
cd assign2
bash scripts/run_server.sh
```

3. Start a client (script compiles and runs and uses the keystore as a truststore):

```bash
cd assign2
bash scripts/run_client.sh [server-host] [port]
```

By default the server listens on port `12345`. If you run the client on the same machine you can omit `server-host` (defaults to `localhost`).

Manual compile & run
- Compile all sources into `out` (scripts do this automatically):

```bash
cd assign2
rm -rf out || true
mkdir -p out
javac -d out $(find src -name "*.java")
```

- Run the server (provide keystore JVM props):

```bash
java -Djavax.net.ssl.keyStore="$PWD/keystore.jks" \
     -Djavax.net.ssl.keyStorePassword=changeit \
     -cp out server.ChatServer
```

- Run the client (provide truststore JVM props):

```bash
java -Djavax.net.ssl.trustStore="$PWD/keystore.jks" \
     -Djavax.net.ssl.trustStorePassword=changeit \
     -Dcpd.clientId="myclient" \
     -cp out client.ChatClient <server-host> <port>
```


Keystore / truststore notes
- The server requires a keystore and the JVM properties `javax.net.ssl.keyStore` and `javax.net.ssl.keyStorePassword`. The run script sets both.
- The client needs a truststore that contains the server certificate. The provided scripts simplify this by using the same `keystore.jks` as the truststore.
- Password used by the scripts is `changeit` (you can change it in scripts/create_keystore.sh).


How to run the server and client on two different PCs
Scenario: PC A will run the server, PC B will run the client.

1. On PC A (server) generate the keystore and start the server:

```bash
cd assign2
bash scripts/create_keystore.sh
bash scripts/run_server.sh
```

2. Determine the server's reachable IP address (e.g., `192.168.1.42`). To find the correct IP:

```bash
ip addr show
```

3. Ensure port `12345` is reachable (open firewall/port forwarding as needed).

```bash
sudo ufw allow 12345/tcp
sudo ufw reload
sudo ufw status
```

4. Copy the server certificate or the keystore to PC B (client).

5. On PC 2, run the client and pass PC 1's IP address: 

```bash
bash scripts/run_client.sh <PC1_IP>
```
