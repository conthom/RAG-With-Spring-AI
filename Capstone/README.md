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
- `POST /api/chat` accepts JSON with `question`. The RAG advisor retrieves the five closest chunks and asks the model to answer only from that context.

## Configuration

Database connection settings are in `compose.yaml` and `application.yml`. Override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` when needed. Ollama defaults to `http://localhost:11434`; override with `SPRING_AI_OLLAMA_BASE_URL`.
