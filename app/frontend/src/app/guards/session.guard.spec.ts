import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { sessionGuard } from './session.guard';
import { SessionState } from '../state/session-state.service';

function runGuard(): unknown {
  return TestBed.runInInjectionContext(() => sessionGuard({} as never, {} as never));
}

describe('sessionGuard', () => {
  let state: SessionState;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SessionState,
        {
          provide: Router,
          useValue: {
            createUrlTree: (commands: unknown[]) => ({ commands } as unknown as UrlTree),
          },
        },
      ],
    });
    state = TestBed.inject(SessionState);
  });

  it('should redirect to / when there is no active session', () => {
    const result = runGuard();
    expect(result).not.toBe(true);
    const tree = result as { commands: unknown[] };
    expect(tree.commands).toEqual(['/']);
  });

  it('should allow navigation when a session is active', () => {
    state.setFromCaseResult({
      sessionId: 'sess-1',
      outcome: 'UZNANA',
      decisionMessageMarkdown: '## Decision',
      decision: {
        outcome: 'UZNANA',
        justification: 'OK',
        nextSteps: [],
        missingInfo: [],
      },
    });
    const result = runGuard();
    expect(result).toBe(true);
  });
});
