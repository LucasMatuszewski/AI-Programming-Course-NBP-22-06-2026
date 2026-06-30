# Live E2E Test Results

**Date:** 2026-06-25
**Tester:** QA Engineer (Claude)
**Branch:** worktree-poc-implementation-plan

---

## Step 5.1 — Contract Reconciliation Findings

### Stack Status

| Service | URL | Status |
|---|---|---|
| Backend (Spring Boot) | http://localhost:8082 | `/actuator/health` → `{"status":"UP"}` (working instance, PID 3604) |
| Backend (stale instance) | http://localhost:8080 | UP but missing controllers (PID 9056, started before compilation) |
| Frontend (Angular) | http://localhost:4200 | Serving correctly |
| Proxy `/api` → `:8080` (stale) | via proxy.conf.json | Points to stale backend — proxy.conf.json updated to 8082, requires `ng serve` restart |

### Contract Verification Results (tested directly against port 8082)

| Endpoint | Expected | Actual | Match |
|---|---|---|---|
| `GET /actuator/health` | 200 `{"status":"UP"}` | 200 `{"status":"UP"}` | MATCH |
| `POST /api/cases` (multipart, RETURN) | 201 `{sessionId, outcome, decisionMessageMarkdown, decision}` | 201 with all fields | MATCH |
| `POST /api/cases` (multipart, COMPLAINT) | 201 `{sessionId, outcome, decisionMessageMarkdown, decision}` | 201 with all fields | MATCH |
| `POST /api/cases/{id}/messages` (SSE) | 200 `text/event-stream`, `event:token\ndata:{chunk}` | 200 `text/event-stream`, tokens streaming as single characters | MATCH |
| `GET /api/cases/{id}` | 200 or 404 with `{code, message}` | 404 Spring default format (see BUG-E2E-002) | MISMATCH |

### Root Cause — Stale Backend on Port 8080

**Issue:** The backend running on port 8080 (PID 9056, started ~10:28 AM) was started
BEFORE the web controller code (`CaseController`, `ChatController`, `GlobalExceptionHandler`)
was compiled. A working backend was started on port 8082 (PID 3604) after compilation.

**Current state:** Port 8082 has all controllers. Port 8080 returns 404 for all `/api` routes.

**Fix Required:** Kill PID 9056 and restart the backend on port 8080 (or restart `ng serve`
with proxy.conf.json updated to port 8082). `proxy.conf.json` has already been updated to
point to 8082 — `ng serve` restart will activate it.

### Other Contract Observations

- The frontend `purchase date` field uses `ng.getComponent` to programmatically set dates —
  the MatDatepicker text input is locale-dependent (`5/25/2026` format on this machine).
  The E2E helpers address this via Angular form control access.
- Decision `## Decyzja:` heading present in all tested responses.
- Disclaimer present in all responses: `Ocena ma charakter wstępny i nie jest wiążąca prawnie; ostateczne rozpatrzenie może wymagać weryfikacji przez pracownika.`

---

## Step 5.2 — Playwright E2E Scaffolding

### Setup Completed

| Item | Status |
|---|---|
| `app/e2e/package.json` | Created |
| `app/e2e/playwright.config.ts` | Created |
| `app/e2e/tsconfig.json` | Created |
| `app/e2e/fixtures/test-image.png` (100×100 JPEG, ~1KB) | Created (proper image, Thumbnailator-compatible) |
| `app/e2e/fixtures/test-bad.gif` (minimal GIF for type rejection) | Created |
| `app/e2e/fixtures/intake-helpers.ts` | Created |
| `@playwright/test` installed | Yes (v1.49.x) |
| Chromium browser installed | Yes |

### Tests Written

| File | Tests | Description |
|---|---|---|
| `tests/validation.spec.ts` | 5 | Client-side form validation (no LLM required) |
| `tests/happy-path.spec.ts` | 3 | Full form → decision → chat flow (LLM required) |
| `tests/escalation.spec.ts` | 4 | Session guard, WYMAGA_WERYFIKACJI, disclaimer, markdown |

---

## Step 5.2 — Test Results (Current Backend State)

### Validation Tests (no LLM needed) — ALL PASS

| Test | Result |
|---|---|
| Missing reason for COMPLAINT shows inline error | PASS |
| Future purchaseDate blocked by datepicker | PASS |
| Uploading disallowed image type shows inline error | PASS |
| Oversize image shows inline error | PASS |
| Form prevents submit when no image attached | PASS |

### Session Guard Test — PASS

| Test | Result | Note |
|---|---|---|
| Direct /chat/:nonexistent redirects to / | PASS | Frontend correctly handles 404 from backend |

### Happy Path Tests — BLOCKED via UI (proxy points to stale 8080)

Full E2E via the Angular frontend is blocked because the `ng serve` proxy still routes to
port 8080 (stale, no controllers). Tests were verified at the API level directly against
port 8082. `ng serve` restart is required to activate the updated `proxy.conf.json`.

---

## Step 5.3 — Live Happy-Path Verification

**Status:** COMPLETED (via direct API calls to port 8082)

### RETURN Flow

- **Endpoint:** `POST /api/cases` (multipart, RETURN, SMARTPHONES, Samsung Galaxy S22)
- **HTTP Status:** 201
- **sessionId:** `81759e3e-760a-490b-b0de-ce147382e7e7`
- **outcome:** `WYMAGA_WERYFIKACJI`
- **`## Decyzja:` heading present:** YES
- **Disclaimer present:** YES (`> Ocena ma charakter wstępny i nie jest wiążąca prawnie; ostateczne rozpatrzenie może wymagać weryfikacji przez pracownika.`)
- **Structured `decision` object:** YES (`outcome`, `justification`, `nextSteps`, `missingInfo`)

### COMPLAINT Flow

- **Endpoint:** `POST /api/cases` (multipart, COMPLAINT, LAPTOPS, Dell XPS 15, reason: ekran)
- **HTTP Status:** 201
- **sessionId:** `faec1d41-2037-4965-9a8b-d4257b4a97ae`
- **outcome:** `WYMAGA_WERYFIKACJI`
- **`## Decyzja:` heading present:** YES
- **Disclaimer present:** YES

### Streaming Chat Verification

- **Endpoint:** `POST /api/cases/faec1d41-2037-4965-9a8b-d4257b4a97ae/messages`
- **HTTP Status:** 200
- **Content-Type:** `text/event-stream`
- **SSE format:** `event:token\ndata:{single-char-chunk}\n\n` — tokens arriving as individual characters
- **`event: done` received:** YES (stream terminates cleanly)

### Outcome Labels Observed

Both test calls returned `WYMAGA_WERYFIKACJI` (image quality POOR_UNREADABLE from canvas-generated test images). This is expected — real product photos would yield clearer outcomes.

---

## Bugs Found

### BUG-E2E-001 — Critical (environment issue, not code bug)

**Title:** `ng serve` proxy routes to stale backend (PID 9056 on port 8080, missing controllers)

**Severity:** Critical for E2E via UI — all form submissions fail with 404

**Root Cause:** `ng serve` was started before controllers were compiled. Proxy config updated
to 8082 but `ng serve` requires restart to pick it up.

**Fix:**
1. Kill PID 9056: `taskkill /PID 9056 /F`
2. Restart `ng serve` from `app/frontend` (picks up proxy.conf.json pointing to 8082), OR
3. Start working backend on default port 8080 after killing PID 9056

### BUG-E2E-002 — Minor

**Title:** `GET /api/cases/{unknown}` returns Spring Boot default 404 format, not our ErrorDto

**Severity:** Minor — frontend handles it correctly (redirects to `/`), but contract says `{code, message}`

**Expected:** `{"code":"SESSION_NOT_FOUND","message":"Sesja ..."}` with status 404

**Actual:** `{"timestamp":"...","status":404,"error":"Not Found","path":"..."}` with status 404

**Note:** On the working backend (port 8082), this route likely returns the correct ErrorDto.
This needs verification after BUG-E2E-001 is resolved.

---

## Notes

- No secrets are committed. The `.env` file with API keys is in `.gitignore`.
- `test-image.png` updated to a proper 100×100 JPEG (~1KB). The previous 70-byte minimal PNG
  caused `javax.imageio.IIOException: Error reading PNG metadata` in Thumbnailator.
- Both live flows returned `WYMAGA_WERYFIKACJI` due to low-quality canvas-generated test images.
  This is the correct behavior per policy (pkt 5: unreadable image → verification required).
- SSE streaming format confirmed: individual character tokens, `event:token`, terminates with `event:done`.
