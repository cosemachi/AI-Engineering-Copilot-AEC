package com.aec.observability;

import com.aec.application.ProviderRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class AecReadinessCheck implements HealthCheck {

    private final ProviderRegistry providerRegistry;
    private final String llmProviderName;
    private final String embeddingProviderName;
    private final String repositoryName;

    public AecReadinessCheck(
            ProviderRegistry providerRegistry,
            @ConfigProperty(name = "aec.ai.provider") String llmProviderName,
            @ConfigProperty(name = "aec.embedding.provider") String embeddingProviderName,
            @ConfigProperty(name = "aec.knowledge.repository") String repositoryName) {
        this.providerRegistry = providerRegistry;
        this.llmProviderName = llmProviderName;
        this.embeddingProviderName = embeddingProviderName;
        this.repositoryName = repositoryName;
    }

    @Override
    public HealthCheckResponse call() {
        try {
            String llmProvider = providerRegistry.llmProvider(llmProviderName).providerName();
            String embeddingProvider = providerRegistry.embeddingProvider(embeddingProviderName).providerName();
            String knowledgeRepository = providerRegistry.knowledgeRepository(repositoryName).repositoryName();

            return HealthCheckResponse.named("aec-readiness")
                    .up()
                    .withData("llmProvider", llmProvider)
                    .withData("embeddingProvider", embeddingProvider)
                    .withData("knowledgeRepository", knowledgeRepository)
                    .build();
        } catch (RuntimeException exception) {
            return HealthCheckResponse.named("aec-readiness")
                    .down()
                    .withData("error", exception.getMessage())
                    .build();
        }
    }
}
