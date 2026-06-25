# PoC Implementation Plan — Hardware Service Decision Copilot

## Context

`app/` is currently an **empty scaffold** — no backend or frontend code exists. Everything around it is ready: the PRD, four ADRs, the two Polish policy documents (`docs/policies/`), the NBP design tokens + fonts + logo (`assets/`), and `.env.example`. The goal of this plan is to deliver a **fully working proof of concept** of the Hardware Service Decision Copilot — a Spring Boot + Angular app where a customer submits an electronics return/complaint with one photo and receives a preliminary, advisory LLM decision, then chats with the agent.

This is an **orchestration plan**. The orchestrator (me) does **not** write code. All implementation is delegated to three pre-contextualized subagents — `be-developer`, `fe-developer`, `qa-engineer` — each of which already knows the full stack, TDD rules, verification commands, and required ADR reading. Task prompts therefore carry only **task-specific acceptance criteria + cross-agent contracts**, never restated stack rules.

The intended outcome: full coverage of all PRD acceptance criteria, built test-first, committed in small focused steps, with backend and frontend progressing in parallel through isolated git worktrees and re-joining for end-to-end QA.

## Locked decisions (from clarification)

| Decision | Choice |
|---|---|
| LLM access | Live OpenRouter key available, **free to use**; models taken **verbatim** from env vars (no pre-flight check) |
| Scope | **Full coverage** — both scenarios, all 4 decision categories, all validation/error paths, retry, streaming chat, off-topic |
| Execution | **Parallel in isolated git worktrees** after a shared contract step; merge per milestone |
| Angular version | **Pin to Angular 18** (Material + ngx-markdown pinned to the same major) |
| Git | Commit each step onto current branch `electronic-complains-chat-app`; worktrees branch off it and merge back; **no push** |
| TDD | Test-first, confirm red, implement minimum, verify green, refactor green |

## Source-of-truth documents (orchestrator quotes the relevant slice into each task prompt)

- `docs/PRD-Product-Requirements-Document.md` — functional behavior, acceptance criteria AC-01..AC-27.
- `docs/ADR/000-main-architecture.md` — system, data models, global testing strategy, TAC-01..TAC-10.
- `docs/ADR/001-backend-api.md` — endpoints, DTOs, error model, TAC-001-01..08.
- `docs/ADR/002-llm-integration.md` — gateway, prompts, structured outputs, TAC-002-01..08.
- `docs/ADR/003-frontend.md` — screens, SSE consumption, validators, TAC-003-01..10.
- `docs/design-guidelines.md` + `assets/design-tokens.json` — NBP brand tokens/fonts.
- `docs/policies/polityka-zwrotow.md`, `docs/policies/polityka-reklamacji.md` — agent rules.

---

## Orchestration principles

1. **Minimal context per task.** Each delegated task gets: the exact ACs/TACs it must satisfy (quoted), the relevant contract slice, the file paths it owns, the verification command to run, and the commit message. Not the whole PRD/ADR set.
2. **Contract-first decoupling.** A single checked-in contract artifact + shared fixtures lets the FE track run **fully concurrently** with the BE track. The only true FE→BE runtime dependency is the final "re-point to live proxy" step.
3. **Worktree discipline.** After the fork, **no dev worktree edits any root or `docs/**` file.** The orchestrator owns root/docs/plan files on the integration branch and merges them down before each fork. This makes policies and contract read-only inputs, not conflict sources.
4. **Commit cadence.** One focused commit per step (`Backend:`/`Frontend:`/`QA:`/`Docs:`). Merge per **milestone**, not per commit. Re-fork fresh worktrees off the updated integration branch at each milestone. Keep milestones ≤ ~10 commits.
5. **Each step is verified before commit** with the scope-appropriate command; an agent reports red→green honestly.

---

## Critical path (true serialization points)

```
Phase 0 (foundation + contract freeze)  ──►  M0 merge  ──►  FORK two worktrees
                                                              │
                        ┌─────────────────────────────────────┴───────────────┐
                  BE track (Phase 1 ► 3 ► 4.1-4.2)                  FE track (Phase 2, concurrent)
                        └─────────────────────────────┬───────────────────────┘
                                                  M4 merge (rejoin)
                                                       │
                                          Phase 4.3 FE re-point to live proxy
                                                       │
                                          Phase 5 QA (smoke + E2E + live smoke)
                                                       │
                                          Phase 6 polish + README + final merge
```

**Must exist before forking:** generated Maven wrapper, both apps scaffolded green, shared contract artifact, shared fixtures (incl. SSE transcript), policy-load decision, WireMock conventions.
**Must be merged before QA E2E:** BE contract endpoints + LLM integration + streaming; FE intake + chat; deterministic stubbed-LLM run profile.

---

## Phase 0 — Foundation & contract freeze (shared branch, before fork)

> Goal: both apps compile/test green, and every cross-agent seam is frozen as a checked-in artifact so the two tracks never block each other.

| Step | Agent | Deliverable | Verify | Commit |
|---|---|---|---|---|
| 0.1 | be | Scaffold `app/backend` (Spring Boot 3.5, Java 21, Maven), **generate `mvnw`/`mvnw.cmd`** honoring `.gitattributes` (LF for `mvnw`, CRLF for `*.cmd`), `/health` via Actuator | `./mvnw -v` then `./mvnw test` | `Backend: scaffold Spring Boot app + Maven wrapper` |
| 0.2 | be | Wire test stack (JUnit 5, Mockito, AssertJ, Spring Boot Test, MockMvc, **WireMock**) + one passing smoke test | `./mvnw test` | `Backend: add test harness (JUnit5, Mockito, WireMock)` |
| 0.3 | fe | Scaffold `app/frontend` (**Angular 18**, Material + ngx-markdown pinned to v18), `proxy.conf.json` → `:8080` | `npm test && npm run lint && npm run build` | `Frontend: scaffold Angular 18 app + Material + proxy` |
| 0.4 | orchestrator | **Contract artifact** `docs/contracts/api-contract.md`: enums (CaseType, the PRD §8 equipment list, the 4 DecisionCategory values, ImageAnalysis fields), DTO shapes, error model `{code,message,fields?}` + status codes, **exact SSE frame format** | review | `Docs: freeze API contract for PoC` |
| 0.5 | orchestrator | **Shared fixtures** `app/fixtures/`: valid JPEG/PNG/WebP, >10MB generator, one `ImageAnalysis` + one `DecisionResult` JSON **per scenario × per category** (8), and a chunked **SSE transcript** (incl. a frame split across chunk boundaries + terminal `done` + mid-stream `error`) | review | `Docs: add shared test fixtures + SSE transcript` |
| 0.6 | orchestrator | Decide & document **policy-load path** (copy into `backend/src/main/resources` vs read repo path) and any `.gitignore` additions | review | `Docs: decide policy-load strategy` |

**M0 — merge 0.1–0.6 into `electronic-complains-chat-app`, then fork two worktrees** (one for be-developer, one for fe-developer).

---

## Phase 1 — Backend core, LLM stubbed (be worktree)

> All steps use an **in-memory stub `LlmGateway`** and **WireMock**; no live LLM. Each step: test first → red → implement → `./mvnw test` green → commit.

1. **1.1 ImageCompressor** — downscale to configured max long edge, re-encode JPEG; output smaller than input, long edge ≤ max, tiny image not upscaled. *(TAC-001-02, TAC-001-06)*
2. **1.2 SessionStore** interface + InMemory impl — create/get/appendMessage/exists, concurrent append.
3. **1.3 PolicyProvider** — returns return policy for `ZWROT`, complaint policy for `REKLAMACJA`.
4. **1.4 Request DTO + Bean Validation** — required/blank, `requiredIfComplaint` reason, future-date rejected, max-lengths. *(AC-03..AC-06)*
5. **1.5 GlobalExceptionHandler** — 400 VALIDATION_ERROR (+`fields`), 415 UNSUPPORTED_MEDIA_TYPE, 413 PAYLOAD_TOO_LARGE; multipart max-size config. *(TAC-001-08)*
6. **1.6 `POST /cases` integration (stub gateway)** — happy + each validation failure asserts **0 outbound LLM calls** via WireMock count. *(TAC-001-01, TAC-01/02/03)*
7. **1.7 MessageComposer** — first message = greeting + body + **mandatory disclaimer**, for every category. *(TAC-001-04, TAC-001-05, AC-24)*
8. **1.8 Decision enum coercion** — any out-of-set category → `NEEDS_HUMAN_REVIEW`. *(TAC-04)*
9. **1.9 `GET /cases/{id}`** — summary + transcript; 404 unknown.
10. **1.10 `POST /cases/{id}/messages` SSE (stub stream)** — `Content-Type: text/event-stream`, ≥1 token + terminal `done` (tested against the **Phase-0 SSE transcript**); 404 before any LLM; 400 empty message. *(TAC-001-05, TAC-001-07)*
11. **1.11 Deterministic stubbed-LLM run profile** (e.g. Spring profile `stub-llm`) — boots the real backend with a canned `LlmGateway` returning per-category fixtures; **this is the QA E2E backend.** *(supports ADR-000 §10 E2E strategy)*

**M1 — merge be worktree → integration branch.**

---

## Phase 2 — Frontend core (fe worktree, CONCURRENT with Phase 1)

> Runs against the **contract + fixtures only**, not a live backend. Each step test-first (`*.spec.ts`).

1. **2.1 Models** — TS types/enums mirroring the contract artifact (CaseType, EquipmentCategory + Polish labels, DecisionCategory, SubmitCaseResponse, ChatMessage, ApiError).
2. **2.2 Validators** — `futureDateForbidden`, `requiredIfComplaint`, file type/size. *(TAC-003-01/02/03)*
3. **2.3 api.service** — multipart submit + `ApiError` normalization (busy/error states). *(TAC-003-05)*
4. **2.4 SSE parser** — `fetch()`+`ReadableStream`+`TextDecoder`, tested against the **Phase-0 SSE transcript** incl. split-frame + `error` + `done`. *(TAC-003-06)* — highest-risk seam, do early.
5. **2.5 Intake component** — Reactive Form, file preview, objectURL revoke on destroy/replace, inline validation, submit/loading/error with **data preserved on error**. *(TAC-003-04/05, AC-07/08)*
6. **2.6 Navigate + first message render** — markdown render with **disclaimer always visible**; `missingInfo` shown for MORE_INFO_REQUIRED. *(TAC-003-08, AC-20)*
7. **2.7 Chat component** — streaming bubble accumulation + typing indicator only while streaming + decision-category visual highlight + unknown-category guard. *(TAC-003-07)*
8. **2.8 Case-summary header + Polish-label audit** — all UI text Polish; NBP design tokens/fonts applied. *(TAC-003-09, AC-25)*

**M2 — merge fe worktree → integration branch** (alongside M1).

---

## Phase 3 — LLM integration (be worktree, after M1)

> WireMock for all tests; **live key used for manual prompt iteration only** (outside the suite).

1. **3.1 OpenAiClientConfig** — explicit `.baseUrl()` + key precedence (`OPENAI_API_KEY` wins, else `OPENROUTER_API_KEY`); **never `fromEnv()`**, never api.openai.com; optional attribution headers. *(TAC-002-01/02)*
2. **3.2 Model routing** — vision→`OPENROUTER_VISION_MODEL`, decide/chat→`OPENROUTER_TEXT_MODEL`, fallback `OPENROUTER_MODEL`. *(TAC-002-03)*
3. **3.3 analyzeImage** — base64 `data:image/jpeg` image_url; structured `ImageAnalysis` per scenario. *(TAC-002-06, AC-11/12/13)*
4. **3.4 decide** — structured `DecisionResult`; **full policy text present in request payload**; per-category parse. *(TAC-002-04/05, AC-14/16/17)*
5. **3.5 Retry/fail-closed** — transient 5xx/429/timeout retried to bound, then gateway exception → **502/503 with no session persisted**. *(TAC-001-06, TAC-002-07)*
6. **3.6 `/chat/completions` only** — assert `/responses` never called. *(TAC-002-08, TAC-10)*
7. **3.7 PromptCatalog** — 4 prompts + chat system prompt, Polish output, escalation rules (never invent facts; insufficient/contradictory → MORE_INFO_REQUIRED/NEEDS_HUMAN_REVIEW). *(AC-18/19)* Wire real gateway into CaseService; full submit per scenario.

**M3 — merge be worktree.**

---

## Phase 4 — Streaming chat end-to-end

1. **4.1 streamChat (be)** — WireMock SSE token stream + mid-stream error → SSE `error` event.
2. **4.2 ChatService (be)** — append user msg → build full context (form+analysis+decision+transcript) → stream → append assistant msg; bounded executor for emitters. *(AC-21/22, off-topic decline AC-23)*
3. **M4 — merge be + fe worktrees → integration branch (rejoin).**
4. **4.3 FE re-point (fe)** — point at the real `/api` proxy; verify incremental SSE render against the running backend. *(only true FE→BE dependency)*

---

## Phase 5 — QA (integration branch, qa-engineer)

1. **5.0** Confirm/boot the deterministic `stub-llm` run profile (from 1.11) for reproducible all-category coverage.
2. **5.1 Manual smoke** (Playwright MCP) — boot real BE (`stub-llm`) + FE, walk form→decision→chat, screenshots at each step, compare to wireframes + NBP design system; document bugs (no automated tests yet).
3. **5.2 Automated E2E** (real stack, LLM stubbed) — return+complaint happy paths, all 4 decision categories, validation 400/413/415, LLM-unavailable 502/503 with **retry + data preserved**, incremental SSE render, **disclaimer always present**, off-topic decline-and-redirect. *(ADR-000 §10 scenarios)*
4. **5.3 Single live smoke** — one real OpenRouter run proving the live provider path (return + complaint), treated as path-smoke, not category assertion.

---

## Phase 6 — Integration polish & docs

- Fix any defects QA filed (routed to be/fe-developer with the failing test reproduced first).
- `README` run instructions (two dev processes + proxy + env vars).
- Design polish pass against `design-guidelines.md`.
- Final merge to `electronic-complains-chat-app`. (No push unless explicitly requested.)

---

## Dependency matrix

| Task | Agent | Depends on |
|---|---|---|
| 0.1 BE scaffold + mvnw | be | — |
| 0.2 BE test harness | be | 0.1 |
| 0.3 FE scaffold | fe | — |
| 0.4 contract artifact | orchestrator | — |
| 0.5 fixtures + SSE transcript | orchestrator | 0.4 |
| 0.6 policy-path + gitignore | orchestrator | — |
| **M0 merge + fork** | orchestrator | 0.1–0.6 |
| Phase 1 (BE core, stub) | be | M0 |
| Phase 2 (FE core) | fe | M0 (**not** Phase 1) |
| Phase 3 (LLM) | be | M1 |
| Phase 4.1–4.2 (BE stream) | be | M1 |
| **M4 rejoin** | orchestrator | Phase 3, 4.1–4.2, M2 |
| 4.3 FE re-point | fe | M4 |
| 5.0 stub run profile | qa (+ be 1.11) | M4 |
| 5.1–5.3 E2E | qa | M4 + 5.0 |
| Phase 6 | all | Phase 5 |

---

## LLM usage by phase

| Phase | LLM source |
|---|---|
| 0–2 | None (BE in-memory stub gateway; FE uses fixtures only — FE never touches the LLM, TAC-003-10/TAC-09) |
| 1 / 3 / 4 integration tests | **WireMock** (canned structured outputs, 5xx fail-closed, SSE stream, `/responses`-never) |
| 3 prompt iteration | **Live** (manual, outside the suite) |
| 5.2 E2E suite | Deterministic `stub-llm` run profile (stable all-category coverage) |
| 5.3 + 6 | **Live** OpenRouter, once each (path smoke) |

---

## Worktree / merge hazards

- Disjoint `app/backend` vs `app/frontend` ⇒ near-zero code conflicts. Real risk is **shared root/docs files**: `.gitignore`, `.gitattributes`, `.env*`, `docs/**`, `README.md`, `app/README.md`, `.mcp.json`, `.claude/**`.
- **Rule:** dev worktrees never edit root/docs files; orchestrator owns them and merges down before each fork.
- If policies are *copied* into `backend/src/main/resources` (0.6), `docs/policies` stays the single source — keep them in sync, don't diverge.
- `mvnw` generation must honor `.gitattributes` line endings (Windows host + Git Bash).

---

## Top risks & mitigations

1. **Missing `mvnw` breaks every BE/QA command** → Phase-0 gate 0.1; verify `./mvnw -v` before any other BE step.
2. **No deterministic backend profile → flaky/expensive E2E, can't hit all 4 categories** → make `stub-llm` profile a Phase-1 deliverable (1.11), reused by QA (5.0).
3. **SSE wire-format drift between isolated worktrees** → freeze byte-level SSE fixture in Phase 0 (0.5); steps 1.10 and 2.4 both test against it.
4. **FE blocking on BE** → contract (0.4) + fixtures (0.5) make Phase 2 fully concurrent; only 4.3 needs a live backend.
5. **Structured-output schema mismatch (ImageAnalysis/DecisionResult)** → derive both BE classes and FE models from the single contract artifact; per-category fixtures catch drift.
6. **Live-model nondeterminism on category boundaries** → assert category logic only against stubs; live runs are path-smoke only.

---

## Verification (end-to-end, before final merge)

- **Backend:** `cd app/backend && ./mvnw test && ./mvnw clean package` — all green; TAC-001-* and TAC-002-* covered.
- **Frontend:** `cd app/frontend && npm test && npm run lint && npm run build` — all green; TAC-003-* covered.
- **E2E:** boot BE (`stub-llm`) + FE, run Playwright suite — full form→decision→chat journey + validation/error/retry/off-topic paths green.
- **Live smoke:** one real OpenRouter run each for return and complaint completes form→decision→chat.
- **Manual:** open `http://localhost:4200`, confirm Polish UI, NBP branding, disclaimer on every decision, retry preserves form data.

---

## Per-task delegation template (how each agent is briefed)

Every delegated task prompt contains exactly these blocks (and nothing more):

```
TASK: <one step, e.g. "1.1 ImageCompressor">
CONTEXT SLICE: <only the ACs/TACs + contract fields this step needs, quoted>
FILES YOU OWN: <specific paths under app/backend or app/frontend>
DO NOT TOUCH: root files, docs/**, the other app/ subtree
TDD: write the failing test first (cite the AC/TAC), confirm red, implement minimum, verify green
VERIFY: <exact command>
COMMIT: <exact message, e.g. "Backend: add ImageCompressor with downscale + JPEG re-encode">
REPORT BACK: red→green evidence + the commit hash
```
