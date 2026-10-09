package com.example.capstone.ingestion;

public record DocumentChunk(
        String id,
        String docId,
        String section,
        String equipment,
        String revision,
        String effectiveDate,
        String sourcePath,
        String content,
        int chunkIndex,
        String checksum) {
}
