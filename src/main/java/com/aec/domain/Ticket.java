package com.aec.domain;

import java.util.List;

public record Ticket(
        String id,
        String title,
        String description,
        List<String> comments,
        String source) {
}
