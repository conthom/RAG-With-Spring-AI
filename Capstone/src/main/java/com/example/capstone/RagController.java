package com.example.capstone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;
    private final ConversationService conversationService;

    public RagController(
            RagService ragService,
            ConversationService conversationService) {
        this.ragService = ragService;
        this.conversationService = conversationService;
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    ConversationResponse createConversation() {
        return new ConversationResponse(conversationService.create().toString(), List.of());
    }

    @GetMapping("/conversations/{conversationId}")
    ConversationResponse getConversation(@PathVariable String conversationId) {
        UUID id = parseConversationId(conversationId);
        List<MessageResponse> messages = conversationService.recentMessages(id).stream()
                .map(message -> new MessageResponse(message.role(), message.content(), message.createdAt()))
                .toList();
        return new ConversationResponse(id.toString(), messages);
    }

    @DeleteMapping("/conversations/{conversationId}")
    ResponseEntity<Void> deleteConversation(@PathVariable String conversationId) {
        conversationService.delete(parseConversationId(conversationId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    IngestResponse ingest(@Valid @RequestBody IngestRequest request) {
        String source = request.source() == null || request.source().isBlank() ? "api" : request.source();
        int chunks = this.ragService.ingest(source, request.content());
        return new IngestResponse(source, chunks);
    }

    @PostMapping("/chat")
    ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        UUID conversationId = request.conversationId() == null || request.conversationId().isBlank()
            ? conversationService.create()
            : parseConversationId(request.conversationId());
        List<ConversationMessage> history = conversationService.recentMessages(conversationId);
        DocumentFilter filter = new DocumentFilter(
                request.docId(),
                request.section(),
                request.equipment(),
                request.revision(),
                request.effectiveDate());
        String answer = this.ragService.answer(request.question(), filter, history);
        conversationService.saveTurn(conversationId, request.question(), answer);
        return new ChatResponse(conversationId.toString(), answer);
    }

    private UUID parseConversationId(String conversationId) {
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid conversation ID", exception);
        }
    }

    record IngestRequest(
            @NotBlank @Size(max = 200_000) String content,
            @Size(max = 255) String source) {
    }

    record IngestResponse(String source, int chunksStored) {
    }

    record ChatRequest(
            @NotBlank @Size(max = 4_000) String question,
            String conversationId,
            String docId,
            String section,
            String equipment,
            String revision,
            String effectiveDate) {
    }

    record ChatResponse(String conversationId, String answer) {
    }

    record ConversationResponse(String conversationId, List<MessageResponse> messages) {
    }

    record MessageResponse(String role, String content, Instant createdAt) {
    }
}