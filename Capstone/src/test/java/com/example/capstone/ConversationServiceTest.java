package com.example.capstone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConversationServiceTest {

    @Test
    void create_returnsGeneratedConversationId() {
        ConversationRepository repository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(repository, 4, 30);

        UUID conversationId = service.create();

        assertThat(conversationId).isNotNull();
        verify(repository).create(conversationId);
    }

    @Test
    void recentMessages_returnsOnlyConfiguredWindowInChronologicalOrder() {
        ConversationRepository repository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(repository, 2, 30);
        UUID conversationId = UUID.randomUUID();
        List<ConversationMessage> messages = List.of(
                new ConversationMessage("USER", "second", Instant.parse("2026-01-01T00:00:02Z")),
                new ConversationMessage("ASSISTANT", "reply", Instant.parse("2026-01-01T00:00:03Z")));
        when(repository.exists(conversationId)).thenReturn(true);
        when(repository.findRecentMessages(conversationId, 2)).thenReturn(messages);

        assertThat(service.recentMessages(conversationId)).containsExactlyElementsOf(messages);
    }

    @Test
    void recentMessages_rejectsUnknownConversation() {
        ConversationRepository repository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(repository, 4, 30);
        UUID conversationId = UUID.randomUUID();
        when(repository.exists(conversationId)).thenReturn(false);

        assertThatThrownBy(() -> service.recentMessages(conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void saveTurn_persistsBothMessages() {
        ConversationRepository repository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(repository, 4, 30);
        UUID conversationId = UUID.randomUUID();
        when(repository.exists(conversationId)).thenReturn(true);

        service.saveTurn(conversationId, "What is RAG?", "RAG retrieves relevant context.");

        verify(repository).appendMessage(conversationId, "USER", "What is RAG?");
        verify(repository).appendMessage(conversationId, "ASSISTANT", "RAG retrieves relevant context.");
    }

    @Test
    void findMessages_returnsEmptyForNewConversation() {
        ConversationRepository repository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(repository, 4, 30);
        UUID conversationId = UUID.randomUUID();
        when(repository.exists(conversationId)).thenReturn(true);
        when(repository.findRecentMessages(conversationId, 4)).thenReturn(List.of());

        assertThat(service.recentMessages(conversationId)).isEqualTo(List.of());
    }

    @Test
    void deleteExpiredConversations_returnsDeletedCount() {
        ConversationRepository repository = mock(ConversationRepository.class);
        ConversationService service = new ConversationService(repository, 4, 30);
        when(repository.deleteExpiredBefore(org.mockito.ArgumentMatchers.any())).thenReturn(3);

        assertThat(service.deleteExpiredConversations()).isEqualTo(3);
        verify(repository).deleteExpiredBefore(org.mockito.ArgumentMatchers.any());
    }
}
