import { Injectable, signal, computed } from '@angular/core';
import { ChatMessage, CaseResult, Decision } from '../models/models';

/** Lightweight signal store for the current session (ADR-002 §3) */
@Injectable({ providedIn: 'root' })
export class SessionState {
  /** Active session identifier; null means no active session */
  readonly sessionId = signal<string | null>(null);

  /** The structured decision from createCase */
  readonly decision = signal<Decision | null>(null);

  /** Decision message markdown (first chat bubble) */
  readonly decisionMessageMarkdown = signal<string | null>(null);

  /** Full chat message list */
  readonly messages = signal<ChatMessage[]>([]);

  /** True when a session is active and has a decision */
  readonly hasActiveSession = computed(
    () => this.sessionId() !== null && this.decision() !== null
  );

  /** Set state from the createCase response */
  setFromCaseResult(result: CaseResult): void {
    this.sessionId.set(result.sessionId);
    this.decision.set(result.decision);
    this.decisionMessageMarkdown.set(result.decisionMessageMarkdown);
    this.messages.set([
      {
        role: 'system',
        content: result.decisionMessageMarkdown,
        createdAt: new Date().toISOString(),
      },
    ]);
  }

  /** Rehydrate from GET /api/cases/:id response */
  rehydrate(data: {
    sessionId: string;
    decision: Decision;
    messages: ChatMessage[];
  }): void {
    this.sessionId.set(data.sessionId);
    this.decision.set(data.decision);
    // First message is the system decision message; use its content for markdown
    const firstSystem = data.messages.find((m) => m.role === 'system');
    this.decisionMessageMarkdown.set(firstSystem?.content ?? null);
    this.messages.set(data.messages);
  }

  /** Append an assistant message chunk (streaming) */
  appendAssistantChunk(chunk: string): void {
    const msgs = this.messages();
    const last = msgs[msgs.length - 1];
    if (last?.role === 'assistant') {
      this.messages.set([
        ...msgs.slice(0, -1),
        { ...last, content: last.content + chunk },
      ]);
    } else {
      this.messages.set([
        ...msgs,
        { role: 'assistant', content: chunk, createdAt: new Date().toISOString() },
      ]);
    }
  }

  /** Append a user message */
  appendUserMessage(content: string): void {
    this.messages.set([
      ...this.messages(),
      { role: 'user', content, createdAt: new Date().toISOString() },
    ]);
  }

  /** Clear all session state */
  clear(): void {
    this.sessionId.set(null);
    this.decision.set(null);
    this.decisionMessageMarkdown.set(null);
    this.messages.set([]);
  }
}
