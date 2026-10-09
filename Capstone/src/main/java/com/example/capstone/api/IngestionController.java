package com.example.capstone.api;

import java.nio.file.Path;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.capstone.ingestion.DocumentIngestionService;
import com.example.capstone.ingestion.DocumentMetadata;
import com.example.capstone.ingestion.IngestionResult;

@RestController
@RequestMapping("/api")
public class IngestionController {

    private final DocumentIngestionService ingestionService;

    public IngestionController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest-file")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestionResponse ingestFile(@Valid @RequestBody IngestionRequest request) {
        Path sourcePath = Path.of(request.sourcePath());
        DocumentMetadata metadata = new DocumentMetadata(
                request.docId(),
                request.section(),
                request.equipment(),
                request.revision(),
                request.effectiveDate());

        IngestionResult result = ingestionService.ingest(sourcePath, metadata);
        return new IngestionResponse(result.sourcePath(), result.chunksStored(), result.status());
    }

    public record IngestionRequest(
            @NotBlank String sourcePath,
            String docId,
            String section,
            String equipment,
            String revision,
            String effectiveDate) {
    }

    public record IngestionResponse(
            String sourcePath,
            int chunksStored,
            String status) {
    }
}
