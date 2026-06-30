/**
 * Validation and error-path E2E tests.
 *
 * Verifies that client-side and server-side validation prevents
 * bad requests from reaching the LLM (AC-09 / TAC-101).
 *
 * Runs against the real stack.
 * @see docs/ADR/001-backend-api.md §5 — Interface Contracts
 */
import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { fillReturnForm, uploadTestImage, TEST_IMAGE_PATH } from '../fixtures/intake-helpers';

// ─── Tests ────────────────────────────────────────────────────────────────────

test.describe('Validation — COMPLAINT requires reason', () => {
  /**
   * AC-09 / TAC-201:
   * Submitting COMPLAINT without reason is blocked client-side:
   *   - inline error visible for reason field
   *   - page does NOT navigate away (no /chat route)
   *   - no HTTP request to /api/cases
   */
  test('missing reason for COMPLAINT shows inline error, no navigation', async ({ page }) => {
    await page.goto('/');

    // Select COMPLAINT but skip reason
    await fillReturnForm(page, {
      requestType: 'COMPLAINT',
      categoryLabel: 'Laptopy / Komputery',
      modelName: 'Test Laptop Validation',
      purchaseDateOffset: -10,
      // reason intentionally omitted
    });
    await uploadTestImage(page, TEST_IMAGE_PATH);

    // Intercept any /api/cases requests to assert none are made
    let apiCallMade = false;
    await page.route('**/api/cases', async (route) => {
      apiCallMade = true;
      await route.continue();
    });

    // Attempt submit
    await page.getByRole('button', { name: 'Wyślij zgłoszenie' }).click();

    // URL must NOT have changed to /chat
    await page.waitForTimeout(1_000); // brief wait to ensure no navigation happened
    expect(page.url()).not.toMatch(/\/chat\//);

    // Inline validation error for reason field must be visible (Polish)
    await expect(page.getByText('Opis defektu jest wymagany dla reklamacji.')).toBeVisible();

    // No HTTP call to /api/cases
    expect(apiCallMade).toBe(false);
  });
});

test.describe('Validation — future purchase date', () => {
  /**
   * TAC-202: Future date is blocked by the datepicker (max=today).
   * Attempting to set a future date should show an inline error.
   */
  test('future purchaseDate is blocked by datepicker', async ({ page }) => {
    await page.goto('/');

    // Fill all fields except date
    await fillReturnForm(page, {
      requestType: 'RETURN',
      categoryLabel: 'Tablety',
      modelName: 'Test Tablet Future',
      purchaseDateOffset: -10, // start with a valid date
    });

    // Now override with a FUTURE date via Angular form control
    await page.evaluate(() => {
      const comp = (window as any)['ng']?.getComponent(
        document.querySelector('app-intake-form')
      );
      if (!comp) throw new Error('IntakeFormComponent not found');
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      comp.form.get('purchaseDate').setValue(tomorrow);
      comp.form.get('purchaseDate').markAsTouched();
      comp.form.get('purchaseDate').updateValueAndValidity();
    });

    await uploadTestImage(page, TEST_IMAGE_PATH);

    let apiCallMade = false;
    await page.route('**/api/cases', async (route) => {
      apiCallMade = true;
      await route.continue();
    });

    await page.getByRole('button', { name: 'Wyślij zgłoszenie' }).click();

    // No navigation to chat
    await page.waitForTimeout(1_000);
    expect(page.url()).not.toMatch(/\/chat\//);

    // Inline date error (Polish) — either from mat-datepicker or our cross-field check
    const errorVisible =
      (await page.getByText('Data zakupu nie może być w przyszłości.').isVisible()) ||
      (await page.getByText(/przyszłości/).isVisible());
    expect(errorVisible).toBe(true);

    expect(apiCallMade).toBe(false);
  });
});

test.describe('Validation — image type', () => {
  /**
   * TAC-203: Non-allowed image type (.gif or .txt) shows inline error,
   * no navigation, no HTTP request.
   */
  test('uploading disallowed image type shows inline error', async ({ page }) => {
    await page.goto('/');

    await fillReturnForm(page, {
      requestType: 'RETURN',
      categoryLabel: 'Audio (słuchawki, głośniki)',
      modelName: 'Test Audio Device',
      purchaseDateOffset: -5,
    });

    // Create a .gif file in the allowed e2e root
    const gifPath = path.join(
      __dirname, '..', 'fixtures', 'test-bad.gif'
    );
    // (fixture must exist — see fixtures/test-bad.gif)

    // Make file input visible
    await page.evaluate(() => {
      const input = document.querySelector<HTMLInputElement>('input[type="file"]');
      if (input) {
        input.style.display = 'block';
        input.style.opacity = '1';
        input.style.position = 'static';
        input.style.pointerEvents = 'auto';
        // Remove the accept attribute so we can upload a .gif
        input.removeAttribute('accept');
      }
    });

    const [fileChooser] = await Promise.all([
      page.waitForEvent('filechooser'),
      page.locator('input[type="file"]').click(),
    ]);
    await fileChooser.setFiles(gifPath);

    // Inline error for image type (Polish)
    await expect(page.getByText('Dozwolone formaty: jpg, jpeg, png, webp.')).toBeVisible();

    // Submit should NOT navigate
    let apiCallMade = false;
    await page.route('**/api/cases', async (route) => {
      apiCallMade = true;
      await route.continue();
    });

    await page.getByRole('button', { name: 'Wyślij zgłoszenie' }).click();
    await page.waitForTimeout(1_000);
    expect(page.url()).not.toMatch(/\/chat\//);
    expect(apiCallMade).toBe(false);
  });
});

test.describe('Validation — oversize image', () => {
  /**
   * TAC-203: Image >10 MB shows inline error.
   * We simulate via Angular component — actually uploading 10+ MB is slow in tests.
   */
  test('oversize image shows inline error', async ({ page }) => {
    await page.goto('/');

    await fillReturnForm(page, {
      requestType: 'RETURN',
      categoryLabel: 'Konsole / Gaming',
      modelName: 'Test Gaming Device',
      purchaseDateOffset: -15,
    });

    // Inject a File object that exceeds 10 MB via Angular's selectedFile signal
    await page.evaluate(() => {
      const comp = (window as any)['ng']?.getComponent(
        document.querySelector('app-intake-form')
      );
      if (!comp) throw new Error('IntakeFormComponent not found');
      // Create a fake 11 MB File object
      const oversizeFile = new File(
        [new ArrayBuffer(11 * 1024 * 1024)],
        'oversize.jpg',
        { type: 'image/jpeg' }
      );
      // Trigger the component's handleFileSelected which has the size check
      comp.handleFileSelected(oversizeFile);
    });

    // Angular inline error for size
    await expect(
      page.getByText('Plik jest zbyt duży. Maksymalny rozmiar to 10 MB.')
    ).toBeVisible();

    // No file selected = cannot submit
    let apiCallMade = false;
    await page.route('**/api/cases', async (route) => {
      apiCallMade = true;
      await route.continue();
    });

    await page.getByRole('button', { name: 'Wyślij zgłoszenie' }).click();
    await page.waitForTimeout(1_000);
    expect(page.url()).not.toMatch(/\/chat\//);
    expect(apiCallMade).toBe(false);
  });
});

test.describe('Validation — no image attached', () => {
  /**
   * Submitting with no image: form shows error and does not navigate.
   */
  test('form prevents submit when no image is attached', async ({ page }) => {
    await page.goto('/');

    // Fill form but do NOT upload an image
    await fillReturnForm(page, {
      requestType: 'RETURN',
      categoryLabel: 'AGD małe',
      modelName: 'Test Appliance',
      purchaseDateOffset: -20,
    });
    // Intentionally skip uploadTestImage()

    let apiCallMade = false;
    await page.route('**/api/cases', async (route) => {
      apiCallMade = true;
      await route.continue();
    });

    await page.getByRole('button', { name: 'Wyślij zgłoszenie' }).click();
    await page.waitForTimeout(1_000);

    // No navigation
    expect(page.url()).not.toMatch(/\/chat\//);

    // Image required error
    await expect(page.getByText('Zdjęcie jest wymagane.')).toBeVisible();

    expect(apiCallMade).toBe(false);
  });
});
