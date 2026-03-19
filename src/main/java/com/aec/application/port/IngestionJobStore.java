package com.aec.application.port;

import com.aec.domain.IngestionJob;
import java.util.Optional;
import java.util.UUID;

public interface IngestionJobStore {
    UUID create(String jobType, UUID targetId);
    Optional<IngestionJob> find(UUID id);
    void markRunning(UUID id);
    void markSucceeded(UUID id);
    void markFailed(UUID id, String error);
}
