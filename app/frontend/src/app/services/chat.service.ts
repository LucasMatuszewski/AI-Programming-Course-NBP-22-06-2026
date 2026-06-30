import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SessionState } from '../state/session-state.service';
import { Decision, ChatMessage } from '../models/models';

export type StreamEvent =
  | { type: 'token'; chunk: string }
  | { type: 'done' }
  | { type: 'error'; message: string };

/**
 * ChatService — sends POST /api/cases/:id/messages and reads the
 * text/event-stream response via the Fetch ReadableStream reader,
 * parsing SSE frames client-side. (ADR-002 §6: POST + fetch SSE)
 */
@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = inject(SessionState);

  /**
   * Open an SSE stream for a chat turn.
   * Emits StreamEvent objects; completes after 'done' or errors.
   */
  stream(sessionId: string, content: string): Observable<StreamEvent> {
    return new Observable<StreamEvent>((subscriber) => {
      let cancelled = false;

      const run = async () => {
        let response: Response;
        try {
          response = await fetch(`/api/cases/${sessionId}/messages`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content }),
          });
        } catch {
          subscriber.error('Błąd połączenia z serwerem.');
          return;
        }

        if (!response.ok) {
          subscriber.error(
            `Błąd serwera (${response.status}). Spróbuj ponownie.`
          );
          return;
        }

        const reader = response.body?.getReader();
        if (!reader) {
          subscriber.error('Brak strumienia odpowiedzi.');
          return;
        }

        const decoder = new TextDecoder();
        let buffer = '';

        while (!cancelled) {
          let readResult: ReadableStreamReadResult<Uint8Array>;
          try {
            readResult = await reader.read();
          } catch {
            if (!cancelled) subscriber.error('Błąd odczytu strumienia.');
            break;
          }

          if (readResult.done) break;
          buffer += decoder.decode(readResult.value, { stream: true });

          // Split on double-newline (SSE event separator)
          const events = buffer.split('\n\n');
          buffer = events.pop() ?? '';

          for (const eventBlock of events) {
            const lines = eventBlock.split('\n').filter((l) => l.length > 0);
            this.parseSSELines(
              lines,
              (chunk) => subscriber.next({ type: 'token', chunk }),
              () => {
                subscriber.next({ type: 'done' });
                subscriber.complete();
              },
              (msg) => subscriber.error(msg)
            );
          }
        }
      };

      run().catch((err: unknown) => {
        if (!cancelled) {
          subscriber.error(String(err));
        }
      });

      // Teardown: cancel the fetch on unsubscribe
      return () => {
        cancelled = true;
      };
    });
  }

  /**
   * Parse a list of lines from a single SSE event block.
   * Calls onToken, onDone, or onError callbacks accordingly.
   * Exposed for unit testing.
   */
  parseSSELines(
    lines: string[],
    onToken: (chunk: string) => void,
    onDone: () => void,
    onError: (message: string) => void
  ): void {
    let eventName = '';
    let dataValue = '';

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice('event:'.length).trim();
      } else if (line.startsWith('data:')) {
        dataValue = line.slice('data:'.length).trim();
      }
    }

    switch (eventName) {
      case 'token':
        if (dataValue) onToken(dataValue);
        break;
      case 'done':
        onDone();
        break;
      case 'error':
        onError(dataValue || 'Nieznany błąd strumienia.');
        break;
    }
  }

  /**
   * Rehydrate session via GET /api/cases/:id.
   * On 404, returns false; on success populates SessionState and returns true.
   */
  rehydrate(sessionId: string): Observable<boolean> {
    return new Observable<boolean>((subscriber) => {
      this.http
        .get<{
          sessionId: string;
          decision: Decision;
          messages: ChatMessage[];
        }>(`/api/cases/${sessionId}`)
        .subscribe({
          next: (data) => {
            this.sessionState.rehydrate(data);
            subscriber.next(true);
            subscriber.complete();
          },
          error: () => {
            subscriber.next(false);
            subscriber.complete();
          },
        });
    });
  }
}
