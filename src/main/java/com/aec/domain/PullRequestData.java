package com.aec.domain;

import java.util.List;

public record PullRequestData(
        String id,
        String title,
        String description,
        String author,
        String baseBranch,
        String headBranch,
        List<ChangedFile> files) {
}
