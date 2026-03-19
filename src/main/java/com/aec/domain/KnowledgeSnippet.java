package com.aec.domain;

public record KnowledgeSnippet(
        String title,
        String source,
        String excerpt,
        double score) {
}
