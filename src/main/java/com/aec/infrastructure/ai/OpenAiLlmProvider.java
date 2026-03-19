package com.aec.infrastructure.ai;

import com.aec.application.AecException;
import com.aec.application.port.LlmProvider;
import com.aec.domain.PrReviewResult;
import com.aec.domain.PullRequestData;
import com.aec.domain.Ticket;
import com.aec.domain.TicketAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OpenAiLlmProvider implements LlmProvider {

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient client;

    public OpenAiLlmProvider(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "aec.openai.base-url") String baseUrl,
            @ConfigProperty(name = "aec.openai.api-key") Optional<String> apiKey,
            @ConfigProperty(name = "aec.openai.chat-model") String model) {
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
    public TicketAnalysis analyzeTicket(Ticket ticket) {
        String prompt = """
                Analyze the engineering ticket and return JSON with keys:
                summary, risks, missing_info, dependencies.

                Ticket:
                %s
                """.formatted(writeJson(ticket));
        return readJson(runPrompt(prompt), TicketAnalysis.class);
    }

    @Override
    public PrReviewResult reviewPullRequest(PullRequestData pullRequestData) {
        String prompt = """
                Review the pull request diff and return JSON with keys:
                summary, issues, suggestions.
                issues must be an array of objects with type in [bug, performance, design] and description.

                Pull request:
                %s
                """.formatted(writeJson(pullRequestData));
        return readJson(runPrompt(prompt), PrReviewResult.class);
    }

    @Override
    public String answerKnowledgeQuery(String query, List<String> supportingDocuments) {
        String prompt = """
                Answer the engineering knowledge query using the provided excerpts.
                Query: %s
                Excerpts:
                %s
                """.formatted(query, supportingDocuments);
        return runPrompt(prompt);
    }

    private String runPrompt(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AecException("OPENAI_API_KEY is required when aec.ai.provider=openai");
        }

        try {
            String payload = objectMapper.createObjectNode()
                    .put("model", model)
                    .putArray("messages")
                    .add(objectMapper.createObjectNode()
                            .put("role", "system")
                            .put("content", "You are an engineering copilot. Return only the requested output."))
                    .add(objectMapper.createObjectNode()
                            .put("role", "user")
                            .put("content", prompt))
                    .toString();

            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AecException("OpenAI call failed: " + response.body());
            }
            JsonNode jsonNode = objectMapper.readTree(response.body());
            return jsonNode.path("choices").path(0).path("message").path("content").asText();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AecException("OpenAI call failed", e);
        }
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (IOException e) {
            throw new AecException("Failed to serialize prompt payload", e);
        }
    }

    private <T> T readJson(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (IOException e) {
            throw new AecException("Failed to parse LLM JSON response", e);
        }
    }
}
