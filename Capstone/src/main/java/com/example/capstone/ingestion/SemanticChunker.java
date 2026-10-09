package com.example.capstone.ingestion;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SemanticChunker {

    private static final int TARGET_WORDS = 500;
    private static final int OVERLAP_WORDS = 100;

    public List<DocumentChunk> chunk(Path sourcePath, DocumentMetadata metadata, String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String[] words = content.trim().split("\\s+");
        List<DocumentChunk> chunks = new ArrayList<>();

        for (int start = 0; start < words.length; start += Math.max(1, TARGET_WORDS - OVERLAP_WORDS)) {
            int end = Math.min(start + TARGET_WORDS, words.length);
            String chunkText = String.join(" ", java.util.Arrays.copyOfRange(words, start, end));

            if (chunkText.isBlank()) {
                continue;
            }

            String checksum = sha256(chunkText);
            chunks.add(new DocumentChunk(
                    UUID.randomUUID().toString(),
                    metadata.docId(),
                    metadata.section(),
                    metadata.equipment(),
                    metadata.revision(),
                    metadata.effectiveDate(),
                    sourcePath.toString(),
                    chunkText,
                    chunks.size(),
                    checksum));

            if (end >= words.length) {
                break;
            }
        }

        return chunks;
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
