package education.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfraiLogsClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsDeclaredIngestFields() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        InfraiLogsClient client = client("/v1/logs/ingest", exchange ->
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));

        client.ingest(new ObjectMapper().createObjectNode().put("message", "overdue"), "delivery-1");

        JsonNode request = new ObjectMapper().readTree(body.get());
        assertEquals("overdue", request.path("entries").path(0).path("message").asText());
        assertEquals("delivery-1", request.path("idempotency_key").asText());
        assertEquals(2, request.size());
    }

    @Test
    void usesQForSearch() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        InfraiLogsClient client = client("/v1/logs/search", exchange -> query.set(exchange.getRequestURI().getRawQuery()));

        client.search("service:course delivery");

        assertTrue(query.get().startsWith("q="));
        assertFalse(query.get().contains("query="));
    }

    private InfraiLogsClient client(String path, RequestCapture capture) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            capture.accept(exchange);
            byte[] response = "{\"ok\":true,\"data\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        InfraiConfiguration configuration = new InfraiConfiguration(
                URI.create("http://localhost:" + server.getAddress().getPort()), "test-key", 1,
                Duration.ofSeconds(2));
        return new InfraiLogsClient(configuration, HttpClient.newHttpClient(), new ObjectMapper());
    }

    @FunctionalInterface
    private interface RequestCapture {
        void accept(HttpExchange exchange) throws IOException;
    }
}
