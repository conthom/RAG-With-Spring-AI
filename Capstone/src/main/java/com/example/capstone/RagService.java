package com.example.capstone;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final TokenTextSplitter textSplitter;

    public RagService(VectorStore vectorStore, ChatClient ragChatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = ragChatClient;
        this.textSplitter = TokenTextSplitter.builder().withChunkSize(800).build();
    }

    public int ingest(String source, String content) {
        var document = new Document(content, Map.of("source", source));
        List<Document> chunks = this.textSplitter.apply(List.of(document));
        this.vectorStore.add(chunks);
        return chunks.size();
    }

    public String answer(String question) {
        return this.chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}