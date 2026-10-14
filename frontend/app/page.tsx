"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import Link from "next/link";

type Message = {
  role: "USER" | "ASSISTANT";
  content: string;
};

const suggestions = [
  "How does RAG use a vector store?",
  "What makes an answer grounded?",
  "Summarize the indexed notes",
];

export default function Home() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [question, setQuestion] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [isIngesting, setIsIngesting] = useState(false);
  const [source, setSource] = useState("spring-ai-notes");
  const [content, setContent] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const transcriptEnd = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const savedId = window.localStorage.getItem("rag-conversation-id");
    if (!savedId) return;

    let cancelled = false;
    fetch(`/api/conversations/${encodeURIComponent(savedId)}`)
      .then((response) => (response.ok ? response.json() : null))
      .then((conversation) => {
        if (cancelled) return;
        if (!conversation) {
          window.localStorage.removeItem("rag-conversation-id");
          return;
        }
        setConversationId(conversation.conversationId);
        setMessages(conversation.messages ?? []);
      })
      .catch(() => {
        if (!cancelled) setError("Could not load the saved conversation.");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    transcriptEnd.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, isSending]);

  async function sendQuestion(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    const submittedQuestion = question.trim();
    if (!submittedQuestion || isSending) return;

    setQuestion("");
    setError("");
    setNotice("");
    setMessages((current) => [...current, { role: "USER", content: submittedQuestion }]);
    setIsSending(true);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: submittedQuestion, conversationId }),
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message ?? result.error ?? "The request failed.");

      setConversationId(result.conversationId);
      window.localStorage.setItem("rag-conversation-id", result.conversationId);
      setMessages((current) => [...current, { role: "ASSISTANT", content: result.answer }]);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not reach the Spring AI API. Check that it is running on port 8080.",
      );
    } finally {
      setIsSending(false);
    }
  }

  async function ingestDocument(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!content.trim() || isIngesting) return;

    setIsIngesting(true);
    setNotice("");
    setError("");
    try {
      const response = await fetch("/api/documents", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source, content }),
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message ?? result.error ?? "Ingestion failed.");
      setNotice(`${result.chunksStored} chunk${result.chunksStored === 1 ? "" : "s"} indexed as ${result.source}.`);
      setContent("");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not index this document.");
    } finally {
      setIsIngesting(false);
    }
  }

  function startNewChat() {
    setMessages([]);
    setConversationId(null);
    setError("");
    setNotice("");
    window.localStorage.removeItem("rag-conversation-id");
  }

  function handleQuestionKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void sendQuestion();
    }
  }

  return (
    <main className="workspace">
      <aside className="sidebar">
        <Link className="brand" href="/" aria-label="Fieldnotes home">
          <span className="brand-mark">f.</span>
          <span className="brand-name">fieldnotes<span> / RAG</span></span>
        </Link>

        <button className="new-chat" onClick={startNewChat} type="button">
          <span aria-hidden="true">+</span> New conversation
        </button>

        <div className="sidebar-section">
          <p className="eyebrow">WORKSPACE</p>
          <div className="workspace-item active-workspace">
            <span className="workspace-dot" />
            <span>Local knowledge base</span>
          </div>
        </div>

        <section className="source-panel">
          <div className="source-heading">
            <div>
              <p className="eyebrow">YOUR SOURCES</p>
              <h2>Knowledge base</h2>
            </div>
            <span className="source-count" aria-label="Vector database">DB</span>
          </div>
          <p className="source-copy">Add text for retrieval. Spring AI splits it into chunks and stores them in PGVector.</p>
          <form className="ingest-form" onSubmit={ingestDocument}>
            <label htmlFor="source-name">Source name</label>
            <input
              id="source-name"
              value={source}
              onChange={(event) => setSource(event.target.value)}
              maxLength={255}
              placeholder="e.g. product-guide"
            />
            <label htmlFor="source-content">Document text</label>
            <textarea
              id="source-content"
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="Paste notes or document text..."
              rows={5}
              required
            />
            <button className="index-button" type="submit" disabled={isIngesting || !content.trim()}>
              {isIngesting ? "Indexing..." : "Add to knowledge base"}
            </button>
          </form>
          <p className="source-footnote">This indexes text you provide; it does not import documents from the web.</p>
        </section>

        <div className="sidebar-bottom">
          <span className="api-indicator" />
          <span>Spring AI API</span>
          <span className="sidebar-bottom-note">localhost:8080</span>
        </div>
      </aside>

      <section className="chat-area" aria-label="Chat">
        <header className="topbar">
          <div className="mobile-brand"><span className="brand-mark">f.</span> fieldnotes</div>
          <div className="model-label"><span className="model-dot" /> RAG assistant <span className="model-divider">/</span> Ollama</div>
          <button className="mobile-new-chat" onClick={startNewChat} type="button">New chat</button>
        </header>

        <div className="conversation-scroll">
          <div className={`conversation ${messages.length === 0 ? "conversation-empty" : ""}`}>
            {messages.length === 0 ? (
              <div className="welcome">
                <p className="eyebrow welcome-eyebrow">YOUR LOCAL KNOWLEDGE, IN CONTEXT</p>
                <h1>What would you<br /><em>like to know?</em></h1>
                <p className="welcome-copy">Ask a question about the documents you have indexed. Answers are generated from retrieved context.</p>
                <div className="suggestions" aria-label="Suggested questions">
                  {suggestions.map((suggestion) => (
                    <button key={suggestion} type="button" onClick={() => setQuestion(suggestion)}>
                      <span aria-hidden="true">↗</span>{suggestion}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <div className="message-list">
                {messages.map((message, index) => (
                  <article className={`message-row ${message.role === "USER" ? "user-row" : "assistant-row"}`} key={`${message.role}-${index}`}>
                    {message.role === "ASSISTANT" && <div className="assistant-avatar" aria-hidden="true">f.</div>}
                    <div className="message-body">
                      <p className="message-label">{message.role === "USER" ? "YOU" : "FIELDNOTES"}</p>
                      <div className="message-content">{message.content}</div>
                    </div>
                    {message.role === "USER" && <div className="user-avatar" aria-hidden="true">Y</div>}
                  </article>
                ))}
                {isSending && (
                  <div className="message-row assistant-row">
                    <div className="assistant-avatar" aria-hidden="true">f.</div>
                    <div className="message-body">
                      <p className="message-label">FIELDNOTES</p>
                      <div className="thinking"><span /><span /><span /></div>
                    </div>
                  </div>
                )}
                <div ref={transcriptEnd} />
              </div>
            )}
          </div>
        </div>

        <div className="composer-wrap">
          {(error || notice) && <p className={`feedback ${error ? "feedback-error" : "feedback-success"}`} role="status">{error || notice}</p>}
          <form className="composer" onSubmit={sendQuestion}>
            <textarea
              aria-label="Your message"
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              onKeyDown={handleQuestionKeyDown}
              placeholder="Ask about your indexed documents..."
              rows={2}
            />
            <div className="composer-footer">
              <span>Shift + Enter for a new line</span>
              <button className="send-button" type="submit" disabled={!question.trim() || isSending}>
                {isSending ? "Thinking..." : "Send question"}<span aria-hidden="true">↗</span>
              </button>
            </div>
          </form>
          <p className="disclaimer">Responses are generated from retrieved content and may need verification.</p>
        </div>
      </section>
    </main>
  );
}
