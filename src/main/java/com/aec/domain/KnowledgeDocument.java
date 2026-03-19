package com.aec.domain;

import java.util.List;
import java.util.UUID;

public record KnowledgeDocument(
        UUID id,
        String title,
        String source,
        String content,
        List<Double> embedding) {
}
