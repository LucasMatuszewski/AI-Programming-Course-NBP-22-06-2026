# App

This folder contains the application built during the course: the **Hardware Service Decision Copilot**.

The stack is decided in `../docs/ADR/`:
- **Backend** (`backend/`) — Java 21 + Spring Boot 3.5 (Spring Web MVC) + Maven; calls OpenRouter via the openai-java SDK.
- **Frontend** (`frontend/`) — Angular + Angular Material + ngx-markdown (custom streaming chat).

## How to start

The app is scaffolded through a structured process:

1. **Research** — use agents to research and validate the project idea
2. **PRD** — generate a Product Requirements Document (`../docs/PRD-Product-Requirements-Document.md`)
3. **ADR** — generate Architecture Decision Records (`../docs/ADR/`) to choose the tech stack
4. **Scaffold** — backend via Spring Initializr; frontend via `ng new`
5. **Implement** — build features with agents using TDD

## Checklist

Use this checklist during scaffolding. Some items are provided by the generators (Spring Initializr, Angular CLI); others you add explicitly.

### Backend (`backend/`)
- [ ] Scaffold via Spring Initializr — Spring Boot 3.5.x, Java 21, Maven
- [ ] Dependencies: Spring Web, Validation, Actuator
- [ ] Add `com.openai:openai-java` (LLM via OpenRouter) and image lib (Thumbnailator)
- [ ] Package layout: `web`, `application`, `llm`, `image`, `session`, `policy`, `config` (ADR-001)
- [ ] `application.yaml` + config binding for OpenRouter env vars

### Frontend (`frontend/`)
- [ ] Scaffold via `ng new` — latest stable Angular, standalone components, routing
- [ ] Add Angular Material (`ng add @angular/material`) and `ngx-markdown`
- [ ] `proxy.conf.json` mapping `/api` → `http://localhost:8080`
- [ ] Feature folders: `core`, `features/intake`, `features/chat`, `shared` (ADR-003)

### Code quality
- [ ] Backend: standard Spring Boot conventions, 4-space indent
- [ ] Frontend: ESLint (`ng lint`), Prettier, `.editorconfig` (optional)

### Testing
- [ ] Backend unit/integration: JUnit 5 + Mockito + AssertJ + Spring Boot Test/MockMvc; **WireMock** to stub OpenRouter
- [ ] Frontend unit: Angular testing utilities (`*.spec.ts`)
- [ ] E2E: Playwright against the real stack (LLM stubbed/recorded)

### Environment
- [ ] `.env.example` with required env vars (see `../docs/ADR/000-main-architecture.md` §7)
- [ ] `OPENROUTER_API_KEY` / `OPENROUTER_BASE_URL` / model vars set locally
- [ ] `.gitignore` (target/, node_modules/, .env, build output, etc.)

### AI integration
- [ ] openai-java client configured with explicit `.baseUrl(OPENROUTER_BASE_URL)` + `.apiKey(...)` (no `fromEnv()`)
- [ ] Chat Completions only (vision, structured outputs, streaming) — never `/responses`
- [ ] `LlmGateway` seam: `analyzeImage` / `decide` / `streamChat`

### Design
- [ ] Design tokens (`../assets/design-tokens.json`)
- [ ] Logo and favicon (`../assets/`)
- [ ] Design system doc (`../docs/design-guidelines.md`)

### Documentation
- [ ] PRD (`../docs/PRD-Product-Requirements-Document.md`)
- [ ] ADRs (`../docs/ADR/`)
- [ ] AGENTS.md / stack-specific rules where helpful

## Notes

- Don't hand-create config files the generators already provide (Spring Initializr, Angular CLI) — it leads to conflicts.
- Run the two dev processes together: Spring Boot on `:8080`, Angular dev server on `:4200` with the `/api` proxy.
- Keep each app organized: separate controllers/services/domain (backend) and routes/components/domain/tests (frontend).
