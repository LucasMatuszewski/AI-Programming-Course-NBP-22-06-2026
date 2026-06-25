# App — Hardware Service Decision Copilot

The application built during the course. Stack chosen in the ADRs (see [`../docs/ADR/`](../docs/ADR/)):

- **Backend:** Spring Boot (Java 21 language level), Maven, openai-java → OpenRouter (Chat Completions), Thumbnailator.
- **Frontend:** Angular (standalone) + Angular Material + ngx-markdown.
- **Topology:** two separate deployables; `ng serve` proxies `/api` to the backend in dev; Docker Compose for one-command local run.

```
app/
  backend/    Spring Boot service (REST + SSE)   — see backend/README.md
  frontend/   Angular SPA                         — see frontend/README.md
  e2e/        Playwright E2E tests
```

## Status

| Part | State |
|---|---|
| Backend (Spring Boot, REST + SSE, 140 tests) | Complete |
| Frontend (Angular + Material, 49 tests, lint + build green) | Complete |
| E2E (Playwright, 12/12 tests passing) | Complete |

## Quick start

### Prerequisites
Copy [`../.env.example`](../.env.example) to `.env` (repo root) and set your API keys.

### Backend
```bash
# Linux / macOS / Git Bash
cd app/backend && ./run-dev.sh

# Windows PowerShell
cd app\backend && .\run-dev.ps1
```
Backend starts on `:8080`. Health check: `/actuator/health`.

### Frontend
```bash
cd app/frontend && npx ng serve      # :4200, proxies /api → :8080
```

### E2E tests (requires both services running)
```bash
cd app/e2e && npm test
```

## Environment
See [`../.env.example`](../.env.example) for all variables. Required: `OPENROUTER_API_KEY` (or `OPENAI_API_KEY`) plus `OPENROUTER_TEXT_MODEL` and `OPENROUTER_VISION_MODEL`.

## Implementation
Business logic is built **test-first** (TDD, per [`../AGENTS.md`](../AGENTS.md)) against the PRD and ADRs.
