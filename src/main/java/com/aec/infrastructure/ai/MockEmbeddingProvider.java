package com.aec.infrastructure.ai;

import com.aec.application.port.EmbeddingProvider;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MockEmbeddingProvider implements EmbeddingProvider {

    private static final int DIMENSIONS = 1536;

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public List<Double> embed(String text) {
        double[] values = new double[DIMENSIONS];
        for (int i = 0; i < text.length(); i++) {
            values[i % values.length] += text.charAt(i);
        }
        List<Double> embedding = new ArrayList<>();
        for (double value : values) {
            embedding.add(value / Math.max(1, text.length()));
        }
        return embedding;
    }
}
