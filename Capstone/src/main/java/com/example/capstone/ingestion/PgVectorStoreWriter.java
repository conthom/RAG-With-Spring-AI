package com.example.capstone.ingestion;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

@Component
public class PgVectorStoreWriter implements VectorStoreWriter {

    private final VectorStore vectorStore;

    public PgVectorStoreWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void write(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<Document> documents = chunks.stream()
                .map(chunk -> new Document(chunk.content(), Map.of(
                        "docId", chunk.docId(),
                        "section", chunk.section(),
                        "equipment", chunk.equipment(),
                        "revision", chunk.revision(),
                        "effectiveDate", chunk.effectiveDate(),
                        "sourcePath", chunk.sourcePath(),
                        "chunkIndex", chunk.chunkIndex(),
                        "checksum", chunk.checksum())))
                .toList();

        vectorStore.add(documents);
    }
}
