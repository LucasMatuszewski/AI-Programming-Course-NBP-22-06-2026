import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideMarkdown } from 'ngx-markdown';
import { ChatComponent } from './chat.component';
import { SessionState } from '../state/session-state.service';
import { routes } from '../app.routes';
import { OutcomeValue } from '../models/models';

async function setupWithOutcome(
  outcome: OutcomeValue
): Promise<{ fixture: ComponentFixture<ChatComponent>; state: SessionState }> {
  await TestBed.configureTestingModule({
    imports: [ChatComponent],
    providers: [
      SessionState,
      provideRouter(routes),
      provideHttpClient(),
      provideAnimationsAsync(),
      provideMarkdown(),
    ],
  }).compileComponents();

  const state = TestBed.inject(SessionState);
  state.setFromCaseResult({
    sessionId: 'sess-test',
    outcome,
    decisionMessageMarkdown: `## Decyzja\nUznana\n\n- Krok 1\n- Krok 2`,
    decision: {
      outcome,
      justification: 'Widoczne uszkodzenie zgodne z polityką.',
      nextSteps: ['Krok 1', 'Krok 2'],
      missingInfo: [],
    },
  });

  const fixture = TestBed.createComponent(ChatComponent);
  fixture.detectChanges();
  await fixture.whenStable();
  fixture.detectChanges();
  return { fixture, state };
}

describe('ChatComponent', () => {
  // ----------------------------------------------------------------
  // TAC-204: first bubble renders markdown + status chip
  // ----------------------------------------------------------------
  describe('first decision bubble', () => {
    it('renders at least one message bubble when session has a decision', async () => {
      const { fixture } = await setupWithOutcome('UZNANA');
      const compiled = fixture.nativeElement as HTMLElement;
      const bubbles = compiled.querySelectorAll('.chat-bubble');
      expect(bubbles.length).toBeGreaterThan(0);
    });

    it('shows a status chip for UZNANA outcome (success color class)', async () => {
      const { fixture } = await setupWithOutcome('UZNANA');
      const compiled = fixture.nativeElement as HTMLElement;
      const chip = compiled.querySelector('.status-chip');
      expect(chip).toBeTruthy();
      expect(chip?.classList.contains('status-chip--success')).toBeTrue();
    });

    it('shows a status chip for ODRZUCONA outcome (error color class)', async () => {
      const { fixture } = await setupWithOutcome('ODRZUCONA');
      const compiled = fixture.nativeElement as HTMLElement;
      const chip = compiled.querySelector('.status-chip--error');
      expect(chip).toBeTruthy();
    });

    it('shows a status chip for WYMAGA_WERYFIKACJI outcome (verification color class)', async () => {
      const { fixture } = await setupWithOutcome('WYMAGA_WERYFIKACJI');
      const compiled = fixture.nativeElement as HTMLElement;
      const chip = compiled.querySelector('.status-chip--verification');
      expect(chip).toBeTruthy();
    });

    it('shows a status chip for PRZYJETY_DO_ODSPRZEDAZY outcome (success color class)', async () => {
      const { fixture } = await setupWithOutcome('PRZYJETY_DO_ODSPRZEDAZY');
      const compiled = fixture.nativeElement as HTMLElement;
      const chip = compiled.querySelector('.status-chip--success');
      expect(chip).toBeTruthy();
    });
  });

  // ----------------------------------------------------------------
  // Polish text
  // ----------------------------------------------------------------
  describe('Polish UI text', () => {
    it('contains a "Nowe zgłoszenie" action', async () => {
      const { fixture } = await setupWithOutcome('UZNANA');
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Nowe zgłoszenie');
    });

    it('contains a Polish send button', async () => {
      const { fixture } = await setupWithOutcome('UZNANA');
      const compiled = fixture.nativeElement as HTMLElement;
      const text = compiled.textContent ?? '';
      expect(text.includes('Wyślij') || text.includes('Napisz') || text.includes('wysyłania')).toBeTrue();
    });
  });

  // ----------------------------------------------------------------
  // Streaming state
  // ----------------------------------------------------------------
  describe('streaming state', () => {
    it('isStreaming() is false initially', async () => {
      const { fixture } = await setupWithOutcome('UZNANA');
      expect(fixture.componentInstance.isStreaming()).toBe(false);
    });
  });
});
