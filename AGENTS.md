# Repository Guidelines

## Project

This is a **course project** for the "AI dla programistów — od pomysłu do MVP" training by JSystems — a **dedicated (closed) course for NBP (Narodowy Bank Polski)**, 12 participants, starting **2026-06-22**. The app is a multimodal AI assistant built live during the course. The domain, tech stack, and architecture are decided by the group through a structured process: research → PRD → ADR → implementation with agents.

This is only the **base starting repository** for the course; concrete decisions are made live with the group.

**Accepted stack (per `docs/ADR/`):** **Java 21 + Spring Boot 3.5.x** (Maven) backend, **Angular** (standalone + Material) frontend, **`com.openai:openai-java` → OpenRouter** (Chat Completions) for LLM calls. Spring AI and the Vercel AI SDK were explicitly **rejected** in ADR-000/003.
TypeScript/Node.js (Next.js, Vercel AI SDK) was the instructor's demo stack and remains in the course materials for reference, but is **not** the chosen implementation stack.
Participants may still work in any language for their own exercises (Java, Python, C#, Go, Rust, etc.).

All user-facing text in **Polish**.

**Key docs** (created during the course — load only when in doubt):
- `docs/PRD-Product-Requirements-Document.md` — product requirements and acceptance criteria
- `docs/ADR/` — Architecture Decision Records
- `docs/design-guidelines.md` — design system and tokens

---

## Repository Layout

```
app/                 Application built during the course (start: empty scaffold)
assets/              Design tokens, logo, favicon
docs/                PRD, ADR, design system
course-materials/    Notes, scripts, examples, research
```

---

## Agent Workflow

### Before Starting Any Task
1. Read the relevant PRD and ADR files for the affected area.
2. Define the expected behavior from the specification before writing or changing any code.

### TDD Rules
For every feature and bug fix:
1. Start from the specification, not the existing implementation.
2. Write or extend tests **before** production code.
3. Run the new tests and confirm they fail for the expected reason.
4. Implement the minimum code needed to make them pass.
5. Run the full verification suite for the changed scope.
6. Refactor only while tests stay green.

If the area has no suitable test infrastructure yet, add it as part of the task — do not silently skip tests.

### Verification (required before every commit)

Run the commands appropriate for the changed scope.

Backend (`app/backend`, Spring Boot / Maven):
```bash
./mvnw test          # JUnit 5 + Mockito unit/integration tests pass
./mvnw verify        # full build + integration tests
```

Frontend (`app/frontend`, Angular):
```bash
npm test             # unit tests pass (Jasmine/Karma or Vitest)
npm run lint         # no lint errors
npm run build        # ng build succeeds
```

Verify only the scope relevant to your change. If the change affects runtime behavior, confirm the app starts correctly.

**Test Strategy:**
| Type | Mocks | Who |
|---|---|---|
| Unit | All deps | be/fe-dev |
| Integration | Only external LLM API | be-dev |
| E2E | NOTHING (real stack) | qa-engineer |

**Verification:** Always start the app before committing. Tests passing ≠ app working.

**Env Vars:** See `.env.example` (OPENROUTER_API_KEY or OPENAI_API_KEY required)

### Commit Rules
- Commit only after verification passes and the changed scope is in a working state.
- Keep commits focused: one logical change per commit.
- Format: `Area: short summary` (e.g. `Backend:`, `Frontend:`, `Docs:`)
- Do **not** push to remote unless the user explicitly asks.

### Completion Criteria
A task is complete only when:
- Implementation matches the relevant PRD, ADR, and design guidance
- Tests were written first and pass honestly
- Verification for the changed scope passed with no errors or warnings
- The commit message is focused and the repository is in a consistent, reviewable state

---

## Context7 MCP Library IDs

Libraries for the accepted stack (per `docs/ADR/`). Resolve via `resolve-library-id` if an ID changes.

**Backend (Java / Spring Boot):**

| Library | Context7 ID |
|---|---|
| Spring Boot | `/spring-projects/spring-boot` |
| Spring Framework | `/spring-projects/spring-framework` |
| OpenAI Java SDK | `/openai/openai-java` |
| OpenRouter (docs) | `/websites/openrouter_ai` |
| Thumbnailator | `/coobird/thumbnailator` |
| Jakarta Bean Validation | `/jakartaee/validation` |

**Frontend (Angular):**

| Library | Context7 ID |
|---|---|
| Angular | `/angular/angular` |
| Angular CLI | `/angular/angular-cli` |
| Angular Material & CDK | `/angular/components` |
| ngx-markdown | `/jfcere/ngx-markdown` |
