package com.example.capstone;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository repository;
    private final int historyMessageLimit;
    private final int retentionDays;

    public ConversationService(
            ConversationRepository repository,
            @Value("${app.conversation.history-message-limit:10}") int historyMessageLimit,
            @Value("${app.conversation.retention-days:30}") int retentionDays) {
        this.repository = repository;
        this.historyMessageLimit = historyMessageLimit;
        this.retentionDays = retentionDays;
    }

    public UUID create() {
        UUID conversationId = UUID.randomUUID();
        repository.create(conversationId);
        return conversationId;
    }

    public List<ConversationMessage> recentMessages(UUID conversationId) {
        requireConversation(conversationId);
        return repository.findRecentMessages(conversationId, historyMessageLimit);
    }

    @Transactional
    public void saveTurn(UUID conversationId, String question, String answer) {
        requireConversation(conversationId);
        repository.appendMessage(conversationId, "USER", question);
        repository.appendMessage(conversationId, "ASSISTANT", answer);
    }

    public void delete(UUID conversationId) {
        if (!repository.delete(conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }
    }

    @Scheduled(fixedDelayString = "${app.conversation.cleanup-delay:3600000}")
    public int deleteExpiredConversations() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        return repository.deleteExpiredBefore(cutoff);
    }

    private void requireConversation(UUID conversationId) {
        if (!repository.exists(conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }
    }
}