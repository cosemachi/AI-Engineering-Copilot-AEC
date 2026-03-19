package com.aec.application;

import com.aec.application.port.EmbeddingProvider;
import com.aec.application.port.KnowledgeRepository;
import com.aec.application.port.LlmProvider;
import com.aec.application.port.TicketSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProviderRegistry {

    @Inject
    Instance<TicketSource> ticketSources;

    @Inject
    Instance<LlmProvider> llmProviders;

    @Inject
    Instance<EmbeddingProvider> embeddingProviders;

    @Inject
    Instance<KnowledgeRepository> knowledgeRepositories;

    public TicketSource ticketSource(String source) {
        return ticketSources.stream()
                .filter(candidate -> candidate.sourceName().equalsIgnoreCase(source))
                .findFirst()
                .orElseThrow(() -> new AecException("Unsupported ticket source: " + source));
    }

    public LlmProvider llmProvider(String provider) {
        return llmProviders.stream()
                .filter(candidate -> candidate.providerName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new AecException("Unsupported LLM provider: " + provider));
    }

    public EmbeddingProvider embeddingProvider(String provider) {
        return embeddingProviders.stream()
                .filter(candidate -> candidate.providerName().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new AecException("Unsupported embedding provider: " + provider));
    }

    public KnowledgeRepository knowledgeRepository(String repository) {
        return knowledgeRepositories.stream()
                .filter(candidate -> candidate.repositoryName().equalsIgnoreCase(repository))
                .findFirst()
                .orElseThrow(() -> new AecException("Unsupported knowledge repository: " + repository));
    }
}
