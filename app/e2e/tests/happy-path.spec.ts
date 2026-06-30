/**
 * Happy-path E2E tests: full form → AI decision → streaming chat flow.
 *
 * Runs against the real stack (no mocking):
 *   Frontend: http://localhost:4200
 *   Backend:  http://localhost:8080  (with real OpenRouter key)
 *
 * @see docs/ADR/000-main-architecture.md §10 — Testing Strategy
 * @see docs/PRD-Product-Requirements-Document.md — AC-01..AC-23
 * @tag @smoke
 */
import { test, expect, Page } from '@playwright/test';
import * as path from 'path';
import { fillReturnForm, uploadTestImage, TEST_IMAGE_PATH } from '../fixtures/intake-helpers';

// ─── Helpers ────────────────────────────────────────────────────────────────

/** Outcome chip labels rendered in the chat bubble (Polish). */
const OUTCOME_LABELS = [
  'Przyjęty do odsprzedaży',
  'Odrzucona',
  'Wymaga weryfikacji',
  'Uznana',
];

/** The non-binding disclaimer text required by AC-16 / TAC-08 / TAC-107. */
const DISCLAIMER_TEXT = 'Ocena ma charakter wstępny';

// ─── Tests ───────────────────────────────────────────────────────────────────

test.describe('Happy path — RETURN flow @smoke', () => {
  test('full flow: form → decision bubble → chat reply', async ({ page }) => {
    // ── Step 1: Navigate to intake form ────────────────────────────────────
    await page.goto('/');
    await expect(page).toHaveTitle(/Asystent/);
    await expect(page.getByRole('heading', { name: 'Nowe zgłoszenie serwisowe' })).toBeVisible();

    // ── Step 2: Fill form (RETURN, LAPTOPS, 30 days ago) ───────────────────
    await fillReturnForm(page, {
      requestType: 'RETURN',
      categoryLabel: 'Laptopy / Komputery',
      modelName: 'Dell XPS 15 E2E Test',
      purchaseDateOffset: -30,
    });
    await uploadTestImage(page, TEST_IMAGE_PATH);

    // ── Step 3: Submit ──────────────────────────────────────────────────────
    // Wait for the spinner while the AI pipeline runs (may take up to 60s)
    const submitBtn = page.getByRole('button', { name: 'Wyślij zgłoszenie' });
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Wait for navigation to /chat/:sessionId (AI pipeline response)
    await page.waitForURL(/\/chat\/[0-9a-f-]{36}/, { timeout: 90_000 });

    // ── Step 4: Assert chat route ───────────────────────────────────────────
    expect(page.url()).toMatch(/\/chat\/[0-9a-f-]{36}/);

    // ── Step 5: Assert first decision bubble ───────────────────────────────
    // The system bubble renders as markdown — check for heading markers
    const chatMessages = page.locator('.chat-bubble--system');
    await expect(chatMessages.first()).toBeVisible({ timeout: 10_000 });

    // Disclaimer must be present (AC-16 / TAC-08)
    // Use .first() — the disclaimer text may appear in multiple rendered markdown paragraphs
    await expect(page.getByText(DISCLAIMER_TEXT).first()).toBeVisible({ timeout: 10_000 });

    // ── Step 6: Assert outcome chip is visible ─────────────────────────────
    const chipVisible = await page.locator('.status-chip').isVisible();
    expect(chipVisible).toBe(true);

    const chipText = await page.locator('.status-chip').textContent();
    const outcomeFound = OUTCOME_LABELS.some((label) => chipText?.includes(label));
    expect(outcomeFound).toBe(true);

    // ── Step 7: Send a chat message ─────────────────────────────────────────
    const chatInput = page.getByRole('textbox', { name: 'Wiadomość do asystenta' });
    await expect(chatInput).toBeVisible();
    await chatInput.fill('Czy mogę przyspieszyć ten proces?');
    await page.getByRole('button', { name: 'Wyślij' }).click();

    // ── Step 8: Assert streaming response appears ───────────────────────────
    // User bubble should appear immediately
    await expect(page.locator('.chat-bubble--user').last()).toBeVisible({ timeout: 5_000 });
    await expect(page.locator('.chat-bubble--user').last()).toContainText('Czy mogę przyspieszyć');

    // Wait for at least one assistant bubble to appear and contain text.
    // Use innerText() — textContent() returns empty before ngx-markdown renders.
    const assistantBubble = page.locator('.chat-bubble--assistant').last();
    await expect(assistantBubble).toBeVisible({ timeout: 60_000 });
    await expect(assistantBubble).not.toHaveText('', { timeout: 30_000 });

    // Input re-enabled after streaming completes
    await expect(chatInput).toBeEnabled({ timeout: 60_000 });

    // ── Step 9: All visible text should be in Polish ────────────────────────
    // Check key UI labels are Polish (AC-22) — use role-based locators to avoid
    // strict-mode violations from markdown-rendered text containing the same words
    await expect(page.getByRole('button', { name: 'Nowe zgłoszenie' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Wyślij wiadomość' })).toBeVisible();
    // No English "Submit" or "Send" buttons
    await expect(page.getByRole('button', { name: 'Submit' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: /^Send$/ })).toHaveCount(0);
  });
});

test.describe('Happy path — COMPLAINT flow @smoke', () => {
  test('complaint form → decision → outcome visible', async ({ page }) => {
    await page.goto('/');

    // Fill COMPLAINT form with reason
    await fillReturnForm(page, {
      requestType: 'COMPLAINT',
      categoryLabel: 'Smartfony',
      modelName: 'Samsung Galaxy A54',
      purchaseDateOffset: -90,
      reason: 'Pęknięty ekran po upadku z niskiej wysokości — widoczne uszkodzenie mechaniczne.',
    });
    await uploadTestImage(page, TEST_IMAGE_PATH);

    const submitBtn = page.getByRole('button', { name: 'Wyślij zgłoszenie' });
    await expect(submitBtn).toBeEnabled();
    await submitBtn.click();

    // Wait for navigation (AI pipeline may take up to 90s)
    await page.waitForURL(/\/chat\/[0-9a-f-]{36}/, { timeout: 90_000 });

    // Decision bubble rendered
    await expect(page.locator('.chat-bubble--system').first()).toBeVisible({ timeout: 10_000 });

    // Disclaimer present (AC-16)
    // Use .first() — disclaimer text may appear in multiple rendered markdown elements
    await expect(page.getByText(DISCLAIMER_TEXT).first()).toBeVisible();

    // Outcome chip for complaint: UZNANA | ODRZUCONA | WYMAGA_WERYFIKACJI
    const chipText = await page.locator('.status-chip').textContent();
    const validComplaintOutcomes = ['Uznana', 'Odrzucona', 'Wymaga weryfikacji'];
    const found = validComplaintOutcomes.some((o) => chipText?.includes(o));
    expect(found).toBe(true);

    // Send a follow-up chat message and verify streaming
    const chatInput = page.getByRole('textbox', { name: 'Wiadomość do asystenta' });
    await chatInput.fill('Jaki jest następny krok?');
    await page.getByRole('button', { name: 'Wyślij' }).click();

    // Wait for assistant response
    const lastAssistant = page.locator('.chat-bubble--assistant').last();
    await expect(lastAssistant).toBeVisible({ timeout: 60_000 });
    await expect(lastAssistant).not.toHaveText('', { timeout: 30_000 });
  });
});

test.describe('"Nowe zgłoszenie" button navigation', () => {
  test('clicking Nowe zgłoszenie navigates back to /', async ({ page }) => {
    await page.goto('/');

    // Fill a minimal valid RETURN form and submit
    await fillReturnForm(page, {
      requestType: 'RETURN',
      categoryLabel: 'Akcesoria / Peryferia',
      modelName: 'Test Device Nav',
      purchaseDateOffset: -10,
    });
    await uploadTestImage(page, TEST_IMAGE_PATH);

    await page.getByRole('button', { name: 'Wyślij zgłoszenie' }).click();
    await page.waitForURL(/\/chat\//, { timeout: 90_000 });

    // Click "Nowe zgłoszenie" in chat toolbar
    await page.getByRole('button', { name: 'Nowe zgłoszenie' }).click();

    // Should navigate back to the intake form
    await expect(page).toHaveURL('/');
    await expect(page.getByRole('heading', { name: 'Nowe zgłoszenie serwisowe' })).toBeVisible();
  });
});
