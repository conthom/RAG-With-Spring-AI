package com.example.capstone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemoryRepository {
    void create(UUID id, UUID conversationId, String content, String sourceContent);

    List<ConversationMemory> findByConversation(UUID conversationId);

    Optional<ConversationMemory> findById(UUID conversationId, UUID id);

    List<String> findApprovedContent(UUID conversationId);

    boolean updatePending(UUID conversationId, UUID id, String content);

    boolean transition(UUID conversationId, UUID id, MemoryStatus from, MemoryStatus to);

    boolean delete(UUID conversationId, UUID id);
}