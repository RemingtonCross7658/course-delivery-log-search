package education.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class InfraiLogsClient {
    // Canonical calls: POST /v1/logs/ingest and GET /v1/logs/search.
    private static final String INGEST_PATH = "/v1/logs/ingest";
    private static final String SEARCH_PATH = "/v1/logs/search";

    private final InfraiConfiguration configuration;
    private final HttpClient httpClient;
    private final ObjectMapper json;

    public InfraiLogsClient(InfraiConfiguration configuration) {
        this(configuration, HttpClient.newBuilder()
                .connectTimeout(configuration.requestTimeout()).build(), new ObjectMapper());
    }

    InfraiLogsClient(InfraiConfiguration configuration, HttpClient httpClient, ObjectMapper json) {
        this.configuration = Objects.requireNonNull(configuration);
        this.httpClient = Objects.requireNonNull(httpClient);
        this.json = Objects.requireNonNull(json);
    }

    public JsonNode ingest(ObjectNode structuredLog, String deliveryId) {
        ObjectNode body = json.createObjectNode();
        body.putArray("entries").add(structuredLog);
        body.put("idempotency_key", deliveryId);
        HttpRequest request = requestBuilder(INGEST_PATH)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(writeJson(body)))
                .build();
        return send(request);
    }

    public JsonNode search(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = requestBuilder(SEARCH_PATH + "?q=" + encoded)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        return send(request);
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(configuration.baseUrl().resolve(path))
                .timeout(configuration.requestTimeout())
                .header("Authorization", "Bearer " + configuration.apiKey())
                .header("Accept", "application/json");
    }

    private JsonNode send(HttpRequest request) {
        for (int attempt = 1; attempt <= configuration.maxAttempts(); attempt++) {
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                throw new IllegalStateException("Infrai transport failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", exception);
            }

            JsonNode envelope = readEnvelope(response.body());
            if (response.statusCode() == 429 && attempt < configuration.maxAttempts()) {
                sleep(backoff(response, attempt));
                continue;
            }
            if (!envelope.path("ok").asBoolean(false)) {
                throw new InfraiApiException(response.statusCode(), envelope.path("error"));
            }
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Infrai transport status " + response.statusCode());
            }
            return envelope.path("data");
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    private JsonNode readEnvelope(String body) {
        try {
            return json.readTree(body);
        } catch (IOException exception) {
            throw new IllegalStateException("Infrai returned an unreadable envelope", exception);
        }
    }

    private Duration backoff(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(value -> Duration.ofSeconds(Long.parseLong(value)))
                .orElseGet(() -> Duration.ofMillis(250L * (1L << (attempt - 1))));
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", exception);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return json.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Structured log could not be encoded", exception);
        }
    }
}
