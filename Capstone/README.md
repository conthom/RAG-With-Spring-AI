# Spring AI RAG Capstone

A Java REST API for ingesting text into PostgreSQL/PGVector and answering questions with retrieval-augmented generation using Spring AI and local Ollama models.

## Requirements

- JDK 25 or newer
- Maven 3.9 or newer
- Docker Desktop
- Ollama

## Start the dependencies

From this directory, start PGVector:

```powershell
docker compose up -d
```

In another terminal, start Ollama and download the chat and embedding models:

```powershell
ollama pull llama3.2
ollama pull nomic-embed-text
```

## Run the API

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Spring AI creates the PGVector schema on startup. The chat endpoint uses the `llama3.2` model and embeddings use `nomic-embed-text` by default; both can be changed with `OLLAMA_CHAT_MODEL` and `OLLAMA_EMBEDDING_MODEL`.

## Try it

Ingest a document:

```powershell
$document = @{
	source = "spring-ai-notes"
	content = "Retrieval augmented generation (RAG) retrieves relevant documents and adds them to an LLM prompt. A vector store indexes document embeddings so semantically similar text can be found for a query."
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/documents -ContentType "application/json" -Body $document
```

Ask a question grounded in the ingested content:

```powershell
$question = @{ question = "How does RAG use a vector store?" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/chat -ContentType "application/json" -Body $question
```

Check application health at `http://localhost:8080/actuator/health`.

## API

- `POST /api/documents` accepts JSON with `source` and `content`. Text is split into chunks and stored with its source metadata.
- `POST /api/conversations` creates a persistent conversation and returns a `conversationId`.
- `POST /api/chat` accepts `question`, an optional `conversationId`, and optional document metadata filters. If no ID is supplied, a conversation is created. The response includes the ID for follow-up turns.
- `GET /api/conversations/{conversationId}` returns the configured recent message window.
- `DELETE /api/conversations/{conversationId}` deletes a conversation and its messages.
- `GET /api/conversations/{conversationId}/memories` lists memory proposals and their review status.
- `POST /api/conversations/{conversationId}/memories` creates a pending proposal with `content` and `sourceContent`. Proposals are not used in answers until explicitly approved.
- `PUT /api/conversations/{conversationId}/memories/{memoryId}` edits a pending proposal. `POST .../{memoryId}/approve` or `POST .../{memoryId}/reject` records the user's decision.
- `POST .../{memoryId}/revoke` removes an approved memory from later prompts. `DELETE .../{memoryId}` permanently deletes a memory record.

To propose a memory, send the user-edited text and the message it came from. The proposal remains pending until approved:

```json
{
	"content": "Prefers concise answers with a short summary first.",
	"sourceContent": "Please keep answers concise and put the summary first."
}
```

Use the returned `id` with the `approve` or `reject` action endpoint. Editing is allowed only while the proposal is pending. Approved memories are supplied only to later turns in that conversation and are not treated as factual source evidence.

Conversation messages are stored in PostgreSQL, separately from document chunks in PGVector. The model receives a bounded recent history for follow-up context, while retrieved source documents remain authoritative for factual answers. Configure the history window with `CONVERSATION_HISTORY_MESSAGE_LIMIT` (defaults to 10 messages), inactivity retention with `CONVERSATION_RETENTION_DAYS` (defaults to 30 days), and cleanup cadence with `CONVERSATION_CLEANUP_DELAY` (defaults to one hour).

Conversation history is still persisted automatically. Long-term memory is separate: the user must manually propose a memory from an assistant reply, review its source passage, edit the pending text if needed, and explicitly approve it. Only `APPROVED` memories are included in later prompts for that conversation; rejected, pending, and revoked memories are excluded. Users can revoke or delete memories. The application does not automatically extract or promote chat messages. See [the conversation and memory implementation plan](CONVERSATION_MEMORY_IMPLEMENTATION_PLAN.md#human-in-the-loop-requirement).

Conversation and memory endpoints do not currently authenticate callers. Memories are scoped to a conversation, not an authenticated person. The generated UUID is difficult to guess, but this is suitable only for local/single-user use; add authentication and owner scoping before exposing it to multiple users.

## Configuration

Database connection settings are in `compose.yaml` and `application.yml`. Override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` when needed. Ollama defaults to `http://localhost:11434`; override with `SPRING_AI_OLLAMA_BASE_URL`.
