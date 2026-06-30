import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Helper service for showing non-technical Polish error toasts */
@Injectable({ providedIn: 'root' })
export class ErrorService {
  private readonly snackBar = inject(MatSnackBar);

  showError(message: string, action = 'Zamknij'): void {
    this.snackBar.open(message, action, {
      duration: 8000,
      panelClass: ['nbp-snack-error'],
      verticalPosition: 'top',
    });
  }

  showSuccess(message: string, action = 'OK'): void {
    this.snackBar.open(message, action, {
      duration: 4000,
      panelClass: ['nbp-snack-success'],
      verticalPosition: 'top',
    });
  }
}
