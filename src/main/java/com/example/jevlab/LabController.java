package com.example.jevlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class LabController {
    private final JevClient client;

    public LabController(JevClient client) { this.client = client; }

    @PostMapping({"/decisions", "/playground"})
    public JsonNode decide(@RequestBody ObjectNode request) {
        validate(request);
        return client.evaluate(request);
    }

    static void validate(ObjectNode request) {
        JsonNode state = request.path("state"), questions = request.path("questions");
        if (!(state.isTextual() || state.isObject() || state.isArray()) || !questions.isObject() || questions.isEmpty()) bad("Supply state and at least one named question.");
        if (request.toString().length() > 2_000_000) bad("Learning lab requests are limited to two million characters.");
        for (JsonNode q : questions) {
            switch (q.path("type").asText()) {
                case "choice" -> { if (!q.path("criteria").isObject() || q.path("criteria").isEmpty()) bad("Choice criteria must be a nonempty object."); }
                case "score" -> { if (!q.path("criteria").isArray() || q.path("criteria").isEmpty() || q.path("criteria").size() > 10) bad("Score criteria must have 1 to 10 ordered levels."); }
                case "noul", "box" -> { }
                default -> bad("Supported types: choice, score, noul, box.");
            }
        }
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}

