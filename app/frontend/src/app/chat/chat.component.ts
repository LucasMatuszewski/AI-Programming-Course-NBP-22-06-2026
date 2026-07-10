import {
  Component,
  signal,
  inject,
  OnInit,
  AfterViewChecked,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MarkdownModule } from 'ngx-markdown';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { SessionState } from '../state/session-state.service';
import { ChatService } from '../services/chat.service';
import { ChatMessage, OutcomeValue } from '../models/models';

interface StatusChipConfig {
  label: string;
  cssClass: string;
  icon: string;
}

const OUTCOME_CONFIG: Record<OutcomeValue, StatusChipConfig> = {
  UZNANA: {
    label: 'Uznana',
    cssClass: 'status-chip--success',
    icon: 'check_circle',
  },
  PRZYJETY_DO_ODSPRZEDAZY: {
    label: 'Przyjęty do odsprzedaży',
    cssClass: 'status-chip--success',
    icon: 'check_circle',
  },
  ODRZUCONA: {
    label: 'Odrzucona',
    cssClass: 'status-chip--error',
    icon: 'cancel',
  },
  WYMAGA_WERYFIKACJI: {
    label: 'Wymaga weryfikacji',
    cssClass: 'status-chip--verification',
    icon: 'pending',
  },
};

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MarkdownModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent implements OnInit, AfterViewChecked {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly sessionState = inject(SessionState);
  private readonly chatService = inject(ChatService);

  @ViewChild('scrollAnchor') scrollAnchor!: ElementRef<HTMLElement>;

  /** Whether an SSE stream is currently in flight */
  readonly isStreaming = signal<boolean>(false);

  /** Inline stream error message */
  readonly streamError = signal<string | null>(null);

  /** Current user input value */
  messageInput = '';

  ngOnInit(): void {
    // Attempt rehydration when session state is missing (e.g. after page refresh)
    if (!this.sessionState.hasActiveSession()) {
      const sessionId = this.route.snapshot.paramMap.get('sessionId');
      if (sessionId) {
        this.chatService.rehydrate(sessionId).subscribe((found) => {
          if (!found) {
            this.router.navigate(['/']);
          }
        });
      } else {
        this.router.navigate(['/']);
      }
    }
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    if (this.scrollAnchor) {
      this.scrollAnchor.nativeElement.scrollIntoView({ behavior: 'smooth' });
    }
  }

  get messages(): ChatMessage[] {
    return this.sessionState.messages();
  }

  get decision() {
    return this.sessionState.decision();
  }

  getStatusChip(outcome: OutcomeValue): StatusChipConfig {
    return OUTCOME_CONFIG[outcome] ?? OUTCOME_CONFIG['WYMAGA_WERYFIKACJI'];
  }

  isFirstSystemMessage(msg: ChatMessage, index: number): boolean {
    return index === 0 && msg.role === 'system';
  }

  onNewCase(): void {
    this.sessionState.clear();
    this.router.navigate(['/']);
  }

  onSendMessage(): void {
    const content = this.messageInput.trim();
    if (!content || this.isStreaming()) return;
    const sessionId = this.sessionState.sessionId();
    if (!sessionId) return;

    this.messageInput = '';
    this.streamError.set(null);
    this.sessionState.appendUserMessage(content);
    this.isStreaming.set(true);

    this.chatService.stream(sessionId, content).subscribe({
      next: (event) => {
        if (event.type === 'token') {
          this.sessionState.appendAssistantChunk(event.chunk);
        } else if (event.type === 'done') {
          this.isStreaming.set(false);
        }
      },
      error: (msg: string) => {
        this.streamError.set(msg || 'Wystąpił błąd podczas odbierania odpowiedzi.');
        this.isStreaming.set(false);
      },
      complete: () => {
        this.isStreaming.set(false);
      },
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.onSendMessage();
    }
  }
}
