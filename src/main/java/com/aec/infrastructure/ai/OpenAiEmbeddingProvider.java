package com.aec.infrastructure.ai;

import com.aec.application.AecException;
import com.aec.application.port.EmbeddingProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient client;

    public OpenAiEmbeddingProvider(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "aec.openai.base-url") String baseUrl,
            @ConfigProperty(name = "aec.openai.api-key") Optional<String> apiKey,
            @ConfigProperty(name = "aec.openai.embedding-model") String model) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey.orElse("");
        this.model = model;
        this.client = HttpClient.newHttpClient();
    }

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public List<Double> embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AecException("OPENAI_API_KEY is required when aec.embedding.provider=openai");
        }

        try {
            String payload = objectMapper.createObjectNode()
                    .put("model", model)
                    .put("input", text)
                    .toString();

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/embeddings"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AecException("OpenAI embedding call failed: " + response.body());
            }

            JsonNode embeddingNode = objectMapper.readTree(response.body()).path("data").path(0).path("embedding");
            List<Double> embedding = new ArrayList<>();
            embeddingNode.forEach(value -> embedding.add(value.asDouble()));
            return embedding;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AecException("OpenAI embedding call failed", e);
        }
    }
}
