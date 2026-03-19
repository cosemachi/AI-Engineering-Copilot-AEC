package com.aec.infrastructure.knowledge;

import com.aec.application.port.KnowledgeRepository;
import com.aec.domain.KnowledgeDocument;
import com.aec.domain.KnowledgeSnippet;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class InMemoryKnowledgeRepository implements KnowledgeRepository {

    private final List<KnowledgeDocument> documents = new CopyOnWriteArrayList<>();

    @Override
    public String repositoryName() {
        return "memory";
    }

    @Override
    public void save(KnowledgeDocument document) {
        documents.add(document);
    }

    @Override
    public List<KnowledgeSnippet> search(List<Double> embedding, int limit) {
        return documents.stream()
                .map(document -> toSnippet(document, cosine(document.embedding(), embedding)))
                .sorted(Comparator.comparingDouble(KnowledgeSnippet::score).reversed())
                .limit(limit)
                .toList();
    }

    private KnowledgeSnippet toSnippet(KnowledgeDocument document, double score) {
        String excerpt = document.content().length() <= 180
                ? document.content()
                : document.content().substring(0, 177) + "...";
        return new KnowledgeSnippet(document.title(), document.source(), excerpt, score);
    }

    private double cosine(List<Double> left, List<Double> right) {
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) {
            dot += left.get(i) * right.get(i);
            leftNorm += left.get(i) * left.get(i);
            rightNorm += right.get(i) * right.get(i);
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
