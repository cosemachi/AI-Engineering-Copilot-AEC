package com.aec.application.port;

import java.util.List;

public interface EmbeddingProvider {
    String providerName();
    List<Double> embed(String text);
}
