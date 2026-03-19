package com.aec.domain;

public record ChangedFile(
        String path,
        String status,
        int additions,
        int deletions,
        String patch) {
}
