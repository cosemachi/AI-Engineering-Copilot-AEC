package com.aec.infrastructure.pr;

import com.aec.application.AecException;
import com.aec.application.port.PullRequestGateway;
import com.aec.domain.ChangedFile;
import com.aec.domain.PullRequestData;
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
public class GitHubPullRequestGateway implements PullRequestGateway {

    private final ObjectMapper objectMapper;
    private final HttpSupport httpSupport;
    private final HttpClient httpClient;
    private final String apiBase;
    private final String token;

    public GitHubPullRequestGateway(
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
    public PullRequestData fetch(String owner, String repo, int number) {
        URI prUri = URI.create("%s/repos/%s/%s/pulls/%d".formatted(apiBase, owner, repo, number));
        URI filesUri = URI.create("%s/repos/%s/%s/pulls/%d/files".formatted(apiBase, owner, repo, number));

        HttpRequest prRequest = httpSupport.jsonRequest(prUri, token).GET().build();
        HttpRequest filesRequest = httpSupport.jsonRequest(filesUri, token).GET().build();

        try {
            JsonNode pr = objectMapper.readTree(httpSupport.send(httpClient, prRequest));
            JsonNode files = objectMapper.readTree(httpSupport.send(httpClient, filesRequest));
            List<ChangedFile> changedFiles = new ArrayList<>();
            files.forEach(file -> changedFiles.add(new ChangedFile(
                    file.path("filename").asText(),
                    file.path("status").asText(),
                    file.path("additions").asInt(),
                    file.path("deletions").asInt(),
                    file.path("patch").asText(""))));
            return new PullRequestData(
                    pr.path("number").asText(),
                    pr.path("title").asText(),
                    pr.path("body").asText(""),
                    pr.path("user").path("login").asText(""),
                    pr.path("base").path("ref").asText(""),
                    pr.path("head").path("ref").asText(""),
                    changedFiles);
        } catch (IOException e) {
            throw new AecException("Failed to parse GitHub pull request response", e);
        }
    }
}
