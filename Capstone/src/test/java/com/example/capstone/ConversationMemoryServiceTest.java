package com.example.capstone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ConversationMemoryServiceTest {

    @Test
    void propose_createsPendingMemoryAndPreservesItsSource() {
        ConversationRepository conversations = mock(ConversationRepository.class);
        ConversationMemoryRepository memories = mock(ConversationMemoryRepository.class);
        ConversationMemoryService service = new ConversationMemoryService(conversations, memories);
        UUID conversationId = UUID.randomUUID();
        when(conversations.exists(conversationId)).thenReturn(true);
        when(memories.findById(eq(conversationId), any(UUID.class))).thenAnswer(invocation -> {
            UUID memoryId = invocation.getArgument(1);
            return Optional.of(memory(memoryId, conversationId, "Prefers concise answers.",
                    "Please keep answers concise.", MemoryStatus.PENDING));
        });

        ConversationMemory proposed = service.propose(
                conversationId, " Prefers concise answers. ", " Please keep answers concise. ");

        assertThat(proposed.status()).isEqualTo(MemoryStatus.PENDING);
        assertThat(proposed.content()).isEqualTo("Prefers concise answers.");
        assertThat(proposed.sourceContent()).isEqualTo("Please keep answers concise.");
        verify(memories).create(proposed.id(), conversationId,
                "Prefers concise answers.", "Please keep answers concise.");
    }

    @Test
    void approve_requiresPendingStateAndReturnsApprovedMemory() {
        ConversationRepository conversations = mock(ConversationRepository.class);
        ConversationMemoryRepository memories = mock(ConversationMemoryRepository.class);
        ConversationMemoryService service = new ConversationMemoryService(conversations, memories);
        UUID conversationId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        ConversationMemory pending = memory(memoryId, conversationId, "Prefers concise answers.",
                "Please keep answers concise.", MemoryStatus.PENDING);
        ConversationMemory approved = memory(memoryId, conversationId, "Prefers concise answers.",
                "Please keep answers concise.", MemoryStatus.APPROVED);
        when(conversations.exists(conversationId)).thenReturn(true);
        when(memories.findById(conversationId, memoryId)).thenReturn(Optional.of(pending), Optional.of(approved));
        when(memories.transition(conversationId, memoryId, MemoryStatus.PENDING, MemoryStatus.APPROVED))
                .thenReturn(true);

        ConversationMemory result = service.approve(conversationId, memoryId);

        assertThat(result.status()).isEqualTo(MemoryStatus.APPROVED);
        verify(memories).transition(conversationId, memoryId, MemoryStatus.PENDING, MemoryStatus.APPROVED);
    }

    @Test
    void approvedContentReturnsOnlyRepositoryApprovedMemory() {
        ConversationRepository conversations = mock(ConversationRepository.class);
        ConversationMemoryRepository memories = mock(ConversationMemoryRepository.class);
        ConversationMemoryService service = new ConversationMemoryService(conversations, memories);
        UUID conversationId = UUID.randomUUID();
        when(conversations.exists(conversationId)).thenReturn(true);
        when(memories.findApprovedContent(conversationId)).thenReturn(List.of("Prefers concise answers."));

        assertThat(service.approvedContent(conversationId)).containsExactly("Prefers concise answers.");
        verify(memories).findApprovedContent(conversationId);
    }

    @Test
    void edit_rejectsApprovedMemory() {
        ConversationRepository conversations = mock(ConversationRepository.class);
        ConversationMemoryRepository memories = mock(ConversationMemoryRepository.class);
        ConversationMemoryService service = new ConversationMemoryService(conversations, memories);
        UUID conversationId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        when(conversations.exists(conversationId)).thenReturn(true);
        when(memories.findById(conversationId, memoryId)).thenReturn(Optional.of(
                memory(memoryId, conversationId, "Prefers concise answers.",
                        "Please keep answers concise.", MemoryStatus.APPROVED)));

        assertThatThrownBy(() -> service.edit(conversationId, memoryId, "Changed preference."))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(memories, never()).updatePending(eq(conversationId), eq(memoryId), any());
    }

    private ConversationMemory memory(
            UUID id,
            UUID conversationId,
            String content,
            String source,
            MemoryStatus status) {
        Instant now = Instant.parse("2026-10-14T12:00:00Z");
        return new ConversationMemory(id, conversationId, content, source, status, now, now);
    }
}