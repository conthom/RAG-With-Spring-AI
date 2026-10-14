"use client";

import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import Link from "next/link";

type Message = {
  role: "USER" | "ASSISTANT";
  content: string;
};

type MemoryStatus = "PENDING" | "APPROVED" | "REJECTED" | "REVOKED";

type Memory = {
  id: string;
  content: string;
  sourceContent: string;
  status: MemoryStatus;
  createdAt: string;
  updatedAt: string;
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
  const [memories, setMemories] = useState<Memory[]>([]);
  const [memoryEdits, setMemoryEdits] = useState<Record<string, string>>({});
  const [memoryDraftSource, setMemoryDraftSource] = useState<string | null>(null);
  const [memoryDraft, setMemoryDraft] = useState("");
  const [isProposingMemory, setIsProposingMemory] = useState(false);
  const [busyMemoryId, setBusyMemoryId] = useState<string | null>(null);
  const [isMemoryPanelOpen, setIsMemoryPanelOpen] = useState(false);
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
        return fetch(`/api/conversations/${encodeURIComponent(conversation.conversationId)}/memories`)
          .then(async (response) => {
            if (!response.ok) {
              throw new Error("Memory review is unavailable. Restart the Spring API to load the new endpoints.");
            }
            return response.json();
          })
          .then((savedMemories: Memory[]) => {
            if (!cancelled) setMemories(savedMemories);
          });
      })
      .catch((requestError: unknown) => {
        if (!cancelled) {
          setError(requestError instanceof Error ? requestError.message : "Could not load the saved conversation.");
        }
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
      const memoryResponse = await fetch(
        `/api/conversations/${encodeURIComponent(result.conversationId)}/memories`,
      );
      if (memoryResponse.ok) {
        setMemories(await memoryResponse.json());
      } else {
        setError("Your answer was saved, but memory review is unavailable. Restart the Spring API to load the new endpoints.");
      }
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
    setMemories([]);
    setMemoryEdits({});
    setMemoryDraftSource(null);
    window.localStorage.removeItem("rag-conversation-id");
  }

  async function submitMemoryProposal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!conversationId || !memoryDraftSource || !memoryDraft.trim() || isProposingMemory) return;

    setIsProposingMemory(true);
    setError("");
    try {
      const response = await fetch(
        `/api/conversations/${encodeURIComponent(conversationId)}/memories`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ content: memoryDraft, sourceContent: memoryDraftSource }),
        },
      );
      const result = await response.json();
      if (!response.ok) throw new Error(result.message ?? "Could not submit memory for review.");
      setMemories((current) => [result, ...current]);
      setMemoryDraftSource(null);
      setMemoryDraft("");
      setNotice("Memory submitted as a draft. It will not be used until you approve it.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not submit memory for review.");
    } finally {
      setIsProposingMemory(false);
    }
  }

  async function updateMemory(memory: Memory, action: string, method = "POST") {
    if (!conversationId || busyMemoryId) return;
    setBusyMemoryId(memory.id);
    setError("");
    try {
      const response = await fetch(
        `/api/conversations/${encodeURIComponent(conversationId)}/memories/${encodeURIComponent(memory.id)}${action ? `/${action}` : ""}`,
        {
          method,
          ...(method === "PUT"
            ? {
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ content: memoryEdits[memory.id] ?? memory.content }),
              }
            : {}),
        },
      );
      const result = method === "DELETE" ? null : await response.json();
      if (!response.ok) throw new Error(result?.message ?? "Could not update memory.");

      if (method === "DELETE") {
        setMemories((current) => current.filter((item) => item.id !== memory.id));
      } else {
        setMemories((current) => current.map((item) => item.id === memory.id ? result : item));
        setMemoryEdits((current) => {
          const next = { ...current };
          delete next[memory.id];
          return next;
        });
      }
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not update memory.");
    } finally {
      setBusyMemoryId(null);
    }
  }

  function handleQuestionKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      void sendQuestion();
    }
  }

  return (
    <main className="workspace">
      {isMemoryPanelOpen && <button className="sidebar-backdrop" type="button" aria-label="Close memory panel" onClick={() => setIsMemoryPanelOpen(false)} />}
      <aside className={`sidebar ${isMemoryPanelOpen ? "sidebar-open" : ""}`}>
        <Link className="brand" href="/" aria-label="Fieldnotes home">
          <span className="brand-mark">f.</span>
          <span className="brand-name">fieldnotes<span> / RAG</span></span>
        </Link>
        <button className="mobile-sidebar-close" type="button" onClick={() => setIsMemoryPanelOpen(false)}>Close review panel</button>

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

        <section className="memory-panel" aria-label="Reviewed memories">
          <div className="memory-panel-heading">
            <div>
              <p className="eyebrow">HUMAN REVIEW</p>
              <h2>Conversation memory</h2>
            </div>
            <span className="memory-count" aria-label="Approved memories">{memories.filter((memory) => memory.status === "APPROVED").length}</span>
          </div>
          {!conversationId ? (
            <p className="memory-empty">Start a conversation to propose a memory for review.</p>
          ) : memories.length === 0 ? (
            <p className="memory-empty">Nothing saved. Assistant replies are never added automatically.</p>
          ) : (
            <div className="memory-list">
              {memories.map((memory) => {
                const editedContent = memoryEdits[memory.id] ?? memory.content;
                const isBusy = busyMemoryId === memory.id;
                return (
                  <article className="memory-item" key={memory.id}>
                    <div className="memory-item-heading">
                      <span className={`memory-status status-${memory.status.toLowerCase()}`}>{memory.status}</span>
                    </div>
                    {memory.status === "PENDING" ? (
                      <textarea
                        aria-label="Edit proposed memory"
                        value={editedContent}
                        onChange={(event) => setMemoryEdits((current) => ({ ...current, [memory.id]: event.target.value }))}
                        rows={3}
                      />
                    ) : (
                      <p className="memory-content">{memory.content}</p>
                    )}
                    <details className="memory-source">
                      <summary>Review source passage</summary>
                      <p>{memory.sourceContent}</p>
                    </details>
                    <div className="memory-actions">
                      {memory.status === "PENDING" && (
                        <>
                          <button type="button" disabled={isBusy || !editedContent.trim()} onClick={() => void updateMemory(memory, "", "PUT")}>Save edit</button>
                          <button
                            className="approve-memory"
                            type="button"
                            disabled={isBusy || !editedContent.trim() || editedContent !== memory.content}
                            onClick={() => void updateMemory(memory, "approve")}
                          >Approve</button>
                          <button type="button" disabled={isBusy} onClick={() => void updateMemory(memory, "reject")}>Reject</button>
                        </>
                      )}
                      {memory.status === "APPROVED" && (
                        <button type="button" disabled={isBusy} onClick={() => void updateMemory(memory, "revoke")}>Revoke</button>
                      )}
                      <button className="delete-memory" type="button" disabled={isBusy} onClick={() => void updateMemory(memory, "", "DELETE")}>Delete</button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
          <p className="memory-policy">Only explicitly approved memories are included in later answers for this conversation.</p>
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
          <button className="mobile-memory-toggle" type="button" onClick={() => setIsMemoryPanelOpen(true)}>
            Memories {memories.filter((memory) => memory.status === "PENDING").length > 0 && `· ${memories.filter((memory) => memory.status === "PENDING").length}`}
          </button>
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
                      {conversationId && (
                        <button
                          className="propose-memory"
                          type="button"
                          onClick={() => {
                            setMemoryDraftSource(message.content);
                            setMemoryDraft("");
                          }}
                        >
                          Propose a memory from this message
                        </button>
                      )}
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
      {memoryDraftSource !== null && (
        <div className="memory-dialog-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !isProposingMemory) setMemoryDraftSource(null);
        }}>
          <section className="memory-dialog" role="dialog" aria-modal="true" aria-labelledby="memory-dialog-title">
            <button className="close-dialog" type="button" aria-label="Close memory proposal" onClick={() => setMemoryDraftSource(null)}>×</button>
            <p className="eyebrow">NOT SAVED UNTIL APPROVED</p>
            <h2 id="memory-dialog-title">Propose a conversation memory</h2>
            <p className="dialog-copy">Write the preference or fact you want remembered. This draft is not used in answers unless you approve it in the review panel.</p>
            <details className="dialog-source">
              <summary>Source passage</summary>
              <p>{memoryDraftSource}</p>
            </details>
            <form onSubmit={submitMemoryProposal}>
              <label htmlFor="memory-draft">Proposed memory</label>
              <textarea
                id="memory-draft"
                autoFocus
                required
                maxLength={4000}
                rows={4}
                value={memoryDraft}
                onChange={(event) => setMemoryDraft(event.target.value)}
                placeholder="For example: Prefers concise answers with a short summary first."
              />
              <div className="dialog-actions">
                <button type="button" onClick={() => setMemoryDraftSource(null)}>Cancel</button>
                <button className="approve-memory" type="submit" disabled={isProposingMemory || !memoryDraft.trim()}>
                  {isProposingMemory ? "Submitting..." : "Submit for review"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}
