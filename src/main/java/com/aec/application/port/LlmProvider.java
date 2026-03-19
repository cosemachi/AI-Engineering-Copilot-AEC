package com.aec.application.port;

import com.aec.domain.PrReviewResult;
import com.aec.domain.PullRequestData;
import com.aec.domain.Ticket;
import com.aec.domain.TicketAnalysis;
import java.util.List;

public interface LlmProvider {
    String providerName();
    TicketAnalysis analyzeTicket(Ticket ticket);
    PrReviewResult reviewPullRequest(PullRequestData pullRequestData);
    String answerKnowledgeQuery(String query, List<String> supportingDocuments);
}
