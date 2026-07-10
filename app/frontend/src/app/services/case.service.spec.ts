import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CaseService } from './case.service';
import { CaseResult } from '../models/models';

const MOCK_RESULT: CaseResult = {
  sessionId: 'sess-abc',
  outcome: 'UZNANA',
  decisionMessageMarkdown: '## Decyzja',
  decision: {
    outcome: 'UZNANA',
    justification: 'OK',
    nextSteps: [],
    missingInfo: [],
  },
};

describe('CaseService', () => {
  let service: CaseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CaseService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CaseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should POST FormData to /api/cases and return CaseResult', () => {
    const formData = new FormData();
    formData.append('requestType', 'COMPLAINT');

    let result: CaseResult | undefined;
    service.createCase(formData).subscribe((r) => (result = r));

    const req = httpMock.expectOne('/api/cases');
    expect(req.request.method).toBe('POST');
    req.flush(MOCK_RESULT, { status: 201, statusText: 'Created' });

    expect(result).toEqual(MOCK_RESULT);
  });

  it('should map VALIDATION_ERROR (400) to a Polish error message', () => {
    const formData = new FormData();
    let errorMessage: string | undefined;
    service.createCase(formData).subscribe({
      error: (msg: string) => (errorMessage = msg),
    });

    const req = httpMock.expectOne('/api/cases');
    req.flush(
      { code: 'VALIDATION_ERROR', message: 'Błąd walidacji', fieldErrors: {} },
      { status: 400, statusText: 'Bad Request' }
    );
    expect(errorMessage).toContain('Sprawdź');
  });

  it('should map UNSUPPORTED_IMAGE_TYPE (415) to a Polish error', () => {
    const formData = new FormData();
    let errorMessage: string | undefined;
    service.createCase(formData).subscribe({
      error: (msg: string) => (errorMessage = msg),
    });

    const req = httpMock.expectOne('/api/cases');
    req.flush(
      { code: 'UNSUPPORTED_IMAGE_TYPE', message: 'Nieobsługiwany typ pliku' },
      { status: 415, statusText: 'Unsupported Media Type' }
    );
    expect(errorMessage).toContain('format');
  });

  it('should map IMAGE_TOO_LARGE (413) to a Polish error', () => {
    const formData = new FormData();
    let errorMessage: string | undefined;
    service.createCase(formData).subscribe({
      error: (msg: string) => (errorMessage = msg),
    });

    const req = httpMock.expectOne('/api/cases');
    req.flush(
      { code: 'IMAGE_TOO_LARGE', message: 'Plik zbyt duży' },
      { status: 413, statusText: 'Payload Too Large' }
    );
    expect(errorMessage).toContain('10 MB');
  });

  it('should map LLM_UPSTREAM_ERROR (502) to a Polish error', () => {
    const formData = new FormData();
    let errorMessage: string | undefined;
    service.createCase(formData).subscribe({
      error: (msg: string) => (errorMessage = msg),
    });

    const req = httpMock.expectOne('/api/cases');
    req.flush(
      { code: 'LLM_UPSTREAM_ERROR', message: 'Błąd upstream' },
      { status: 502, statusText: 'Bad Gateway' }
    );
    expect(errorMessage).toContain('usługa');
  });

  it('should map LLM_TIMEOUT (504) to a Polish error', () => {
    const formData = new FormData();
    let errorMessage: string | undefined;
    service.createCase(formData).subscribe({
      error: (msg: string) => (errorMessage = msg),
    });

    const req = httpMock.expectOne('/api/cases');
    req.flush(
      { code: 'LLM_TIMEOUT', message: 'Timeout' },
      { status: 504, statusText: 'Gateway Timeout' }
    );
    expect(errorMessage).toContain('czas');
  });
});
