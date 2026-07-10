# Implementation Plan — Hardware Service Decision Copilot (PoC)

> Orchestration plan. Claude acts **only as orchestrator/manager** — all code is delegated to
> the three specialized agents. Each delegated task below ships with a **self-contained context
> packet** (the exact PRD/ADR/design slices that task needs — nothing more) so agents never have
> to ask questions or read the whole spec.

---

## Context

We are building a **proof of concept** of the *Hardware Service Decision Copilot* — a Polish-language
customer self-service web app that returns a **preliminary, advisory** decision on an electronics
**reklamacja (complaint)** or **zwrot (return)**. The customer fills a form + uploads one photo; the
backend compresses the image, runs a two-stage AI pipeline (multimodal image analysis → policy-grounded
reasoning), and opens a chat seeded with the decision.

**What prompted this:** the domain, requirements (PRD), and architecture (ADR-000..003) are all
**Accepted**, and the design system is fixed (`docs/design-guidelines.md`). The `docs/`, `assets/`,
`docs/policies/`, `.env.example`, and `docker-compose.yml` already exist — but `app/backend/` and
`app/frontend/` are **empty/non-existent** (the `app/README.md` describes intended, not actual, state).
This plan turns the frozen spec into a working PoC via small, test-first, independently committable steps.

**Intended outcome:** a locally runnable PoC — `./mvnw spring-boot:run` (`:8080`) + `ng serve` (`:4200`,
`/api` proxied) — that satisfies the PRD acceptance criteria (AC-01..AC-24) and ADR technical acceptance
criteria (TAC-01..10, 101..108, 201..208, 301..308), with unit + integration tests green and at least one
**live** form→decision→chat happy-path verified by QA against a real OpenRouter key.

### Decisions confirmed with the user (drive this plan)
1. **Agents have outbound network** — agents scaffold Spring Boot + Angular and resolve deps themselves.
2. **Live happy-path E2E** — a valid `OPENROUTER_API_KEY` exists; QA verifies ≥1 live flow. Unit/integration still stub the LLM (ADR test strategy).
3. **Delivery = local dev only** — `mvnw spring-boot:run` + `ng serve`. Existing `docker-compose.yml` is left as-is; Dockerfiles are **not** built/verified in this effort.
4. **Single branch `prd1`, sequenced** — all commits land on `prd1`. Backend and frontend touch **disjoint paths** (`app/backend/**` vs `app/frontend/**`) so they run in parallel without collision; QA runs after both.
5. **Model IDs kept as-is** — agents use `OPENROUTER_*` values from `.env` verbatim (user guarantees they resolve); everything env-driven, no hardcoded model IDs.
6. **FE unit tests = Jasmine/Karma** (Angular CLI default).

---

## Roles & the golden rules every task inherits

**Agents:** `be-developer` (backend), `fe-developer` (frontend), `qa-engineer` (integration/E2E).

**Standard preamble injected into EVERY delegated task** (so it is never repeated in the per-step packets):

- **TDD, strictly** (AGENTS.md): start from the spec → write/extend tests → run them and confirm they
  **fail for the expected reason** → implement the minimum to pass → run the changed-scope verification →
  refactor green. Tests passing ≠ app working: start the app when the change affects runtime.
- **Verification before commit** (changed scope only):
  - Backend: `./mvnw test` (units) / `./mvnw verify` (full); start with `./mvnw spring-boot:run` when runtime is affected.
  - Frontend: `npm test` (Jasmine/Karma), `npm run lint`, `npm run build`.
- **Commit after each step.** Format `Area: short summary` (`Backend:`, `Frontend:`, `Tests:`, `Docs:`).
  One logical change per commit. **Do not push** (no `git push`) and **do not open PRs** unless the user asks.
- **Branch:** work on `prd1`. Backend touches only `app/backend/**`; frontend only `app/frontend/**`;
  QA only `app/e2e/**` (+ reads). **Never edit files outside your lane** without flagging the orchestrator.
- **Polish everywhere** — all user-facing text (labels, errors, agent output, decision message) in Polish (AC-22).
- **Context7 first** — for any library, resolve + query docs via Context7 (IDs listed per task) before coding.
- **Frozen API contract** (see below) — neither side changes it unilaterally.
- Report back: what was implemented, test results (paste failing-then-passing summary), files touched, and any contract ambiguity discovered.

### Frozen API contract (shared truth for parallel FE/BE work)
This is the coordination keystone. **Both** `be-developer` and `fe-developer` code against it; it does **not**
change without an explicit orchestrator-mediated sync. Source: ADR-000 §6, ADR-001 §4–5, ADR-002 §4–5.

| Endpoint | Method | Request | Success | Errors |
|---|---|---|---|---|
| `/api/cases` | POST `multipart/form-data` | fields `requestType, category, modelName, purchaseDate (ISO), reason` + part `image` | `201 { sessionId, outcome, decisionMessageMarkdown, decision:{ outcome, justification, nextSteps[], missingInfo[] } }` | `400 VALIDATION_ERROR` (+`fieldErrors`), `415 UNSUPPORTED_IMAGE_TYPE`, `413 IMAGE_TOO_LARGE`, `502 LLM_UPSTREAM_ERROR`, `504 LLM_TIMEOUT` |
| `/api/cases/{sessionId}/messages` | POST JSON `{ content }` | non-blank content | `200 text/event-stream`: `token` events → terminal `done`; mid-stream `error` event | `404 SESSION_NOT_FOUND`, `400` blank |
| `/api/cases/{sessionId}` | GET | — | `200 { sessionId, decision, messages:[{role,content,createdAt}] }` | `404 SESSION_NOT_FOUND` |
| `/actuator/health` | GET | — | `200` | — |

Error body shape: `{ code, message (PL), fieldErrors?: {field: msg} }`.
Enums — `requestType ∈ {COMPLAINT, RETURN}`; complaint outcome `∈ {UZNANA, ODRZUCONA, WYMAGA_WERYFIKACJI}`;
return outcome `∈ {PRZYJETY_DO_ODSPRZEDAZY, ODRZUCONA, WYMAGA_WERYFIKACJI}`; 10 categories per PRD §8.

---

## Phase & dependency overview

```
P0 Scaffold ──┬─ 0.1 BE scaffold ─────────────► gates all BE
              └─ 0.2 FE scaffold ─────────────► gates all FE      (0.1 ∥ 0.2)

P1 BE units  ── 1.1 models → {1.2 img-validate, 1.3 img-compress, 1.4 policy, 1.5 session, 1.6 msg-builder}
P2 BE LLM    ── 2.1 prompts, 2.2 parser, 2.3 client    (needs 1.1, 1.4)
P3 BE web    ── 3.1 orchestrator → 3.2 case ctrl → 3.3 chat/GET ctrl → 3.4 wire+CORS   (needs P1,P2)

P4 FE        ── 4.1 theme/shell → {4.2 form, 4.3 CaseService} → {4.4 chat UI, 4.5 ChatService+state} → 4.6 UX/build
                 (runs ENTIRELY IN PARALLEL with P1–P3, against the frozen contract + mocked services)

P5 QA        ── 5.1 local wire-up → 5.2 Playwright E2E (stubbed) → 5.3 LIVE happy-path → 5.4 error/AC E2E
                 (needs P3 done AND P4 done)

P6 Final     ── full verification, app-start proof, README/Docs touch-ups   (needs P5)
```

**Parallelism:** P0.1∥P0.2. Then the **backend track (P1→P2→P3)** and the **frontend track (P4)** run
**concurrently** — they share only the frozen contract and never the same files. P5 is the join point
(both tracks complete), P6 closes out.

### Agent × phase assignment matrix

| Phase | be-developer | fe-developer | qa-engineer |
|---|---|---|---|
| P0 Scaffold | 0.1 | 0.2 | — |
| P1 BE units | 1.1–1.6 | — | — |
| P2 BE LLM | 2.1–2.3 | — | — |
| P3 BE web | 3.1–3.4 | — | — |
| P4 FE | — | 4.1–4.6 | — |
| P5 QA | (fix if QA finds BE bug) | (fix if QA finds FE bug) | 5.1–5.4 |
| P6 Final | assist | assist | lead verification |

---

## Phase 0 — Scaffolding (gate; 0.1 ∥ 0.2)

### Step 0.1 — Backend scaffold · `be-developer`
**Depends on:** none. **Blocks:** all of P1–P3.
**Task:** Create a buildable Spring Boot 3.5.x / Java-21-language-level Maven project at `app/backend/`
with the Maven wrapper, package skeleton (`web, case, llm, image, policy, session, config`), `application.yml`
binding the env vars, `AppProperties`, an enabled Actuator health endpoint, and **mirror** the two policy
files into `src/main/resources/policies/`. App must start and `GET /actuator/health` → 200. One smoke test.
**TDD:** a context-loads test + a health-endpoint test (`@SpringBootTest`, `MockMvc`) written first.
**Verify:** `./mvnw test`, then `./mvnw spring-boot:run` → curl `/actuator/health`.
**Commit:** `Backend: scaffold Spring Boot project, config, policies, health smoke test`.
**Context packet:**
- ADR-000 §3 (repo structure, tech stack table), §7 (env var table — `OPENROUTER_API_KEY/BASE_URL/TEXT_MODEL/VISION_MODEL`, `OPENAI_API_KEY` precedence, `SERVER_PORT`, `APP_SESSION_TTL_MINUTES`, `APP_IMAGE_MAX_BYTES`, `APP_CORS_ALLOWED_ORIGIN`).
- ADR-001 §3 (component/package layout), §6 (TTL store decision).
- Dependencies to add: `spring-boot-starter-web`, `-validation`, `-actuator`, `-test`, `com.openai:openai-java`, `net.coobird:thumbnailator`, a MockWebServer/WireMock test dep.
- Existing files to mirror policies **from**: `docs/policies/complaint-policy.md`, `docs/policies/return-policy.md` → copy into `app/backend/src/main/resources/policies/`.
- Java: JDK 25 installed, **pin language level to 21**. Context7: `/spring-projects/spring-boot`.
- Note: `app/README.md` is aspirational; the project does **not** exist yet — create it fresh.

### Step 0.2 — Frontend scaffold + brand theme · `fe-developer`
**Depends on:** none. **Blocks:** all of P4.
**Task:** Scaffold an Angular (latest stable, **standalone**, signals) app at `app/frontend/` with Angular
Material + CDK + ngx-markdown, routing (`/` and `/chat/:sessionId`), `proxy.conf.json` (`/api` → `:8080`),
and the **NBP brand theme** wired from `assets/` (self-hosted fonts + design tokens). App shell renders the
NBP navy header with logo; `ng serve` and `ng build` both succeed.
**TDD:** `AppComponent` spec (renders shell / router outlet) written first; default CLI specs kept green.
**Verify:** `npm test`, `npm run lint`, `npm run build`.
**Commit:** `Frontend: scaffold Angular standalone app, Material, ngx-markdown, NBP theme`.
**Context packet:**
- ADR-002 §3 (standalone components, routes, no NgRx), §5 (dev proxy), §1 scope.
- `docs/design-guidelines.md` **in full** (colors, typography, components, logo usage) + `assets/design-tokens.json`, `assets/fonts/fonts.css`, `assets/logo.svg`, `assets/favicon.ico`. Map tokens → Material theme + global CSS vars; fonts: Brygada 1918 (headings), Libre Franklin (body).
- Context7: `/angular/angular`, `/angular/angular-cli`, `/angular/components`, `/jfcere/ngx-markdown`.
- FE unit runner: **Jasmine/Karma** (CLI default). Polish UI text only.

---

## Phase 1 — Backend domain & pure units · `be-developer` (sequential within agent)

> All steps depend on **0.1**. 1.1 blocks 1.2–1.6. Each is a separate TDD step + commit. Mocks all deps.

### Step 1.1 — Domain models & enums
**Task:** Immutable domain types + enums: `RequestType`, `EquipmentCategory` (10 PRD values), complaint/return
`Outcome` enums (or one enum with scenario validity), `CaseRequest`, `ImageAssessment`, `Decision`,
`ChatMessage`, `Session`. **TDD:** tests asserting enum membership (10 categories; scenario-correct outcome sets),
construction/invariants. **Commit:** `Backend: domain models and enums`.
**Context packet:** ADR-000 §5 (all model field lists), PRD §8 (10 categories), §6 AC-12 (outcome sets). No LLM/web.

### Step 1.2 — ImageValidator
**Task:** MIME/type ∈ {jpeg,png,webp} else **415**; size ≤ `APP_IMAGE_MAX_BYTES` (10 MB) else **413**; exactly one image else 400.
**TDD:** `.gif`/`.bmp`→415; 10 MB+1→413; exactly 10 MB ok; spoofed-extension-vs-MIME. **Commit:** `Backend: image validation (type/size)`.
**Context packet:** ADR-001 §5 validation rules + §8 table rows (bad type/oversize), PRD AC-06/07/08. Custom exceptions for 413/415.

### Step 1.3 — ImageCompressor (Thumbnailator)
**Task:** Resize to max long edge (~1568px) + quality re-encode; **guarantee output bytes ≤ input bytes**; return bytes+content-type + base64 data URL. Never upscale small images.
**TDD:** 8 MB jpeg → output ≤ input + valid data URL; already-small image not upscaled. **Commit:** `Backend: image compression/resize (Thumbnailator)`.
**Context packet:** ADR-001 §3 `ImageCompressor`, TAC-103 (≤ input), PRD AC-10. Context7: `/coobird/thumbnailator`.

### Step 1.4 — PolicyProvider
**Task:** Load `classpath:policies/complaint-policy.md` | `return-policy.md` by `RequestType`, cached; fail-fast if missing.
**TDD:** returns non-empty matching text per type; missing file → startup/health fails fast. **Commit:** `Backend: policy loader`.
**Context packet:** ADR-001 §3 `PolicyProvider`, ADR-000 §8 documents table. (Files already mirrored in 0.1.)

### Step 1.5 — SessionStore + InMemorySessionStore
**Task:** `SessionStore` interface + `ConcurrentHashMap` impl; TTL eviction (`APP_SESSION_TTL_MINUTES`); refresh `lastAccessedAt` on read; image bytes **not** retained.
**TDD:** save/get; entry older than TTL evicted; `lastAccessedAt` refresh prevents eviction; expired get → empty. **Commit:** `Backend: in-memory session store with TTL`.
**Context packet:** ADR-000 §5 Session model + image-discard decision, ADR-001 §3/§6 TTL store, TAC-105.

### Step 1.6 — DecisionMessageBuilder
**Task:** Pure builder: structured `Decision` → Polish **markdown** first message — greeting → decision status label → justification → next-steps list → **mandatory disclaimer** (`"Ocena ma charakter wstępny i nie jest wiążąca prawnie; ostateczne rozpatrzenie może wymagać weryfikacji przez pracownika."`). Headings/lists, not one paragraph.
**TDD:** message always contains disclaimer (TAC-107/AC-16); contains all 5 blocks; status label maps per outcome; escalation lists `missingInfo`. **Commit:** `Backend: decision message builder (Polish markdown)`.
**Context packet:** PRD §11.3 + AC-17/AC-16, ADR-003 §7.6 (disclaimer text), ADR-001 §3 builder.

---

## Phase 2 — Backend LLM integration · `be-developer`

> Depends on **1.1** (models) and **1.4** (policy). 2.3 depends on 2.1+2.2. LLM always stubbed in tests (TAC-308).

### Step 2.1 — PromptFactory
**Task:** Build 4 prompt variants (complaint/return × image/reasoning) + chat system prompt. Image prompts per ADR-003 §7.1/7.2; reasoning prompts inject the **full matching policy text** + form + assessment, instruct JSON-only, cite ≥1 rule, escalate on unreadable/contradictory/implausible/borderline, never invent rules, refuse off-topic. Chat prompt per §7.5 (full context, Polish default + language mirroring, stay on-case).
**TDD:** complaint vs return select **distinct** image prompts and **distinct** reasoning prompts (TAC-302); reasoning prompt **contains the matching policy text** + "JSON only" (TAC-303); chat prompt carries decision+assessment+policy. **Commit:** `Backend: prompt factory (4 scenario prompts + chat)`.
**Context packet:** ADR-003 §7 (all subsections, verbatim intent), §3 `PromptFactory`/`CaseContext`, PRD §11.1/11.2, AC-11/13/14/15/20/21.

### Step 2.2 — DecisionParser
**Task:** Parse model JSON → `Decision`; validate `outcome` ∈ scenario enum; on malformed/out-of-enum/empty → controlled `WYMAGA_WERYFIKACJI` fallback with `missingInfo=["Nie udało się jednoznacznie ocenić zgłoszenia"]`. **Never throws to 500.**
**TDD:** valid JSON → enum outcome (extra fields ignored); non-JSON/truncated/wrong-enum/empty → fallback, no exception (TAC-304). **Commit:** `Backend: decision JSON parser with safe fallback`.
**Context packet:** ADR-003 §4 (Decision shape), §5 failure handling, §6 (fallback decision), TAC-304.

### Step 2.3 — LlmClient + OpenAiClientConfig + OpenRouterLlmClient
**Task:** `OpenAiClientConfig` builds one `OpenAIClient` with `baseUrl=OPENROUTER_BASE_URL`, key resolution
(`OPENAI_API_KEY` over `OPENROUTER_API_KEY`), timeouts; optional OpenRouter ranking headers. `LlmClient` interface
(`analyzeImage`, `decide`, `streamChat`) hiding SDK types; `OpenRouterLlmClient` impl + `ModelSelector`
(vision model for image, text model for reason+chat). Vision = base64 data-URL content part.
**TDD (integration, MockWebServer — no real network):** client targets OpenRouter base URL; key precedence (TAC-301);
vision uses `OPENROUTER_VISION_MODEL` + image part, reason/chat use `OPENROUTER_TEXT_MODEL` (TAC-305); happy path =
one vision then one reasoning call in order; streaming relays deltas in order + clean terminate, early close → single error (TAC-306); fail-fast if neither key set.
**Commit:** `Backend: OpenRouter LLM client (vision, reasoning, streaming)`.
**Context packet:** ADR-003 §3, §4, §5 (call table), §6 (Chat Completions decision), ADR-000 §7 (env). Context7: `/openai/openai-java`, `/websites/openrouter_ai`. **Models from env, verbatim — no hardcoded IDs.**

---

## Phase 3 — Backend orchestration & web · `be-developer`

> Depends on **P1 + P2**. Sequential: 3.1 → 3.2 → 3.3 → 3.4.

### Step 3.1 — CaseService orchestrator
**Task:** `createCase(CaseRequest, imageBytes)`: validate → compress → `analyzeImage` (vision) → load policy →
`decide` (reasoning) → `DecisionMessageBuilder` → persist `Session` → return `CaseResult`. Validation failure ⇒ **zero** LLM calls.
**TDD (unit, all deps mocked):** valid complaint/return → correct enum outcome + session seeded; **0 LLM calls on validation failure** (TAC-101/TAC-01); exactly **1 vision then 1 reasoning** call, in order (TAC-102); unreadable image → escalation propagates (AC-15). **Commit:** `Backend: case orchestration service`.
**Context packet:** ADR-001 §3 `CaseService`, ADR-000 §9.2 data flow, PRD §4.1/4.2/4.3, AC-09/10/11/12/13/15.

### Step 3.2 — DTOs + CaseController (POST /api/cases) + GlobalExceptionHandler
**Task:** `CaseFormDto`, `CaseResultDto`, `ErrorDto`; multipart `POST /api/cases`; `@RestControllerAdvice`
mapping validation→400(+fieldErrors), bad type→415, oversize→413, LLM upstream→502, timeout→504. Server-side
validation re-enforced (AC-09).
**TDD (integration, LLM stubbed):** valid complaint/return → 201 `CaseResultDto`; missing reason for complaint → 400 + 0 LLM calls; future date → 400; `.gif` → 415; >10 MB → 413; LLM 5xx → 502, timeout → 504, never 500 (TAC-106/109). **Commit:** `Backend: case intake controller + error handling`.
**Context packet:** ADR-001 §4 (DTOs), §5 (POST contract + validation), §8 table, ADR-000 §6, PRD AC-01..09, the **frozen contract** above. Context7: `/jakartaee/validation`.

### Step 3.3 — ChatController (SSE) + GET /api/cases/{id}
**Task:** `POST /api/cases/{id}/messages` → `text/event-stream` via `SseEmitter`: load session (404 if missing/expired),
append USER msg, stream assistant tokens (`token` events) with **full context**, append ASSISTANT msg, emit `done`;
mid-stream upstream failure → `error` event. `GET /api/cases/{id}` → `SessionViewDto` | 404. Configure async/SSE timeout.
**TDD (integration, stubbed streaming):** 3 chunks → `Content-Type: text/event-stream`, ≥1 `token` + exactly one `done`, ASSISTANT appended (TAC-104/TAC-06); unknown id → 404 `SESSION_NOT_FOUND` (TAC-105/TAC-07); blank content → 400; full context honored (AC-18/19). **Commit:** `Backend: chat SSE controller + session view`.
**Context packet:** ADR-001 §5 (SSE + GET contracts), §6 (SSE decision), §4 (SSE event shapes), ADR-003 §5 chat row + §7.5, PRD AC-17..21, frozen contract.

### Step 3.4 — WebConfig + final wiring + start proof
**Task:** `WebConfig` CORS (`APP_CORS_ALLOWED_ORIGIN`, default `http://localhost:4200`), multipart limits, async timeout.
Wire the full app; confirm it **starts** and the whole pipeline is reachable with the LLM stubbed.
**TDD:** CORS allows configured origin; app starts with only required env (TAC-108). **Verify:** `./mvnw verify` + `./mvnw spring-boot:run` smoke. **Commit:** `Backend: web config, CORS, multipart, full wiring`.
**Context packet:** ADR-001 §3 `WebConfig`, ADR-000 §7, TAC-108. **Backend track DONE after this.**

---

## Phase 4 — Frontend · `fe-developer` (runs in PARALLEL with P1–P3)

> Depends on **0.2**. Codes against the **frozen contract** with mocked `CaseService`/`ChatService`;
> needs no running backend until P5. Sequence: 4.1 → {4.2, 4.3} → {4.4, 4.5} → 4.6.

### Step 4.1 — Client models + routing + SessionState + route guard
**Task:** TS models (`RequestType`, `EquipmentCategory` value+PL label ×10, `CaseResult`, `ChatMessage`, `ApiError`);
routes `/`→form, `/chat/:sessionId`→chat; `SessionState` signal store; guard sending session-less users to `/`.
**TDD:** guard redirects when no session; model label map has 10 categories. **Commit:** `Frontend: client models, routing, session state`.
**Context packet:** ADR-002 §3 (routes/guard/SessionState), §4 (client models), PRD §8 (10 categories + PL labels), frozen contract.

### Step 4.2 — IntakeFormComponent (form + dynamic validation + image control)
**Task:** Typed reactive form: requestType (2), category (10), modelName, purchaseDate (mat-datepicker `max=today`),
reason (mat-textarea), image (drag-drop + picker, thumbnail, remove/replace). **Dynamic:** reason `required` when
COMPLAINT, cleared for RETURN, label/help updates. Client checks mirror server (type, ≤10 MB). Loading lock + duplicate-submit prevention.
**TDD:** reason-required toggling (preserve typed reason) (TAC-201); future date blocked (TAC-202); `.gif`/12 MB → inline error, **no HTTP** (TAC-203); duplicate submit prevented; Polish text. **Commit:** `Frontend: intake form with dynamic validation and image upload`.
**Context packet:** ADR-002 §3 (`IntakeFormComponent`), PRD §9.1 + AC-01..08/24, design-guidelines (inputs/buttons), frozen contract (FormData fields). Context7: `/angular/components`, `/angular/angular`.

### Step 4.3 — CaseService (HTTP) + error mapping
**Task:** `createCase(FormData): Observable<CaseResult>`; map `ErrorDto`→user-facing **Polish** messages (400/413/415/502/504); `ErrorService`/snackbar.
**TDD:** posts FormData to `/api/cases`; maps each error code to a Polish message. **Commit:** `Frontend: case service + error mapping`.
**Context packet:** ADR-002 §3/§5, frozen contract error codes, AC-23.

### Step 4.4 — ChatComponent (bubbles + first decision message + status chip)
**Task:** Render `messages` signal; first bubble = system decision via **ngx-markdown** with a visually distinct
status chip (approved/rejected/verification, NBP colors); user/assistant bubbles; CDK auto-scroll; "Nowe zgłoszenie" → `/`.
**TDD:** first bubble renders markdown headings/lists; chip matches outcome incl. escalation (TAC-204); Polish. **Commit:** `Frontend: chat UI with decision bubble and status chip`.
**Context packet:** ADR-002 §3 (`ChatComponent`), PRD §9.3 + AC-17, design-guidelines (colors for status: success `#2E7D32`, error `#C0392B`, gold/navy for verification), `error`/`success` tokens. Context7: `/jfcere/ngx-markdown`, `/angular/components`.

### Step 4.5 — ChatService (POST + streamed fetch SSE) + streaming render
**Task:** `ChatService` opens POST `/api/cases/:id/messages`, reads `text/event-stream` via fetch `ReadableStream`,
parses SSE frames → `Observable<string>` token chunks + completion + error; `SessionState` rehydrate via `GET /api/cases/:id`.
Input disabled + typing indicator during stream; reset on `done`; mid-stream error inline, prior messages intact.
**TDD:** 3 chunks → assistant bubble grows, input re-enabled on done (TAC-205); mid-stream error inline + messages intact (TAC-206); rehydrate restores; 404 → redirect `/`. **Commit:** `Frontend: streaming chat service (POST SSE) and render`.
**Context packet:** ADR-002 §3 (`ChatService`/`SessionState`), §6 (POST+fetch SSE decision), §5, frozen contract SSE shape, PRD §9.3.

### Step 4.6 — Loading/error UX, processing state, build green
**Task:** Submit→chat navigation passing initial decision; processing indicator (PL "Analizujemy zdjęcie i Twoje zgłoszenie…");
retry panel preserving form values on error (AC-23); final lint/build pass; all UI text Polish (TAC-208).
**TDD:** successful submit navigates to `/chat/:id` + renders first bubble (TAC-204); submit error → retry panel, values preserved. **Verify:** `npm test`, `npm run lint`, `npm run build` (TAC-207). **Commit:** `Frontend: loading/error UX and processing state`.
**Context packet:** ADR-002 §3 (submit flow), PRD §9.1/9.2 + AC-23/24, design-guidelines. **Frontend track DONE after this.**

---

## Phase 5 — Integration & E2E · `qa-engineer`

> **Depends on P3 done AND P4 done.** Works in `app/e2e/**`. Live key available. If QA finds a backend bug → hand a
> precise repro to `be-developer`; a frontend bug → to `fe-developer` (those fixes are their own TDD micro-steps + commits).

### Step 5.1 — Local wire-up & contract reconciliation
**Task:** Start backend (`./mvnw spring-boot:run`) + frontend (`ng serve`), confirm `/api` proxy works and the FE↔BE
contract matches in practice (field names, error bodies, SSE frames). File precise bug reports for any mismatch. **No commit** unless adding scripts.
**Context packet:** frozen contract, ADR-000 §6, `proxy.conf.json`, run commands from `app/README.md`.

### Step 5.2 — Playwright E2E (LLM stubbed/deterministic)
**Task:** Scaffold Playwright at `app/e2e/`; author the **full-flow** E2E: fill form → submit → first decision bubble
→ send chat message → streamed reply. Drive against a deterministic/stubbed LLM for stable assertions. Assert Polish UI throughout.
**TDD/spec-first:** write the E2E scenario expectations before running. **Commit:** `Tests: Playwright E2E full flow (stubbed LLM)`.
**Context packet:** ADR-000 §10 (E2E row, key scenarios), ADR-002 §8 (full-flow row + AC-22), PRD §4.1, frozen contract.

### Step 5.3 — LIVE happy-path verification
**Task:** With a real `OPENROUTER_API_KEY` + the env model IDs, run **two live flows**: (a) Return → expect a
plausible decision + well-formed first bubble (greeting/decision/justification/next-steps/**disclaimer**); (b) Complaint
with drop-damage image → reasoned outcome. Verify the two-stage pipeline actually fires and chat streams. Record results.
**Commit:** `Tests: live happy-path E2E verification (real OpenRouter)` (test artifacts/fixtures only; no secrets).
**Context packet:** PRD §4.1/4.2 + AC-16/17, ADR-003 §5 (pipeline), `.env.example` (key/model vars, kept as-is). **No secrets committed.**

### Step 5.4 — Error/escalation/AC coverage E2E
**Task:** E2E for: invalid submission (missing reason for complaint, future date, bad type, oversize, no image) → inline
errors, no AI call; LLM failure → non-technical PL error + retry preserving data (AC-23); escalation `WYMAGA_WERYFIKACJI`
first bubble lists missing info (AC-15); language mirroring + off-topic redirect (AC-20/21). **Commit:** `Tests: E2E error, escalation, and AC coverage`.
**Context packet:** PRD §4.4/4.5 + AC-15/20/21/23/24, ADR-000 §10 scenarios.

---

## Phase 6 — Final verification & close-out

> **Depends on P5.** Lead: `qa-engineer`; assists: be/fe.

- **6.1** Run full changed-scope verification both tracks: BE `./mvnw verify`; FE `npm test && npm run lint && npm run build`; E2E suite green. Start the app and confirm a real flow works (tests passing ≠ app working).
- **6.2** Tidy docs: refresh `app/README.md` status table to reflect actual state; ensure `.env.example` matches the env vars the code reads (note any divergence — e.g. add `APP_*`/`SERVER_PORT` if the backend relies on them). **Docs-only**, orchestrator-mediated to avoid lane conflicts.
- **Commit:** `Docs: update run instructions and env after PoC implementation`.
- **Done when:** PRD ACs + ADR TACs are met, unit/integration green, ≥1 live happy-path verified, app starts locally, repo is clean and reviewable on `prd1`.

---

## Task dependency matrix (blocking edges)

| Task | Depends on | Can run parallel with |
|---|---|---|
| 0.1 BE scaffold | — | 0.2 |
| 0.2 FE scaffold | — | 0.1 |
| 1.1 models | 0.1 | 0.2, all P4 |
| 1.2–1.6 units | 1.1 | each other (sequential by one agent), all P4 |
| 2.1 prompts | 1.1, 1.4 | P4 |
| 2.2 parser | 1.1 | P4 |
| 2.3 LLM client | 2.1, 2.2 | P4 |
| 3.1 orchestrator | 1.2,1.3,1.4,1.5,1.6,2.3 | P4 |
| 3.2 case ctrl | 3.1 | P4 |
| 3.3 chat ctrl | 3.1, 2.3 | P4 |
| 3.4 wire/CORS | 3.2, 3.3 | P4 |
| 4.1 models/routing | 0.2 | all of P1–P3 |
| 4.2 form / 4.3 CaseService | 4.1 | P1–P3 |
| 4.4 chat UI / 4.5 ChatService | 4.1 (4.4 also 4.2 patterns) | P1–P3 |
| 4.6 UX/build | 4.2–4.5 | P1–P3 |
| 5.1 wire-up | 3.4, 4.6 | — |
| 5.2 E2E stubbed | 5.1 | — |
| 5.3 E2E live | 5.2 | — |
| 5.4 E2E errors | 5.2 | 5.3 |
| 6.x final | 5.3, 5.4 | — |

**Conflict-prevention rules:** (1) disjoint file lanes — `app/backend/**`, `app/frontend/**`, `app/e2e/**`;
(2) the **frozen contract** is the only shared artifact and changes only via orchestrator sync; (3) root files
(`README`, `.env.example`, `docker-compose.yml`) are touched **only** in P6 by one actor; (4) every step ends in a
green, committed state before the next dependent step starts.

---

## Verification (how we'll know the PoC works)

1. **Per step:** the agent's TDD cycle (red→green) + changed-scope commands above; commit only when green.
2. **Backend track close (3.4):** `./mvnw verify` all green; `./mvnw spring-boot:run` starts; `/actuator/health` 200.
3. **Frontend track close (4.6):** `npm test`, `npm run lint`, `npm run build` all green.
4. **Integration (5.1):** both processes up; `ng serve` proxy reaches backend; contract matches live.
5. **E2E (5.2/5.4):** Playwright full flow + error/escalation/AC scenarios green against a deterministic LLM.
6. **Live (5.3):** real OpenRouter key → ≥1 Return-approved and ≥1 Complaint flow produce well-formed,
   disclaimer-bearing decisions and a streaming chat reply.
7. **Final (6.1):** entire suite green + manual app-start smoke of a real flow.

**Mapping to acceptance criteria:** Form AC-01..09 → 1.2/3.2/4.2; AI AC-10..16 → 1.3/1.6/2.1/2.2/3.1; Chat
AC-17..21 → 1.6/3.3/4.4/4.5; General AC-22..24 → 4.x/3.2/4.6. TACs covered as cited inline per step.
