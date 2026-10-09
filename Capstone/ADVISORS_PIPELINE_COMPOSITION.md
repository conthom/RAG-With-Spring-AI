# Advisors & Pipeline Composition

## Overview

This implementation phase focuses on how the application composes the RAG retrieval pipeline in Spring AI. The purpose is to connect the vector store to the LLM so that answers are generated from retrieved context instead of from model memory alone.

In this project, the composition happens in the configuration layer, where a `ChatClient` is created with a `QuestionAnswerAdvisor` and a default system instruction.

---

## Current Pattern in the App

The key setup is in `RagConfiguration`:

```java
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
```

This means:
- the LLM is not called directly with a raw free-form prompt only
- Spring AI first searches the vector store for relevant chunks
- those chunks become the retrieval context
- the LLM is then instructed to answer only using that context

---

## Why Advisors Matter

Advisors are the bridge between retrieval and generation. They let the system inject domain-aware behavior into the chat pipeline without writing custom prompt logic for every endpoint.

In a RAG app, advisors are useful for:
- grounding answers in retrieved documents
- controlling retrieval behavior
- enforcing response policies
- reducing hallucinations by limiting knowledge to retrieved context

The `QuestionAnswerAdvisor` is especially relevant because it performs vector search automatically and passes the retrieved docs into the chat call.

---

## Pipeline Composition

The full composition is:

1. Document ingestion writes text chunks into the vector store
2. User asks a question
3. `QuestionAnswerAdvisor` runs a similarity search on the vector store
4. Top matching chunks are fetched
5. The model gets the question plus the retrieved chunks
6. The model answers only from that context

Conceptually:

```text
Question --> Vector Search --> Retrieved Context --> LLM --> Answer
```

This is the core retrieval-augmented generation pattern.

---

## Retrieval Settings

The current retrieval configuration is:

```java
.searchRequest(SearchRequest.builder()
        .topK(5)
        .similarityThreshold(0.55d)
        .build())
```

### `topK = 5`
This means the system retrieves the five closest matches for a question.

### `similarityThreshold = 0.55`
This filters out weaker matches that are too semantically distant.

These settings are important because they directly affect answer quality:
- too low a threshold may bring in noisy or irrelevant chunks
- too high a threshold may reduce recall and miss relevant context

---

## Context-Grounding Prompting

The system prompt is intentionally strict:

```java
.defaultSystem("Answer using only the supplied context. If the context does not contain the answer, say you do not know.")
```

This provides a safety pattern for RAG:
- the model is grounded in retrieved context
- the model should not invent facts
- the model should avoid answering outside the available evidence

This is one of the most important implementation points in a production-quality RAG app.

---

## Why This Fits the Current Architecture

This project already follows a clean separation between:
- ingestion of documents into a vector store
- retrieval from the vector store
- generation from the LLM

The advisor-based composition keeps that boundary clean:
- ingestion logic does not need to know about prompt generation
- the chat layer does not need to manually manage retrieval calls
- the application remains easier to extend with filters, ranking, or re-ranking later

---

## Extension Points

This phase is a good place to extend the pipeline with:

- metadata filters (`docId`, `equipment`, `revision`, `effectiveDate`)
- reranking of retrieved chunks before prompting
- citation generation from matched chunks
- fallback behavior when no matches exceed the threshold
- multi-turn memory or conversation history

For example, future retrieval could include filters like:

```java
SearchRequest.builder()
    .query(question)
    .topK(5)
    .similarityThreshold(0.55d)
    .filterExpression("equipment == 'Pump-07' and revision == 'Rev-3'")
    .build();
```

---

## Summary

The advisor composition phase is the heart of the RAG pipeline. It brings together three critical components:

- vector search
- retrieval of relevant chunks
- answer generation grounded in that context

In this project, that responsibility is implemented cleanly through `QuestionAnswerAdvisor` and a strict answer policy in the `ChatClient` configuration. This keeps the application aligned with the best-practice RAG pattern: retrieve first, then answer from retrieved evidence only.
