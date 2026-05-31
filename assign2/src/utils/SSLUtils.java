package utils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

public class SSLUtils {

    public static ServerSocket createServerSocket(int port) throws Exception {
        try {
            SSLContext context = createServerContext();
            SSLServerSocketFactory ssf = context.getServerSocketFactory();
            return ssf.createServerSocket(port);
        } catch (Exception e) {
            throw new Exception("Failed to create SSL server socket.\n" +
                    "Make sure you provided a server keystore via JVM properties:\n" +
                    "-Djavax.net.ssl.keyStore=/path/to/keystore.jks \n" +
                    "-Djavax.net.ssl.keyStorePassword=changeit\n" +
                    "You can create a self-signed keystore using keytool. Original error: " + e.getMessage(), e);
        }
    }

    private static SSLContext createServerContext() throws Exception {
        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");
        String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
        String keyStoreType = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());

        if (keyStorePath == null || keyStorePath.isBlank()) {
            throw new Exception("Missing JVM property javax.net.ssl.keyStore");
        }
        if (keyStorePassword == null) {
            throw new Exception("Missing JVM property javax.net.ssl.keyStorePassword");
        }

        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (FileInputStream in = new FileInputStream(keyStorePath)) {
            keyStore.load(in, keyStorePassword.toCharArray());
        }

        String keyPassword = System.getProperty("javax.net.ssl.keyPassword", keyStorePassword);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPassword.toCharArray());

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, null);
        return context;
    }

    public static Socket createClientSocket(String host, int port) throws Exception {
        try {
            SSLContext context = createClientContext();
            SSLSocketFactory sf = context.getSocketFactory();
            return sf.createSocket(host, port);
        } catch (Exception e) {
            throw new Exception("Failed to create SSL client socket.\n" +
                    "Set a truststore that contains the server certificate, for example:\n" +
                    "-Djavax.net.ssl.trustStore=/path/to/keystore.jks\n" +
                    "-Djavax.net.ssl.trustStorePassword=changeit\n" +
                    "If you used a self-signed certificate for the server, the truststore can be the same JKS file.\n" +
                    "Original error: " + e.getMessage(), e);
        }
    }

    private static SSLContext createClientContext() throws Exception {
        String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());

        if (trustStorePath == null || trustStorePath.isBlank()) {
            trustStorePath = System.getProperty("javax.net.ssl.keyStore");
            trustStorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
        }

        if (trustStorePath == null || trustStorePath.isBlank()) {
            throw new Exception("Missing JVM property javax.net.ssl.trustStore (or javax.net.ssl.keyStore as fallback)");
        }
        if (trustStorePassword == null) {
            throw new Exception("Missing JVM property javax.net.ssl.trustStorePassword (or javax.net.ssl.keyStorePassword as fallback)");
        }

        KeyStore trustStore = KeyStore.getInstance(trustStoreType);
        try (FileInputStream in = new FileInputStream(trustStorePath)) {
            trustStore.load(in, trustStorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
        return context;
    }
}
