package com.aec.application.port;

import com.aec.domain.KnowledgeDocumentRecord;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentStore {
    UUID create(String title, String source, String content, Map<String, String> metadata);
    Optional<KnowledgeDocumentRecord> find(UUID id);
    void markProcessing(UUID id, UUID jobId);
    void markIndexed(UUID id, UUID jobId, int chunkCount);
    void markFailed(UUID id, UUID jobId);
}
