import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { IntakeFormComponent } from './intake-form.component';
import { routes } from '../app.routes';

async function createComponent(): Promise<ComponentFixture<IntakeFormComponent>> {
  await TestBed.configureTestingModule({
    imports: [IntakeFormComponent, ReactiveFormsModule],
    providers: [
      provideRouter(routes),
      provideHttpClient(),
      provideAnimationsAsync(),
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(IntakeFormComponent);
  fixture.detectChanges();
  return fixture;
}

describe('IntakeFormComponent', () => {
  // ----------------------------------------------------------------
  // TAC-201: reason required toggling
  // ----------------------------------------------------------------
  describe('reason field validation', () => {
    it('reason is NOT required when requestType = RETURN', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      comp.form.patchValue({ requestType: 'RETURN', reason: '' });
      fixture.detectChanges();
      expect(comp.form.get('reason')?.errors?.['required']).toBeFalsy();
    });

    it('reason IS required when requestType = COMPLAINT', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      comp.form.patchValue({ requestType: 'COMPLAINT', reason: '' });
      fixture.detectChanges();
      expect(comp.form.get('reason')?.errors?.['required']).toBeTruthy();
    });

    it('pre-typed reason value is preserved when switching from RETURN to COMPLAINT', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      comp.form.patchValue({ requestType: 'RETURN', reason: 'Opis problemu' });
      fixture.detectChanges();
      comp.form.patchValue({ requestType: 'COMPLAINT' });
      fixture.detectChanges();
      expect(comp.form.get('reason')?.value).toBe('Opis problemu');
    });
  });

  // ----------------------------------------------------------------
  // TAC-202: future date blocked
  // ----------------------------------------------------------------
  describe('purchase date validation', () => {
    it('a past date is valid', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      const past = new Date();
      past.setFullYear(past.getFullYear() - 1);
      comp.form.patchValue({ purchaseDate: past });
      fixture.detectChanges();
      expect(comp.form.get('purchaseDate')?.errors?.['matDatepickerMax']).toBeFalsy();
    });

    it('today is valid', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      comp.form.patchValue({ purchaseDate: new Date() });
      fixture.detectChanges();
      expect(comp.form.get('purchaseDate')?.errors?.['matDatepickerMax']).toBeFalsy();
    });
  });

  // ----------------------------------------------------------------
  // TAC-203: image type/size client check
  // ----------------------------------------------------------------
  describe('image validation', () => {
    it('rejects a .gif file and sets imageError', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      const gifFile = new File(['data'], 'test.gif', { type: 'image/gif' });
      comp.handleFileSelected(gifFile);
      fixture.detectChanges();
      expect(comp.imageError()).toContain('jpg');
    });

    it('rejects a file larger than 10 MB and sets imageError', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      // 11 MB
      const bigFile = new File([new ArrayBuffer(11 * 1024 * 1024)], 'big.jpg', {
        type: 'image/jpeg',
      });
      comp.handleFileSelected(bigFile);
      fixture.detectChanges();
      expect(comp.imageError()).toContain('10 MB');
    });

    it('accepts a valid jpg file', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      const jpgFile = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });
      comp.handleFileSelected(jpgFile);
      fixture.detectChanges();
      expect(comp.imageError()).toBeNull();
      expect(comp.selectedFile()).toBe(jpgFile);
    });

    it('accepts a valid png file up to 10 MB exactly', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      const pngFile = new File([new ArrayBuffer(10 * 1024 * 1024)], 'photo.png', {
        type: 'image/png',
      });
      comp.handleFileSelected(pngFile);
      fixture.detectChanges();
      expect(comp.imageError()).toBeNull();
    });
  });

  // ----------------------------------------------------------------
  // Polish labels
  // ----------------------------------------------------------------
  describe('Polish UI text', () => {
    it('renders the form with Polish labels', async () => {
      const fixture = await createComponent();
      const compiled = fixture.nativeElement as HTMLElement;
      const text = compiled.textContent ?? '';
      expect(text).toContain('Typ zgłoszenia');
      expect(text).toContain('Kategoria sprzętu');
      expect(text).toContain('Wyślij zgłoszenie');
    });
  });

  // ----------------------------------------------------------------
  // Duplicate submit prevention
  // ----------------------------------------------------------------
  describe('duplicate submit prevention', () => {
    it('isSubmitting() is false initially', async () => {
      const fixture = await createComponent();
      const comp = fixture.componentInstance;
      expect(comp.isSubmitting()).toBe(false);
    });
  });
});
