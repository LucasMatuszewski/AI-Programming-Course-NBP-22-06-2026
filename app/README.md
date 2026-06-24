# App — Hardware Service Decision Copilot

The application built during the course. Stack chosen in the ADRs (see [`../docs/ADR/`](../docs/ADR/)):

- **Backend:** Spring Boot (Java 21 language level), Maven, openai-java → OpenRouter (Chat Completions), Thumbnailator.
- **Frontend:** Angular (standalone) + Angular Material + ngx-markdown.
- **Topology:** two separate deployables; `ng serve` proxies `/api` to the backend in dev; Docker Compose for one-command local run.

```
app/
  backend/    Spring Boot service (REST + SSE)   — see backend/README.md
  frontend/   Angular SPA                         — see frontend/README.md
```

## Status of initialization

| Part | State |
|---|---|
| Backend skeleton (pom, main class, config, policies, smoke test) | Created by hand (buildable once Maven resolves deps). |
| Maven Wrapper | **To generate** (needs network): `cd app/backend && mvn -N wrapper:wrapper`. |
| Angular app | **To scaffold** (needs network): see [`frontend/README.md`](frontend/README.md). |

The agent's sandbox has no outbound network, so the two network-dependent generator
steps above are run by a developer; everything else is in place.

## Quick start (after the two generator steps)

```bash
# Backend
cd app/backend && ./mvnw spring-boot:run         # :8080, /actuator/health

# Frontend (new terminal)
cd app/frontend && npx ng serve                   # :4200, proxies /api -> :8080

# Or both via Docker (after Dockerfiles are added in the implementation phase)
docker compose up
```

## Environment
Copy [`../.env.example`](../.env.example) to `.env` and set `OPENROUTER_API_KEY`
(or `OPENAI_API_KEY`). See ADR-000 §7 for the full variable list.

## Implementation
Business logic is built **test-first** (TDD, per [`../AGENTS.md`](../AGENTS.md)) against the
PRD and ADRs during the implementation phase — it is intentionally not pre-written here.
