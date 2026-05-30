# CPD Projects

CPD Projects of group T&lt;11&gt;G&lt;13&gt;.

Group members:

1. &lt;Vasco&gt; &lt;Vieira&gt; (&lt;up202307797@up.pt&gt;)
2. &lt;Leonardo&gt; &lt;Carvalho&gt; (&lt;up202307152@up.pt&gt;)

## Run server and client for Project 2 (local testing)

You can generate a self-signed keystore and run the server/client using the helper scripts in `src/scripts/`.

- Create keystore (if not present):

```bash
bash scripts/create_keystore.sh
```

- Start server (compiles sources then runs):

```bash
bash scripts/run_server.sh
```

- Start client (compiles sources then runs):

```bash
bash scripts/run_client.sh 192.168.1.10
```

The scripts create `assign2/keystore.jks` (password `changeit`) and use it for both server keystore and client truststore for local testing.

## Run across 2 PCs

1. On PC 1, start the server:

```bash
cd assign2
bash scripts/run_server.sh
```

2. Find the LAN IP of PC 1, for example with `ip addr`.

3. Make sure PC 1 allows inbound traffic on port `12345`.

4. On PC 2, run the client and pass PC 1's IP address:

```bash
cd assign2
bash scripts/run_client.sh <PC1_IP>

