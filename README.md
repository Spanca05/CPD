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

# CPD Projects

CPD Projects of group T&lt;11&gt;G&lt;13&gt;.

Group members:

1. &lt;Vasco&gt; &lt;Vieira&gt; (&lt;up202307797@up.pt&gt;)
2. &lt;Leonardo&gt; &lt;Carvalho&gt; (&lt;up202307152@up.pt&gt;)
