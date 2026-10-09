package com.example.capstone.ingestion;

import java.util.List;

public interface VectorStoreWriter {
    void write(List<DocumentChunk> chunks);
}
