import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CaseResult, ApiError } from '../models/models';

/** Polish user-facing messages keyed by API error code */
const ERROR_MESSAGES: Record<string, string> = {
  VALIDATION_ERROR:
    'Sprawdź wprowadzone dane. Formularz zawiera błędy walidacji.',
  UNSUPPORTED_IMAGE_TYPE:
    'Nieobsługiwany format zdjęcia. Dozwolone formaty: jpg, jpeg, png, webp.',
  IMAGE_TOO_LARGE:
    'Zdjęcie jest zbyt duże. Maksymalny rozmiar pliku to 10 MB.',
  LLM_UPSTREAM_ERROR:
    'Przepraszamy — zewnętrzna usługa AI jest chwilowo niedostępna. Spróbuj ponownie.',
  LLM_TIMEOUT:
    'Przekroczono czas oczekiwania na odpowiedź AI. Prosimy spróbować ponownie za chwilę.',
  SESSION_NOT_FOUND:
    'Nie znaleziono sesji. Zacznij nowe zgłoszenie.',
  DEFAULT:
    'Wystąpił nieoczekiwany błąd. Spróbuj ponownie.',
};

function mapError(response: HttpErrorResponse): string {
  const body = response.error as Partial<ApiError> | null;
  const code = body?.code ?? '';
  if (code && ERROR_MESSAGES[code]) {
    return ERROR_MESSAGES[code];
  }
  // Fallback by HTTP status
  switch (response.status) {
    case 400: return ERROR_MESSAGES['VALIDATION_ERROR'];
    case 413: return ERROR_MESSAGES['IMAGE_TOO_LARGE'];
    case 415: return ERROR_MESSAGES['UNSUPPORTED_IMAGE_TYPE'];
    case 502: return ERROR_MESSAGES['LLM_UPSTREAM_ERROR'];
    case 504: return ERROR_MESSAGES['LLM_TIMEOUT'];
    default:  return ERROR_MESSAGES['DEFAULT'];
  }
}

@Injectable({ providedIn: 'root' })
export class CaseService {
  private readonly http = inject(HttpClient);

  /**
   * POST /api/cases with FormData.
   * On success emits CaseResult.
   * On error emits a Polish user-facing string via throwError.
   */
  createCase(formData: FormData): Observable<CaseResult> {
    return this.http
      .post<CaseResult>('/api/cases', formData)
      .pipe(catchError((err: HttpErrorResponse) => throwError(() => mapError(err))));
  }
}
