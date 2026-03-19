package com.aec.domain;

import java.util.List;

public record KnowledgeQueryResult(
        String answer,
        List<KnowledgeSnippet> relevantDocuments) {
}
