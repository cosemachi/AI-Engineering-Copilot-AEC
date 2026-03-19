package com.aec.domain;

import java.util.List;

public record TicketAnalysis(
        String summary,
        List<String> risks,
        List<String> missing_info,
        List<String> dependencies) {
}
