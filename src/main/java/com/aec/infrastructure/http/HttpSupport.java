package com.aec.infrastructure.http;

import com.aec.application.AecException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HttpSupport {

    public String send(java.net.http.HttpClient client, HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new AecException("HTTP " + response.statusCode() + " calling " + request.uri() + ": " + response.body());
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AecException("HTTP request failed for " + request.uri(), e);
        }
    }

    public HttpRequest.Builder jsonRequest(URI uri, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "aec");
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }
}
