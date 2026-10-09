package com.example.capstone.ingestion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionService {

    private final TikaDocumentReader documentReader;
    private final SemanticChunker semanticChunker;
    private final FileDocumentWriter fileDocumentWriter;
    private final VectorStoreWriter vectorStoreWriter;
    private final Map<String, String> checksumIndex = new ConcurrentHashMap<>();

    public DocumentIngestionService(
            TikaDocumentReader documentReader,
            SemanticChunker semanticChunker,
            FileDocumentWriter fileDocumentWriter,
            VectorStoreWriter vectorStoreWriter) {
        this.documentReader = documentReader;
        this.semanticChunker = semanticChunker;
        this.fileDocumentWriter = fileDocumentWriter;
        this.vectorStoreWriter = vectorStoreWriter;
    }

    public IngestionResult ingest(Path sourcePath, DocumentMetadata metadata) {
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Source document does not exist: " + sourcePath);
        }

        String content = documentReader.read(sourcePath);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("No readable content found in: " + sourcePath);
        }

        String normalizedPath = sourcePath.toAbsolutePath().normalize().toString();
        String checksum = sha256(content);
        String previousChecksum = checksumIndex.get(normalizedPath);

        if (previousChecksum != null && previousChecksum.equals(checksum)) {
            return new IngestionResult(normalizedPath, 0, "SKIPPED");
        }

        List<DocumentChunk> chunks = semanticChunker.chunk(sourcePath, metadata, content);
        if (!chunks.isEmpty()) {
            fileDocumentWriter.write(chunks);
            vectorStoreWriter.write(chunks);
        }

        checksumIndex.put(normalizedPath, checksum);
        return new IngestionResult(normalizedPath, chunks.size(), "SUCCESS");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
