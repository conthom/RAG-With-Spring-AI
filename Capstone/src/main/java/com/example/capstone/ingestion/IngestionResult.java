package com.example.capstone.ingestion;

public record IngestionResult(
        String sourcePath,
        int chunksStored,
        String status) {
}
