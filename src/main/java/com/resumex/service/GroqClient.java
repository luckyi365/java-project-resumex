package com.resumex.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.resumex.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Ultra-fast Groq AI Client implementing {@link AiClient}.
 * Powered by Groq's LPU Inference Engine using models like llama-3.3-70b-versatile.
 */
public class GroqClient implements AiClient {

    private static final Logger LOG = Logger.getLogger(GroqClient.class.getName());
    private static final Gson GSON = new Gson();
    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final HttpClient client;

    public GroqClient() {
        this.apiKey = AppConfig.get("GROQ_API_KEY", "groq.api.key", "");
        this.model = AppConfig.get("GROQ_MODEL", "groq.model", "llama-3.3-70b-versatile");
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public GroqClient(String apiKey, String model) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.model = (model != null && !model.isBlank()) ? model : "llama-3.3-70b-versatile";
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public boolean hasValidKey() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("your_groq_api_key");
    }

    public String getModelName() {
        return model;
    }

    @Override
    public String generate(String systemPrompt, String userMessage) throws ResumeXException {
        if (!hasValidKey()) {
            throw new ResumeXException("Groq API Key is not configured. Please add groq.api.key in config.properties or Settings.");
        }
        return attemptCall(systemPrompt, userMessage, 0);
    }

    private String attemptCall(String systemPrompt, String userMessage, int attempt) throws ResumeXException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("temperature", 0.2);

        // Enforce JSON object output for deterministic ATS parsing
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        requestBody.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        
        // System prompt
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);
        }

        // User prompt
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        requestBody.add("messages", messages);

        String jsonPayload = GSON.toJson(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                return parseGroqResponse(response.body());
            } else if (statusCode == 429 && attempt < 2) {
                LOG.warning("Groq rate limit (429). Retrying in 1.5s...");
                Thread.sleep(1500);
                return attemptCall(systemPrompt, userMessage, attempt + 1);
            } else {
                String errorBody = response.body();
                LOG.severe("Groq API error (" + statusCode + "): " + errorBody);
                throw new ResumeXException("Groq API error (" + statusCode + "): " + extractErrorMessage(errorBody));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResumeXException("AI request was interrupted.", e);
        } catch (ResumeXException e) {
            throw e;
        } catch (Exception e) {
            if (attempt < 1) {
                LOG.warning("Groq network glitch, retrying: " + e.getMessage());
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                return attemptCall(systemPrompt, userMessage, attempt + 1);
            }
            throw new ResumeXException("Failed to connect to Groq AI service: " + e.getMessage(), e);
        }
    }

    private String parseGroqResponse(String responseBody) throws ResumeXException {
        try {
            JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ResumeXException("Groq returned empty choices in response.");
            }
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.getAsJsonObject("message");
            return message.get("content").getAsString();
        } catch (Exception e) {
            throw new ResumeXException("Failed to parse Groq response JSON: " + responseBody, e);
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject err = root.getAsJsonObject("error");
                if (err.has("message")) return err.get("message").getAsString();
            }
        } catch (Exception ignored) {}
        return responseBody;
    }
}

