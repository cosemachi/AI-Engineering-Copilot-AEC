package com.aec.domain;

import java.time.Instant;
import java.util.UUID;

public record IngestionJob(
        UUID id,
        String jobType,
        UUID targetId,
        String status,
        int attemptCount,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {
}
