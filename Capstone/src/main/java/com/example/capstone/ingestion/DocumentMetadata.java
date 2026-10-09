package com.example.capstone.ingestion;

public record DocumentMetadata(
        String docId,
        String section,
        String equipment,
        String revision,
        String effectiveDate) {
}
