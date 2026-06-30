import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { IntakeFormComponent } from './intake-form.component';
import { CaseService } from '../services/case.service';
import { SessionState } from '../state/session-state.service';
import { routes } from '../app.routes';
import { of, throwError, Subject } from 'rxjs';
import { CaseResult } from '../models/models';

const MOCK_RESULT: CaseResult = {
  sessionId: 'sess-999',
  outcome: 'UZNANA',
  decisionMessageMarkdown: '## Decyzja\nUznana',
  decision: {
    outcome: 'UZNANA',
    justification: 'OK',
    nextSteps: ['Następny krok'],
    missingInfo: [],
  },
};

async function createComponent(): Promise<{
  fixture: ComponentFixture<IntakeFormComponent>;
  caseService: jasmine.SpyObj<CaseService>;
  router: Router;
  state: SessionState;
}> {
  const caseServiceSpy = jasmine.createSpyObj<CaseService>('CaseService', ['createCase']);

  await TestBed.configureTestingModule({
    imports: [IntakeFormComponent, ReactiveFormsModule],
    providers: [
      { provide: CaseService, useValue: caseServiceSpy },
      SessionState,
      provideRouter(routes),
      provideHttpClient(),
      provideHttpClientTesting(),
      provideAnimationsAsync(),
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(IntakeFormComponent);
  fixture.detectChanges();
  return {
    fixture,
    caseService: caseServiceSpy,
    router: TestBed.inject(Router),
    state: TestBed.inject(SessionState),
  };
}

/** Fill the form with valid values and attach a file */
function fillValidForm(fixture: ComponentFixture<IntakeFormComponent>): void {
  const comp = fixture.componentInstance;
  const past = new Date('2024-01-15');
  comp.form.patchValue({
    requestType: 'RETURN',
    category: 'SMARTPHONES',
    modelName: 'Samsung A53',
    purchaseDate: past,
    reason: '',
  });
  const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
  comp.handleFileSelected(file);
  fixture.detectChanges();
}

describe('IntakeFormComponent — submit wiring (step 4.6)', () => {
  // ----------------------------------------------------------------
  // TAC-204: successful submit navigates + sets SessionState
  // ----------------------------------------------------------------
  it('navigates to /chat/:sessionId on successful submit', async () => {
    const { fixture, caseService, router } = await createComponent();
    caseService.createCase.and.returnValue(of(MOCK_RESULT));
    fillValidForm(fixture);

    const navigateSpy = spyOn(router, 'navigate');
    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    expect(caseService.createCase).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/chat', 'sess-999']);
  });

  it('sets SessionState from CaseResult after successful submit', async () => {
    const { fixture, caseService, state } = await createComponent();
    caseService.createCase.and.returnValue(of(MOCK_RESULT));
    fillValidForm(fixture);

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    expect(state.sessionId()).toBe('sess-999');
    expect(state.decision()?.outcome).toBe('UZNANA');
  });

  it('shows processing text during submit (isSubmitting = true)', async () => {
    const { fixture, caseService } = await createComponent();
    // Return a never-completing observable to keep loading state
    caseService.createCase.and.returnValue(new Subject<CaseResult>().asObservable());
    fillValidForm(fixture);
    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(fixture.componentInstance.isSubmitting()).toBeTrue();
    expect(compiled.textContent).toContain('Analizujemy');
  });

  // ----------------------------------------------------------------
  // AC-23: error shows retry panel, form values preserved
  // ----------------------------------------------------------------
  it('shows submit error and resets isSubmitting on failure', async () => {
    const { fixture, caseService } = await createComponent();
    caseService.createCase.and.returnValue(throwError(() => 'Błąd usługi AI.'));
    fillValidForm(fixture);

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    expect(fixture.componentInstance.isSubmitting()).toBeFalse();
    expect(fixture.componentInstance.submitError()).toContain('Błąd');
  });

  it('preserves form values after a failed submit', async () => {
    const { fixture, caseService } = await createComponent();
    caseService.createCase.and.returnValue(throwError(() => 'Błąd usługi AI.'));
    fillValidForm(fixture);

    fixture.componentInstance.onSubmit();
    fixture.detectChanges();

    // modelName should still be set
    expect(fixture.componentInstance.form.get('modelName')?.value).toBe('Samsung A53');
  });

  // ----------------------------------------------------------------
  // Duplicate submit prevention
  // ----------------------------------------------------------------
  it('does not call createCase a second time when isSubmitting is true', async () => {
    const { fixture, caseService } = await createComponent();
    caseService.createCase.and.returnValue(new Subject<CaseResult>().asObservable());
    fillValidForm(fixture);

    fixture.componentInstance.onSubmit();
    fixture.componentInstance.onSubmit(); // second call — should be ignored
    fixture.detectChanges();

    expect(caseService.createCase).toHaveBeenCalledTimes(1);
  });
});
