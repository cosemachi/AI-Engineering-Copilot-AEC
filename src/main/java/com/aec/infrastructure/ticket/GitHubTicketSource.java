package com.aec.infrastructure.ticket;

import com.aec.application.AecException;
import com.aec.application.port.TicketSource;
import com.aec.domain.Ticket;
import com.aec.infrastructure.http.HttpSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GitHubTicketSource implements TicketSource {

    private final ObjectMapper objectMapper;
    private final HttpSupport httpSupport;
    private final HttpClient httpClient;
    private final String apiBase;
    private final String token;

    public GitHubTicketSource(
            ObjectMapper objectMapper,
            HttpSupport httpSupport,
            @ConfigProperty(name = "aec.github.api-base") String apiBase,
            @ConfigProperty(name = "aec.github.token") Optional<String> token) {
        this.objectMapper = objectMapper;
        this.httpSupport = httpSupport;
        this.apiBase = apiBase;
        this.token = token.orElse("");
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public String sourceName() {
        return "github";
    }

    @Override
    public Ticket fetch(String identifier) {
        String[] parts = identifier.split("#");
        if (parts.length != 2 || !parts[0].contains("/")) {
            throw new AecException("GitHub ticket id must look like owner/repo#123");
        }

        String[] repoParts = parts[0].split("/");
        URI issueUri = URI.create("%s/repos/%s/%s/issues/%s".formatted(apiBase, repoParts[0], repoParts[1], parts[1]));
        URI commentsUri = URI.create("%s/repos/%s/%s/issues/%s/comments".formatted(apiBase, repoParts[0], repoParts[1], parts[1]));

        HttpRequest issueRequest = httpSupport.jsonRequest(issueUri, token).GET().build();
        HttpRequest commentsRequest = httpSupport.jsonRequest(commentsUri, token).GET().build();

        try {
            JsonNode issue = objectMapper.readTree(httpSupport.send(httpClient, issueRequest));
            JsonNode comments = objectMapper.readTree(httpSupport.send(httpClient, commentsRequest));
            List<String> commentBodies = new ArrayList<>();
            comments.forEach(comment -> commentBodies.add(comment.path("body").asText("")));
            return new Ticket(
                    issue.path("number").asText(),
                    issue.path("title").asText(),
                    issue.path("body").asText(""),
                    commentBodies,
                    "github");
        } catch (IOException e) {
            throw new AecException("Failed to parse GitHub issue response", e);
        }
    }
}
