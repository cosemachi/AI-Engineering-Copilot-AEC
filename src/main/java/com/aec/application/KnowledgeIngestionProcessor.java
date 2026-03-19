package com.aec.application;

import com.aec.application.port.EmbeddingProvider;
import com.aec.application.port.IngestionJobStore;
import com.aec.application.port.KnowledgeDocumentStore;
import com.aec.application.port.KnowledgeRepository;
import com.aec.domain.KnowledgeDocument;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KnowledgeIngestionProcessor {

    private static final Logger LOG = Logger.getLogger(KnowledgeIngestionProcessor.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public void submit(
            UUID documentId,
            UUID jobId,
            String title,
            String source,
            String content,
            EmbeddingProvider embeddingProvider,
            KnowledgeRepository repository,
            KnowledgeDocumentStore documentStore,
            IngestionJobStore jobStore) {
        executor.submit(() -> process(documentId, jobId, title, source, content,
                embeddingProvider, repository, documentStore, jobStore));
    }

    private void process(
            UUID documentId,
            UUID jobId,
            String title,
            String source,
            String content,
            EmbeddingProvider embeddingProvider,
            KnowledgeRepository repository,
            KnowledgeDocumentStore documentStore,
            IngestionJobStore jobStore) {
        try {
            jobStore.markRunning(jobId);
            documentStore.markProcessing(documentId, jobId);
            List<String> chunks = chunk(content);
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                repository.save(new KnowledgeDocument(
                        UUID.randomUUID(),
                        title + " [chunk " + (i + 1) + "]",
                        source,
                        chunk,
                        embeddingProvider.embed(chunk)));
            }
            documentStore.markIndexed(documentId, jobId, chunks.size());
            jobStore.markSucceeded(jobId);
            LOG.infof("Knowledge ingestion job %s succeeded for document %s with %d chunks",
                    jobId, documentId, chunks.size());
        } catch (RuntimeException exception) {
            documentStore.markFailed(documentId, jobId);
            jobStore.markFailed(jobId, exception.getMessage());
            LOG.errorf(exception, "Knowledge ingestion job %s failed for document %s", jobId, documentId);
        }
    }

    private List<String> chunk(String content) {
        List<String> chunks = new ArrayList<>();
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            chunks.add("");
            return chunks;
        }

        int chunkSize = 400;
        for (int start = 0; start < normalized.length(); start += chunkSize) {
            int end = Math.min(normalized.length(), start + chunkSize);
            chunks.add(normalized.substring(start, end));
        }
        return chunks;
    }
}
