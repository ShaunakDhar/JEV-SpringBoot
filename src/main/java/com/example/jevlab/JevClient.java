package com.example.jevlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.springframework.http.HttpStatus.*;

/** Plain HTTP integration keeps the exact TypeSafe wire format visible to learners. */
@Component
public class JevClient {
    private final ObjectMapper mapper;
    private final String key, model;
    private final URI endpoint;
    private final boolean images, longContext;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public JevClient(ObjectMapper mapper, @Value("${jev.api-key}") String key, @Value("${jev.base-url}") String base,
                     @Value("${jev.model}") String model, @Value("${jev.images-enabled}") boolean images,
                     @Value("${jev.long-context-enabled}") boolean longContext) {
        this.mapper = mapper; this.key = key; this.model = model;
        this.images = images; this.longContext = longContext;
        endpoint = URI.create(base.replaceAll("/+$", "") + "/v1/systemone");
    }

    public String model() { return model; }
    public boolean imagesEnabled() { return images; }

    public JsonNode evaluate(ObjectNode body) {
        if (key.isBlank()) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Set TYPESAFE_API_KEY in .env or your environment.");
        boolean vision = body.has("assets");
        for (JsonNode q : body.path("questions")) vision |= q.path("type").asText().equals("box");
        if (vision && !images) throw new ResponseStatusException(CONFLICT, "Experimental images require JEV_IMAGES_ENABLED=true.");
        ObjectNode request = body.deepCopy();
        request.put("model", model);
        try {
            var builder = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json");
            if (vision) builder.header("TypeSafe-Danger-Zone-Images", "true");
            if (longContext) builder.header("TypeSafe-Danger-Zone-Long-Context", "true");
            var response = http.send(builder.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request))).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new ResponseStatusException(TOO_MANY_REQUESTS, "TypeSafe rate limit reached. Wait before retrying.");
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new ResponseStatusException(BAD_GATEWAY, "TypeSafe returned HTTP " + response.statusCode() + ". Check your API key, account access and request format.");
            JsonNode result = mapper.readTree(response.body());
            if (result == null || !result.path("answers").isObject()) throw new IOException("Missing answers");
            validateAnswers(request.path("questions"), result.path("answers"));
            return result;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "TypeSafe did not respond within the time limit.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Request interrupted.");
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "Could not read a valid TypeSafe response.");
        }
    }

    static void validateAnswers(JsonNode questions, JsonNode answers) throws IOException {
        var fields = questions.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            JsonNode a = answers.path(entry.getKey());
            String type = entry.getValue().path("type").asText();
            if (!a.path("type").asText().equals(type)) throw new IOException("Answer type mismatch");
            switch (type) {
                case "choice" -> {
                    if (!a.path("choice").isTextual() || !entry.getValue().path("criteria").has(a.path("choice").asText())) throw new IOException("Invalid choice");
                    unit(a.path("confidence"));
                }
                case "score" -> {
                    if (!a.path("score").isNumber() || a.path("score").asDouble() < 0 || a.path("score").asDouble() > entry.getValue().path("criteria").size()-1) throw new IOException("Invalid score");
                    unit(a.path("confidence"));
                }
                case "noul" -> unit(a.path("noul"));
                case "box" -> {
                    for (String edge : new String[]{"top", "bottom", "left", "right"}) unit(a.path("box").path(edge));
                    if (a.path("box").path("top").asDouble() > a.path("box").path("bottom").asDouble() || a.path("box").path("left").asDouble() > a.path("box").path("right").asDouble()) throw new IOException("Invalid box");
                    if (!a.path("confidence").isNull()) unit(a.path("confidence"));
                }
                default -> throw new IOException("Unknown type");
            }
        }
    }

    private static void unit(JsonNode n) throws IOException {
        if (!n.isNumber() || !Double.isFinite(n.asDouble()) || n.asDouble() < 0 || n.asDouble() > 1) throw new IOException("Expected a probability");
    }
}
