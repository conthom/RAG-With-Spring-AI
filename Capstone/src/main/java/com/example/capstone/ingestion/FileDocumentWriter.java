package com.example.capstone.ingestion;

import java.util.List;

public interface FileDocumentWriter {
    void write(List<DocumentChunk> chunks);
}
