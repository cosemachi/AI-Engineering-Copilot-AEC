package com.aec.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record KnowledgeDocumentRecord(
        UUID id,
        String title,
        String source,
        String content,
        String ingestionStatus,
        int chunkCount,
        UUID latestJobId,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt) {
}
