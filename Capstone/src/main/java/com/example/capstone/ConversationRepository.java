package com.example.capstone;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationRepository {
    void create(UUID conversationId);

    boolean exists(UUID conversationId);

    void appendMessage(UUID conversationId, String role, String content);

    List<ConversationMessage> findRecentMessages(UUID conversationId, int limit);

    boolean delete(UUID conversationId);

    int deleteExpiredBefore(Instant cutoff);
}