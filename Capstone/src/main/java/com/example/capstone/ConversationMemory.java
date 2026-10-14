package com.example.capstone;

import java.time.Instant;
import java.util.UUID;

public record ConversationMemory(
        UUID id,
        UUID conversationId,
        String content,
        String sourceContent,
        MemoryStatus status,
        Instant createdAt,
        Instant updatedAt) {
}