package com.example.capstone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RagServiceTest {

    @Test
    void retrieveContext_returnsTopMatchesFromVectorStore() {
        VectorStore vectorStore = mock(VectorStore.class);
        SearchRequest request = SearchRequest.builder()
                .query("What is RAG?")
                .topK(5)
                .similarityThreshold(0.0d)
                .build();

        when(vectorStore.similaritySearch(request)).thenReturn(List.of(
                new Document("RAG retrieves relevant documents before answering."),
                new Document("It combines retrieval with generation to ground responses.")));

        RagService ragService = new RagService(vectorStore, null);

        List<String> context = ragService.retrieveContext("What is RAG?");

        assertThat(context).containsExactly(
                "RAG retrieves relevant documents before answering.",
                "It combines retrieval with generation to ground responses.");
    }

    @Test
    void retrieveContext_filtersByMetadataWhenRequested() {
        VectorStore vectorStore = mock(VectorStore.class);
        SearchRequest request = SearchRequest.builder()
                .query("What is maintenance protocol?")
                .topK(10)
                .similarityThreshold(0.0d)
                .build();

        when(vectorStore.similaritySearch(request)).thenReturn(List.of(
                new Document("Pump maintenance requires daily calibration.", java.util.Map.of(
                        "docId", "DOC-100",
                        "equipment", "Pump-07",
                        "revision", "Rev-3",
                        "section", "Maintenance")),
                new Document("Generator maintenance requires weekly inspection.", java.util.Map.of(
                        "docId", "DOC-200",
                        "equipment", "Generator-01",
                        "revision", "Rev-1",
                        "section", "Operations"))));

        RagService ragService = new RagService(vectorStore, null);

        List<String> context = ragService.retrieveContext("What is maintenance protocol?",
                new DocumentFilter("DOC-100", "Maintenance", "Pump-07", "Rev-3", null));

        assertThat(context).containsExactly("Pump maintenance requires daily calibration.");
    }
}
