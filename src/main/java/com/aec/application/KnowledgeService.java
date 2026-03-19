package com.aec.application;

import com.aec.application.port.EmbeddingProvider;
import com.aec.application.port.IngestionJobStore;
import com.aec.application.port.KnowledgeDocumentStore;
import com.aec.application.port.KnowledgeRepository;
import com.aec.application.port.LlmProvider;
import com.aec.application.request.IngestKnowledgeCommand;
import com.aec.domain.IngestionJob;
import com.aec.domain.KnowledgeDocumentRecord;
import com.aec.domain.KnowledgeIngestResult;
import com.aec.domain.KnowledgeQueryResult;
import com.aec.domain.KnowledgeSnippet;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class KnowledgeService {

    private static final Logger LOG = Logger.getLogger(KnowledgeService.class);

    private final ProviderRegistry providerRegistry;
    private final KnowledgeDocumentStore knowledgeDocumentStore;
    private final IngestionJobStore ingestionJobStore;
    private final KnowledgeIngestionProcessor knowledgeIngestionProcessor;
    private final String llmProviderName;
    private final String embeddingProviderName;
    private final String repositoryName;

    public KnowledgeService(
            ProviderRegistry providerRegistry,
            KnowledgeDocumentStore knowledgeDocumentStore,
            IngestionJobStore ingestionJobStore,
            KnowledgeIngestionProcessor knowledgeIngestionProcessor,
            @ConfigProperty(name = "aec.ai.provider") String llmProviderName,
            @ConfigProperty(name = "aec.embedding.provider") String embeddingProviderName,
            @ConfigProperty(name = "aec.knowledge.repository") String repositoryName) {
        this.providerRegistry = providerRegistry;
        this.knowledgeDocumentStore = knowledgeDocumentStore;
        this.ingestionJobStore = ingestionJobStore;
        this.knowledgeIngestionProcessor = knowledgeIngestionProcessor;
        this.llmProviderName = llmProviderName;
        this.embeddingProviderName = embeddingProviderName;
        this.repositoryName = repositoryName;
    }

    public KnowledgeIngestResult ingest(IngestKnowledgeCommand command) {
        EmbeddingProvider embeddingProvider = providerRegistry.embeddingProvider(embeddingProviderName);
        KnowledgeRepository repository = providerRegistry.knowledgeRepository(repositoryName);
        LOG.infof("Queueing knowledge document '%s' from %s using embedding provider %s into repository %s",
                command.title(), command.source(), embeddingProvider.providerName(), repository.repositoryName());
        UUID documentId = knowledgeDocumentStore.create(
                command.title(),
                command.source(),
                command.content(),
                Map.of());
        UUID jobId = ingestionJobStore.create("knowledge_document_ingest", documentId);
        knowledgeIngestionProcessor.submit(
                documentId,
                jobId,
                command.title(),
                command.source(),
                command.content(),
                embeddingProvider,
                repository,
                knowledgeDocumentStore,
                ingestionJobStore);
        LOG.infof("Knowledge document '%s' queued successfully with job %s", command.title(), jobId);
        return new KnowledgeIngestResult(documentId, jobId, "queued");
    }

    public KnowledgeQueryResult query(String query) {
        EmbeddingProvider embeddingProvider = providerRegistry.embeddingProvider(embeddingProviderName);
        KnowledgeRepository repository = providerRegistry.knowledgeRepository(repositoryName);
        LlmProvider llmProvider = providerRegistry.llmProvider(llmProviderName);
        LOG.infof("Querying knowledge repository %s with embedding provider %s and LLM provider %s",
                repository.repositoryName(), embeddingProvider.providerName(), llmProvider.providerName());

        List<KnowledgeSnippet> snippets = repository.search(embeddingProvider.embed(query), 3);
        List<String> supportingDocuments = snippets.stream()
                .map(snippet -> snippet.title() + ": " + snippet.excerpt())
                .toList();
        String answer = llmProvider.answerKnowledgeQuery(query, supportingDocuments);
        LOG.infof("Knowledge query completed for '%s' with %d retrieved snippets", query, snippets.size());
        return new KnowledgeQueryResult(answer, snippets);
    }

    public KnowledgeDocumentRecord document(UUID id) {
        return knowledgeDocumentStore.find(id)
                .orElseThrow(() -> new AecNotFoundException("Knowledge document not found: " + id));
    }

    public IngestionJob job(UUID id) {
        return ingestionJobStore.find(id)
                .orElseThrow(() -> new AecNotFoundException("Ingestion job not found: " + id));
    }
}
