package com.aec.application.port;

import com.aec.domain.KnowledgeDocument;
import com.aec.domain.KnowledgeSnippet;
import java.util.List;

public interface KnowledgeRepository {
    String repositoryName();
    void save(KnowledgeDocument document);
    List<KnowledgeSnippet> search(List<Double> embedding, int limit);
}
