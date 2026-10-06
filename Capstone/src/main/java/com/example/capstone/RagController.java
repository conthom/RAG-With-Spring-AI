package com.example.capstone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
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
        return new ChatResponse(this.ragService.answer(request.question()));
    }

    record IngestRequest(
            @NotBlank @Size(max = 200_000) String content,
            @Size(max = 255) String source) {
    }

    record IngestResponse(String source, int chunksStored) {
    }

    record ChatRequest(@NotBlank @Size(max = 4_000) String question) {
    }

    record ChatResponse(String answer) {
    }
}