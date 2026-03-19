package com.aec.application;

import com.aec.application.port.LlmProvider;
import com.aec.application.port.PullRequestGateway;
import com.aec.application.request.ReviewPullRequestCommand;
import com.aec.domain.PrReviewResult;
import com.aec.domain.PullRequestData;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PullRequestReviewService {

    private static final Logger LOG = Logger.getLogger(PullRequestReviewService.class);

    private final PullRequestGateway pullRequestGateway;
    private final ProviderRegistry providerRegistry;
    private final String llmProviderName;

    public PullRequestReviewService(
            PullRequestGateway pullRequestGateway,
            ProviderRegistry providerRegistry,
            @ConfigProperty(name = "aec.ai.provider") String llmProviderName) {
        this.pullRequestGateway = pullRequestGateway;
        this.providerRegistry = providerRegistry;
        this.llmProviderName = llmProviderName;
    }

    public PrReviewResult review(ReviewPullRequestCommand command) {
        PullRequestData pullRequestData =
                pullRequestGateway.fetch(command.owner(), command.repo(), command.number());
        LlmProvider provider = providerRegistry.llmProvider(llmProviderName);
        LOG.infof("Reviewing PR %s/%s#%d with provider %s",
                command.owner(), command.repo(), command.number(), provider.providerName());
        PrReviewResult review = provider.reviewPullRequest(pullRequestData);
        LOG.infof("PR review completed for #%d: %d issues, %d suggestions",
                command.number(), review.issues().size(), review.suggestions().size());
        return review;
    }
}
