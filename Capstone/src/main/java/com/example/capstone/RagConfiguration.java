package com.example.capstone;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

    @Bean
    ChatClient ragChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        var advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(5)
                        .similarityThreshold(0.55d)
                        .build())
                .build();

        return builder
                .defaultSystem("Answer using only the supplied context. If the context does not contain the answer, say you do not know.")
                .defaultAdvisors(advisor)
                .build();
    }
}