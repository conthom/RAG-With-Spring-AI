package com.example.capstone.ingestion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DebugFileDocumentWriter implements FileDocumentWriter {

    @Override
    public void write(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        try {
            Path debugDir = Path.of("ingestion-debug");
            Files.createDirectories(debugDir);

            String fileName = chunks.get(0).docId() + "-" + System.currentTimeMillis() + ".txt";
            Path outputPath = debugDir.resolve(fileName);

            StringBuilder builder = new StringBuilder();
            for (DocumentChunk chunk : chunks) {
                builder.append("=== CHUNK ")
                        .append(chunk.chunkIndex())
                        .append(" ===\n")
                        .append("docId: ").append(chunk.docId()).append("\n")
                        .append("section: ").append(chunk.section()).append("\n")
                        .append("equipment: ").append(chunk.equipment()).append("\n")
                        .append("revision: ").append(chunk.revision()).append("\n")
                        .append("effectiveDate: ").append(chunk.effectiveDate()).append("\n")
                        .append("checksum: ").append(chunk.checksum()).append("\n\n")
                        .append(chunk.content())
                        .append("\n\n");
            }

            Files.writeString(outputPath, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write ingestion debug dump", e);
        }
    }
}
