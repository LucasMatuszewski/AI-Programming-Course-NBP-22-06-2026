import {
  Component,
  signal,
  OnInit,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

import { EQUIPMENT_CATEGORIES, RequestType } from '../models/models';
import { CaseService } from '../services/case.service';
import { SessionState } from '../state/session-state.service';

const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
const MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

@Component({
  selector: 'app-intake-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './intake-form.component.html',
  styleUrl: './intake-form.component.scss',
})
export class IntakeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly caseService = inject(CaseService);
  private readonly sessionState = inject(SessionState);
  private readonly router = inject(Router);

  readonly categories = EQUIPMENT_CATEGORIES;
  readonly requestTypes: { value: RequestType; label: string }[] = [
    { value: 'COMPLAINT', label: 'Reklamacja (reklamacja)' },
    { value: 'RETURN', label: 'Zwrot (zwrot)' },
  ];

  readonly today = new Date();

  /** The reactive form */
  readonly form: FormGroup = this.fb.group({
    requestType: ['', Validators.required],
    category: ['', Validators.required],
    modelName: ['', Validators.required],
    purchaseDate: [null, Validators.required],
    reason: [''],
  });

  /** Currently selected image file */
  readonly selectedFile = signal<File | null>(null);

  /** Image validation error message (Polish) */
  readonly imageError = signal<string | null>(null);

  /** Object URL for thumbnail preview */
  readonly thumbnailUrl = signal<string | null>(null);

  /** Whether a submit is in flight */
  readonly isSubmitting = signal<boolean>(false);

  /** Submit / service error message (for retry panel) */
  readonly submitError = signal<string | null>(null);

  /** Reason field label — changes based on requestType */
  readonly reasonLabel = signal<string>('Opis (opcjonalny)');

  /** Reason field hint — changes based on requestType */
  readonly reasonHint = signal<string>('Możesz opisać powód zwrotu.');

  ngOnInit(): void {
    this.form.get('requestType')!.valueChanges.subscribe((type: RequestType) => {
      this.updateReasonValidation(type);
    });
  }

  private updateReasonValidation(type: RequestType): void {
    const reasonCtrl = this.form.get('reason')!;
    const currentValue = reasonCtrl.value as string;
    if (type === 'COMPLAINT') {
      reasonCtrl.setValidators([Validators.required]);
      this.reasonLabel.set('Opis defektu *');
      this.reasonHint.set('Wymagane. Opisz dokładnie widoczne uszkodzenie lub defekt.');
    } else {
      reasonCtrl.clearValidators();
      this.reasonLabel.set('Opis (opcjonalny)');
      this.reasonHint.set('Możesz opisać powód zwrotu.');
    }
    reasonCtrl.setValue(currentValue, { emitEvent: false });
    reasonCtrl.updateValueAndValidity({ emitEvent: false });
  }

  /** Handle file selected via input or drop */
  handleFileSelected(file: File): void {
    if (!ALLOWED_TYPES.includes(file.type)) {
      this.imageError.set('Dozwolone formaty: jpg, jpeg, png, webp.');
      this.selectedFile.set(null);
      this.thumbnailUrl.set(null);
      return;
    }
    if (file.size > MAX_SIZE_BYTES) {
      this.imageError.set('Plik jest zbyt duży. Maksymalny rozmiar to 10 MB.');
      this.selectedFile.set(null);
      this.thumbnailUrl.set(null);
      return;
    }
    this.imageError.set(null);
    this.selectedFile.set(file);
    const prev = this.thumbnailUrl();
    if (prev) URL.revokeObjectURL(prev);
    this.thumbnailUrl.set(URL.createObjectURL(file));
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) this.handleFileSelected(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.handleFileSelected(file);
  }

  removeImage(): void {
    const prev = this.thumbnailUrl();
    if (prev) URL.revokeObjectURL(prev);
    this.selectedFile.set(null);
    this.thumbnailUrl.set(null);
    this.imageError.set(null);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (!this.selectedFile()) {
      this.imageError.set('Zdjęcie jest wymagane.');
      return;
    }
    if (this.form.invalid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.submitError.set(null);

    const formData = this.buildFormData();

    this.caseService.createCase(formData).subscribe({
      next: (result) => {
        this.sessionState.setFromCaseResult(result);
        this.isSubmitting.set(false);
        this.router.navigate(['/chat', result.sessionId]);
      },
      error: (message: string) => {
        this.submitError.set(message);
        this.isSubmitting.set(false);
        // Form values are preserved — no reset
      },
    });
  }

  /** Retry after error — re-call onSubmit without resetting form */
  onRetry(): void {
    this.submitError.set(null);
    this.onSubmit();
  }

  private buildFormData(): FormData {
    const fd = new FormData();
    const v = this.form.value as {
      requestType: string;
      category: string;
      modelName: string;
      purchaseDate: Date;
      reason: string;
    };
    fd.append('requestType', v.requestType);
    fd.append('category', v.category);
    fd.append('modelName', v.modelName);
    // ISO date string (date-only)
    const d = v.purchaseDate;
    if (d) {
      const iso = d.toISOString().substring(0, 10);
      fd.append('purchaseDate', iso);
    }
    if (v.reason) fd.append('reason', v.reason);
    const file = this.selectedFile();
    if (file) fd.append('image', file, file.name);
    return fd;
  }

  resetSubmitting(): void {
    this.isSubmitting.set(false);
  }

  get submitDisabled(): boolean {
    return this.isSubmitting();
  }
}
