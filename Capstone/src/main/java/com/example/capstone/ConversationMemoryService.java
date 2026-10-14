package com.example.capstone;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationMemoryService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemoryRepository memoryRepository;

    public ConversationMemoryService(
            ConversationRepository conversationRepository,
            ConversationMemoryRepository memoryRepository) {
        this.conversationRepository = conversationRepository;
        this.memoryRepository = memoryRepository;
    }

    public List<ConversationMemory> list(UUID conversationId) {
        requireConversation(conversationId);
        return memoryRepository.findByConversation(conversationId);
    }

    public ConversationMemory propose(UUID conversationId, String content, String sourceContent) {
        requireConversation(conversationId);
        UUID memoryId = UUID.randomUUID();
        memoryRepository.create(memoryId, conversationId, content.trim(), sourceContent.trim());
        return find(conversationId, memoryId);
    }

    public ConversationMemory edit(UUID conversationId, UUID memoryId, String content) {
        ConversationMemory memory = find(conversationId, memoryId);
        requireStatus(memory, MemoryStatus.PENDING);
        if (!memoryRepository.updatePending(conversationId, memoryId, content.trim())) {
            throw stateConflict();
        }
        return find(conversationId, memoryId);
    }

    public ConversationMemory approve(UUID conversationId, UUID memoryId) {
        return transition(conversationId, memoryId, MemoryStatus.PENDING, MemoryStatus.APPROVED);
    }

    public ConversationMemory reject(UUID conversationId, UUID memoryId) {
        return transition(conversationId, memoryId, MemoryStatus.PENDING, MemoryStatus.REJECTED);
    }

    public ConversationMemory revoke(UUID conversationId, UUID memoryId) {
        return transition(conversationId, memoryId, MemoryStatus.APPROVED, MemoryStatus.REVOKED);
    }

    public void delete(UUID conversationId, UUID memoryId) {
        requireConversation(conversationId);
        if (!memoryRepository.delete(conversationId, memoryId)) {
            throw notFound();
        }
    }

    public List<String> approvedContent(UUID conversationId) {
        requireConversation(conversationId);
        return memoryRepository.findApprovedContent(conversationId);
    }

    private ConversationMemory transition(
            UUID conversationId,
            UUID memoryId,
            MemoryStatus from,
            MemoryStatus to) {
        ConversationMemory memory = find(conversationId, memoryId);
        requireStatus(memory, from);
        if (!memoryRepository.transition(conversationId, memoryId, from, to)) {
            throw stateConflict();
        }
        return find(conversationId, memoryId);
    }

    private ConversationMemory find(UUID conversationId, UUID memoryId) {
        requireConversation(conversationId);
        return memoryRepository.findById(conversationId, memoryId).orElseThrow(this::notFound);
    }

    private void requireConversation(UUID conversationId) {
        if (!conversationRepository.exists(conversationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
    }

    private void requireStatus(ConversationMemory memory, MemoryStatus expected) {
        if (memory.status() != expected) {
            throw stateConflict();
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory not found");
    }

    private ResponseStatusException stateConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Memory is not in a state that allows this action");
    }
}