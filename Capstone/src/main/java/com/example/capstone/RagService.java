package com.example.capstone;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
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

    public List<String> retrieveContext(String question) {
        return retrieveContext(question, DocumentFilter.empty());
    }

    public List<String> retrieveContext(String question, DocumentFilter filter) {
        int topK = filter == null || filter.isEmpty() ? 5 : 10;
        List<Document> matches = this.vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(0.0d)
                        .build());

        if (filter == null || filter.isEmpty()) {
            return matches.stream().map(Document::getText).toList();
        }

        return matches.stream()
                .filter(doc -> matchesFilter(doc, filter))
                .map(Document::getText)
                .toList();
    }

    public String answer(String question) {
        return answer(question, DocumentFilter.empty());
    }

    public String answer(String question, DocumentFilter filter) {
        return answer(question, filter, List.of());
    }

    public String answer(String question, DocumentFilter filter, List<ConversationMessage> history) {
        String retrievalQuery = retrievalQuery(question, history);
        List<String> context = retrieveContext(retrievalQuery, filter);
        if (context.isEmpty()) {
            return "I do not have enough relevant context to answer that question.";
        }

        String contextText = String.join("\n\n---\n\n", context);
        String historyText = history == null ? "" : history.stream()
                .map(message -> message.role() + ": " + message.content())
                .collect(java.util.stream.Collectors.joining("\n"));

        return this.chatClient.prompt()
                .system("Use the conversation history only to resolve references and conversational context. "
                        + "Use only the retrieved source context for factual claims. If the answer is not supported by "
                        + "the retrieved context, say you do not know.")
                .user("Conversation history:\n" + historyText + "\n\nCurrent question: " + question
                        + "\n\nRetrieved source context:\n" + contextText)
                .call()
                .content();
    }

    private String retrievalQuery(String question, List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return question;
        }

        String recentUserQuestions = history.stream()
                .filter(message -> "USER".equals(message.role()))
                .map(ConversationMessage::content)
                .collect(java.util.stream.Collectors.joining(" "));
        return recentUserQuestions.isBlank() ? question : recentUserQuestions + " " + question;
    }

    private boolean matchesFilter(Document document, DocumentFilter filter) {
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null) {
            return false;
        }

        if (filter.docId() != null && !filter.docId().isBlank()) {
            String actual = normalize(metadata.get("docId"));
            if (!Objects.equals(actual, normalize(filter.docId()))) {
                return false;
            }
        }

        if (filter.section() != null && !filter.section().isBlank()) {
            String actual = normalize(metadata.get("section"));
            if (!Objects.equals(actual, normalize(filter.section()))) {
                return false;
            }
        }

        if (filter.equipment() != null && !filter.equipment().isBlank()) {
            String actual = normalize(metadata.get("equipment"));
            if (!Objects.equals(actual, normalize(filter.equipment()))) {
                return false;
            }
        }

        if (filter.revision() != null && !filter.revision().isBlank()) {
            String actual = normalize(metadata.get("revision"));
            if (!Objects.equals(actual, normalize(filter.revision()))) {
                return false;
            }
        }

        if (filter.effectiveDate() != null && !filter.effectiveDate().isBlank()) {
            String actual = normalize(metadata.get("effectiveDate"));
            if (!Objects.equals(actual, normalize(filter.effectiveDate()))) {
                return false;
            }
        }

        return true;
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }
}