package com.aec.domain;

import java.util.UUID;

public record KnowledgeIngestResult(UUID documentId, UUID jobId, String status) {
}
