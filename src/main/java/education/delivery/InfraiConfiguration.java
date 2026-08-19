package education.delivery;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;

public record InfraiConfiguration(URI baseUrl, String apiKey, int maxAttempts, Duration requestTimeout) {
    public static InfraiConfiguration load() {
        Properties properties = new Properties();
        try (InputStream input = InfraiConfiguration.class.getResourceAsStream("/application.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read application.properties", exception);
        }

        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the example");
        }
        String baseUrl = System.getenv().getOrDefault(
                "INFRAI_BASE_URL", properties.getProperty("infrai.base-url"));
        int attempts = Integer.parseInt(System.getenv().getOrDefault(
                "INFRAI_MAX_ATTEMPTS", properties.getProperty("infrai.max-attempts", "4")));
        return new InfraiConfiguration(URI.create(baseUrl), apiKey, attempts, Duration.ofSeconds(20));
    }
}
