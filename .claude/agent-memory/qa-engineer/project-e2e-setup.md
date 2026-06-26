---
name: project-e2e-setup
description: E2E test stack location, how to run against the REAL stack (real OpenRouter), and key patterns
metadata:
  type: project
---

E2E test suite is at `app/e2e/` using Playwright + TypeScript targeting Chromium headless.

**Why:** E2E mocks **NOTHING** — real Angular → real Spring Boot → **real OpenRouter** (real `OPENROUTER_API_KEY`). The `stub-llm` profile is a dev/demo lane only and is **never** the sign-off gate. (An earlier run wrongly ran E2E on `stub-llm` with prefix-faked decisions and hid real bugs — see `app/e2e/AGENTS.md`.)

**How to apply:** Run the real stack:
```bash
# Terminal 1 — REAL backend (needs OPENROUTER_API_KEY in env/.env)
cd app/backend && ./mvnw spring-boot:run
# Terminal 2 — frontend
cd app/frontend && npm start
# Terminal 3 — E2E
cd app/e2e && npx playwright test
```

Key files:
- `app/e2e/playwright.config.ts` — baseURL=http://localhost:4200, workers=1 (stateful backend)
- `app/e2e/tests/helpers.ts` — `fillAndSubmitForm()` + `IMAGES` (real device photos from `assets/example-images-for-tests/`; never synthetic blobs) + `expectDecisionAndDisclaimer()`
- `app/e2e/tests/happy-path.spec.ts` — Reklamacja + Zwrot full flows
- `app/e2e/tests/validation.spec.ts` — empty form, missing reason, missing image
- `app/e2e/tests/decision-categories.spec.ts` — **image-format** coverage (JPEG/PNG/WebP) against the real stack; asserts **one of the four** valid decisions + disclaimer, NOT a specific category

**Assert structure, not LLM wording** (the live model is nondeterministic). Deterministic category-logic (signs-of-use ⇒ NOT_ELIGIBLE, etc.) is verified in **BE integration tests with WireMock**, never in E2E.

Disclaimer text to assert: `wstępna, automatyczna ocena` (from `MessageComposer.DISCLAIMER`) — it is deterministic (backend-appended), so it is safe to assert in E2E.

**`StubLlmGateway` (dev-only `stub-llm` profile, NOT E2E):** routes by model-name prefix — `ELIGIBLE:*`, `NOT_ELIGIBLE:*`, `NEEDS_HUMAN_REVIEW:*`, `MORE_INFO_REQUIRED:*`, default → ELIGIBLE. Useful for fast local FE dev only; do not drive E2E decisions this way.
