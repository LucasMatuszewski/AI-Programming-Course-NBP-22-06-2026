import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ChatService } from './chat.service';
import { SessionState } from '../state/session-state.service';

describe('ChatService', () => {
  let service: ChatService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ChatService,
        SessionState,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ChatService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('stream() returns an Observable', () => {
    const obs = service.stream('sess-1', 'Pytanie');
    expect(obs).toBeTruthy();
    expect(typeof obs.subscribe).toBe('function');
  });

  it('parseSSELine() emits token chunk for "event: token\\ndata: hello"', () => {
    const chunks: string[] = [];
    let done = false;
    let error: string | null = null;

    service['parseSSELines'](
      ['event: token', 'data: hello', ''],
      (chunk) => chunks.push(chunk),
      () => { done = true; },
      (err) => { error = err; }
    );

    expect(chunks).toEqual(['hello']);
    expect(done).toBeFalse();
    expect(error).toBeNull();
  });

  it('parseSSELine() signals done on "event: done" line', () => {
    let done = false;
    service['parseSSELines'](
      ['event: done', 'data: ', ''],
      jasmine.createSpy('onToken'),
      () => { done = true; },
      jasmine.createSpy('onError')
    );
    expect(done).toBeTrue();
  });

  it('parseSSELine() signals error on "event: error" line', () => {
    let errorMsg: string | null = null;
    service['parseSSELines'](
      ['event: error', 'data: Błąd serwera', ''],
      jasmine.createSpy('onToken'),
      jasmine.createSpy('onDone'),
      (err) => { errorMsg = err; }
    );
    expect(errorMsg).toContain('Błąd');
  });
});

// ----------------------------------------------------------------
// SessionState rehydration via GET /api/cases/:id
// ----------------------------------------------------------------
describe('SessionState.rehydrate', () => {
  let state: SessionState;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SessionState,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    state = TestBed.inject(SessionState);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('rehydrate() sets sessionId and messages from GET response', () => {
    state.rehydrate({
      sessionId: 'sess-rehydrate',
      decision: {
        outcome: 'UZNANA',
        justification: 'OK',
        nextSteps: [],
        missingInfo: [],
      },
      messages: [
        { role: 'system', content: '## Decyzja', createdAt: '2026-01-01T00:00:00Z' },
        { role: 'user', content: 'Pytanie', createdAt: '2026-01-01T00:01:00Z' },
      ],
    });

    expect(state.sessionId()).toBe('sess-rehydrate');
    expect(state.messages().length).toBe(2);
    expect(state.messages()[0].role).toBe('system');
  });
});
