---
name: feedback-angular-material-datepicker
description: Angular Material mat-datepicker requires setting date via ng.getComponent — not via text fill
metadata:
  type: feedback
---

When testing Angular Material mat-datepicker in Playwright:
- Filling the text input (e.g., `25.05.2026`) does NOT reliably set the form control value on Windows. The locale format is `M/D/YYYY` in the test browser (`5/25/2026`), not `DD.MM.YYYY`.
- The `mat-label` element overlays the `mat-select` combobox and intercepts pointer events. Clicking via `page.evaluate(() => el.click())` works reliably; `page.getByRole('combobox').click()` may time out.

**Why:** The material datepicker internally stores a `Date` object in the form control, not a string. Filling the text input does not parse reliably across locales.

**How to apply:** Always set the purchase date programmatically via `ng.getComponent(el).form.get('purchaseDate').setValue(new Date(...))` in Playwright tests. See `app/e2e/fixtures/intake-helpers.ts` for the pattern.

Related: [[feedback-playwright-file-upload-restriction]]
