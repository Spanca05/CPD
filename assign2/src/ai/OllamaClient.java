package ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;


public class OllamaClient {
    private final HttpClient http;
    private final String endpointUrl;
    private final String model;
    private final Duration timeout;

    public OllamaClient(String endpointUrl, String model, long timeoutMs) {
        this.http = HttpClient.newBuilder().build();
        this.endpointUrl = endpointUrl;
        this.model = model;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    public static OllamaClient createDefault() {
        return new OllamaClient("http://localhost:11434/api/generate", "llama3", 60_000L);
    }

    public CompletableFuture<String> generate(String prompt) {
        String json = buildRequestBody(prompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(json))
                .build();

        return http.sendAsync(req, BodyHandlers.ofString())
                .thenApply(resp -> {
                    int status = resp.statusCode();
                    String body = resp.body();
                    if (status >= 200 && status < 300) {
                        return body;
                    }
                    throw new RuntimeException("LLM request failed: " + status + " - " + body);
                });
    }

    public String generateSync(String prompt) throws Exception {
        try {
            return generate(prompt).get();
        } catch (Exception e) {
            throw new Exception("LLM generate failed", e);
        }
    }

    private String buildRequestBody(String prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');

        if (model != null && !model.isBlank()) {
            sb.append("\"model\":\"").append(escapeJson(model)).append('\"');
            sb.append(',');
        }

        sb.append("\"prompt\":\"").append(escapeJson(prompt)).append('\"');

        sb.append(',');
        sb.append("\"stream\": false");

        sb.append('}');
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
