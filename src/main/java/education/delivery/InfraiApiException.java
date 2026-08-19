package education.delivery;

import com.fasterxml.jackson.databind.JsonNode;

public final class InfraiApiException extends RuntimeException {
    private final int statusCode;
    private final JsonNode error;

    public InfraiApiException(int statusCode, JsonNode error) {
        super(error.path("code").asText("INFRAI_REQUEST_REJECTED") + ": "
                + error.path("message").asText("Request rejected"));
        this.statusCode = statusCode;
        this.error = error.deepCopy();
    }

    public int statusCode() {
        return statusCode;
    }

    public JsonNode error() {
        return error.deepCopy();
    }
}
