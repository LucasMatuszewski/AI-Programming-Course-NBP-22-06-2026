import { Page } from '@playwright/test';
import * as path from 'path';

/**
 * Path to the test fixture image (small valid PNG, in the e2e package root).
 * Must be within the Playwright allowed roots.
 */
export const TEST_IMAGE_PATH = path.join(__dirname, '..', 'fixtures', 'test-image.png');

/**
 * Fills the intake form fields for a RETURN request.
 *
 * Angular Material's mat-select and mat-datepicker do not accept standard HTML
 * fill()/select() without special handling. This helper uses role-based locators
 * and programmatic Angular form-control access where needed.
 *
 * @param page     Playwright page
 * @param opts     Form field overrides
 */
export async function fillReturnForm(
  page: Page,
  opts: {
    requestType?: 'RETURN' | 'COMPLAINT';
    categoryLabel?: string;
    modelName?: string;
    purchaseDateOffset?: number; // days before today (negative = past)
    reason?: string;
  } = {}
) {
  const {
    requestType = 'RETURN',
    categoryLabel = 'Laptopy / Komputery',
    modelName = 'Test Laptop E2E',
    purchaseDateOffset = -30,
    reason,
  } = opts;

  // --- Request type ---
  const requestTypeLabel = requestType === 'RETURN' ? 'Zwrot (zwrot)' : 'Reklamacja (reklamacja)';
  await page.getByRole('combobox', { name: 'Typ zgłoszenia *' }).click();
  await page.getByRole('option', { name: requestTypeLabel }).click();

  // --- Category ---
  // mat-select label intercepts pointer — click via JS to avoid overlay blocking
  await page.evaluate(() => {
    const el = document.querySelector<HTMLElement>('mat-select[formcontrolname="category"]');
    if (el) el.click();
  });
  await page.getByRole('option', { name: categoryLabel }).click();

  // --- Model name ---
  await page.getByRole('textbox', { name: 'Model / Nazwa urządzenia *' }).fill(modelName);

  // --- Purchase date: set via Angular form control (ng.getComponent) ---
  // The mat-datepicker only accepts a proper Date object internally; filling the
  // text input with a locale string does not reliably parse on all platforms.
  const offsetMs = purchaseDateOffset * 24 * 60 * 60 * 1000;
  await page.evaluate((offsetMs: number) => {
    const comp = (window as any)['ng']?.getComponent(
      document.querySelector('app-intake-form')
    );
    if (!comp) throw new Error('IntakeFormComponent not found via ng.getComponent');
    const date = new Date(Date.now() + offsetMs);
    comp.form.get('purchaseDate').setValue(date);
    comp.form.get('purchaseDate').markAsTouched();
    comp.form.get('purchaseDate').updateValueAndValidity();
  }, offsetMs);

  // --- Reason (optional) ---
  if (reason !== undefined) {
    await page.getByRole('textbox', { name: /Opis/ }).fill(reason);
  }
}

/**
 * Uploads a test image via the hidden file input.
 * The input is made temporarily visible before triggering the file chooser.
 */
export async function uploadTestImage(page: Page, imagePath: string = TEST_IMAGE_PATH) {
  // Make the hidden file input interactable
  await page.evaluate(() => {
    const input = document.querySelector<HTMLInputElement>('input[type="file"]');
    if (input) {
      input.style.display = 'block';
      input.style.opacity = '1';
      input.style.position = 'static';
      input.style.pointerEvents = 'auto';
    }
  });

  const [fileChooser] = await Promise.all([
    page.waitForEvent('filechooser'),
    page.locator('input[type="file"]').click(),
  ]);
  await fileChooser.setFiles(imagePath);
}

/**
 * Returns today's date minus `days` days, formatted as ISO 8601 (yyyy-MM-dd).
 */
export function pastDate(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().substring(0, 10);
}

/**
 * Returns tomorrow's date as ISO 8601 string (yyyy-MM-dd).
 */
export function tomorrowDate(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().substring(0, 10);
}
