package com.aec.infrastructure.knowledge;

import com.aec.application.port.IngestionJobStore;
import com.aec.domain.IngestionJob;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class InMemoryIngestionJobStore implements IngestionJobStore {

    private final Map<UUID, IngestionJob> jobs = new ConcurrentHashMap<>();

    @Override
    public UUID create(String jobType, UUID targetId) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jobs.put(id, new IngestionJob(id, jobType, targetId, "queued", 0, null, now, now));
        return id;
    }

    @Override
    public Optional<IngestionJob> find(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public void markRunning(UUID id) {
        jobs.computeIfPresent(id, (key, existing) -> new IngestionJob(
                existing.id(),
                existing.jobType(),
                existing.targetId(),
                "running",
                existing.attemptCount() + 1,
                null,
                existing.createdAt(),
                Instant.now()));
    }

    @Override
    public void markSucceeded(UUID id) {
        jobs.computeIfPresent(id, (key, existing) -> new IngestionJob(
                existing.id(),
                existing.jobType(),
                existing.targetId(),
                "succeeded",
                existing.attemptCount(),
                null,
                existing.createdAt(),
                Instant.now()));
    }

    @Override
    public void markFailed(UUID id, String error) {
        jobs.computeIfPresent(id, (key, existing) -> new IngestionJob(
                existing.id(),
                existing.jobType(),
                existing.targetId(),
                "failed",
                existing.attemptCount(),
                error,
                existing.createdAt(),
                Instant.now()));
    }
}
