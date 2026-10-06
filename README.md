# RAG with Spring AI Practice

This repository is a hands-on learning project built to strengthen my understanding of Retrieval-Augmented Generation (RAG) using Spring AI. It follows the Infosys Lex training course on "RAG with Spring AI" and focuses on applying the core concepts behind LLM-powered search and grounded question answering.

## Purpose

The goal of this repo is to practice the end-to-end flow of a RAG application:

- ingesting documents and splitting them into meaningful chunks
- converting text into embeddings
- storing embeddings in a vector database for semantic retrieval
- searching for the most relevant context for a user query
- sending that context to an LLM alongside the question to generate a grounded answer

This is a practical example of how RAG improves model responses by anchoring them in relevant knowledge, rather than relying only on the model's general knowledge.

## What is being practiced here

This project demonstrates the main building blocks of modern RAG systems:

- Spring Boot for the application layer
- Spring AI for LLM and embedding integrations
- PostgreSQL with PGVector for vector storage
- Ollama for running local LLM and embedding models
- document ingestion and retrieval pipelines
- prompt construction with retrieved context

The application acts as a small reference implementation for exploring how retrieval and generation work together in real systems.

## Repo structure

- `Capstone/` contains the main Spring AI application and implementation details
- `compose.yaml` sets up the supporting PostgreSQL/PGVector database
- `pom.xml` defines the Java and Spring AI dependencies

## Learning focus

This repo is intentionally a practice-focused project. It is meant to help with familiarity and experimentation around:

- vector databases and semantic search
- embedding models and similarity matching
- chunking strategies for long-form knowledge
- grounding LLM responses in retrieved context
- building simple AI applications with Spring AI

## Outcome

By working through this project, the intention is to gain practical experience in building a small but realistic RAG workflow, using industry-relevant tools and patterns from the Spring AI ecosystem.

For the complete application implementation and usage notes, see the project README in the [Capstone](./Capstone/README.md) folder.
