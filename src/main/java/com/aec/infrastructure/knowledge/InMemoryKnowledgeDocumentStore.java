package com.aec.infrastructure.knowledge;

import com.aec.application.port.KnowledgeDocumentStore;
import com.aec.domain.KnowledgeDocumentRecord;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryKnowledgeDocumentStore implements KnowledgeDocumentStore {

    private final Map<UUID, KnowledgeDocumentRecord> documents = new ConcurrentHashMap<>();

    @Override
    public UUID create(String title, String source, String content, Map<String, String> metadata) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        documents.put(id, new KnowledgeDocumentRecord(
                id,
                title,
                source,
                content,
                "queued",
                0,
                null,
                metadata,
                now,
                now));
        return id;
    }

    @Override
    public Optional<KnowledgeDocumentRecord> find(UUID id) {
        return Optional.ofNullable(documents.get(id));
    }

    @Override
    public void markProcessing(UUID id, UUID jobId) {
        documents.computeIfPresent(id, (key, existing) -> new KnowledgeDocumentRecord(
                existing.id(),
                existing.title(),
                existing.source(),
                existing.content(),
                "processing",
                existing.chunkCount(),
                jobId,
                existing.metadata(),
                existing.createdAt(),
                Instant.now()));
    }

    @Override
    public void markIndexed(UUID id, UUID jobId, int chunkCount) {
        documents.computeIfPresent(id, (key, existing) -> new KnowledgeDocumentRecord(
                existing.id(),
                existing.title(),
                existing.source(),
                existing.content(),
                "indexed",
                chunkCount,
                jobId,
                existing.metadata(),
                existing.createdAt(),
                Instant.now()));
    }

    @Override
    public void markFailed(UUID id, UUID jobId) {
        documents.computeIfPresent(id, (key, existing) -> new KnowledgeDocumentRecord(
                existing.id(),
                existing.title(),
                existing.source(),
                existing.content(),
                "failed",
                existing.chunkCount(),
                jobId,
                existing.metadata(),
                existing.createdAt(),
                Instant.now()));
    }
}
