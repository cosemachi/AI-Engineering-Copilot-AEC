package com.aec.infrastructure.ai;

import com.aec.application.port.LlmProvider;
import com.aec.domain.PrReviewResult;
import com.aec.domain.PullRequestData;
import com.aec.domain.ReviewIssue;
import com.aec.domain.Ticket;
import com.aec.domain.TicketAnalysis;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MockLlmProvider implements LlmProvider {

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public TicketAnalysis analyzeTicket(Ticket ticket) {
        List<String> risks = new ArrayList<>();
        List<String> missingInfo = new ArrayList<>();
        List<String> dependencies = new ArrayList<>();

        if (ticket.description() == null || ticket.description().isBlank()) {
            missingInfo.add("Ticket description is empty.");
        }
        if (ticket.comments() == null || ticket.comments().isEmpty()) {
            missingInfo.add("No discussion history is available.");
        }
        if ((ticket.description() + " " + String.join(" ", ticket.comments())).toLowerCase().contains("api")) {
            dependencies.add("External or internal API contract changes should be validated.");
        }
        if ((ticket.title() + " " + ticket.description()).toLowerCase().contains("migrate")) {
            risks.add("Migration work needs an explicit rollback plan.");
        }
        if (risks.isEmpty()) {
            risks.add("Scope, rollout, and owner assumptions should be validated before implementation.");
        }

        String summary = "Ticket '%s' from %s requests work on %s."
                .formatted(ticket.title(), ticket.source(), compress(ticket.description()));
        return new TicketAnalysis(summary, risks, missingInfo, dependencies);
    }

    @Override
    public PrReviewResult reviewPullRequest(PullRequestData pullRequestData) {
        List<ReviewIssue> issues = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        int changedFileCount = pullRequestData.files() == null ? 0 : pullRequestData.files().size();
        int patchHeavyFiles = 0;

        if (pullRequestData.files() != null) {
            for (var file : pullRequestData.files()) {
                if (file.patch() != null && file.patch().length() > 1200) {
                    patchHeavyFiles++;
                }
                if ("removed".equalsIgnoreCase(file.status())) {
                    issues.add(new ReviewIssue("design", "Deleted file %s may require compatibility checks.".formatted(file.path())));
                }
            }
        }
        if (patchHeavyFiles > 0) {
            issues.add(new ReviewIssue("performance", "Large diffs can hide expensive logic or missing tests in %d files.".formatted(patchHeavyFiles)));
        }
        if (issues.isEmpty()) {
            issues.add(new ReviewIssue("design", "No obvious diff-level issues found; verify tests and rollout assumptions."));
        }
        suggestions.add("Add focused tests for the changed behavior and failure paths.");
        suggestions.add("Document owner-visible tradeoffs if the PR changes API, persistence, or workflow boundaries.");

        String summary = "PR #%s updates %d file(s) from %s into %s."
                .formatted(pullRequestData.id(), changedFileCount, pullRequestData.headBranch(), pullRequestData.baseBranch());
        return new PrReviewResult(summary, issues, suggestions);
    }

    @Override
    public String answerKnowledgeQuery(String query, List<String> supportingDocuments) {
        if (supportingDocuments.isEmpty()) {
            return "No knowledge documents matched the query '%s'. Ingest documents before relying on the answer.".formatted(query);
        }
        return "Based on %d retrieved document(s), '%s' is primarily addressed by: %s"
                .formatted(supportingDocuments.size(), query, supportingDocuments.get(0));
    }

    private String compress(String text) {
        if (text == null || text.isBlank()) {
            return "an unspecified task";
        }
        return text.length() <= 80 ? text : text.substring(0, 77) + "...";
    }
}
