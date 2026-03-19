package com.aec.domain;

import java.util.List;

public record PrReviewResult(
        String summary,
        List<ReviewIssue> issues,
        List<String> suggestions) {
}
