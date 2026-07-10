# Architecture Decision Records

ADRs for the Hardware Service Decision Copilot. Read together with [`../PRD-Product-Requirements-Document.md`](../PRD-Product-Requirements-Document.md).

| ADR | Area |
|---|---|
| [000-main-architecture.md](000-main-architecture.md) | System architecture, stack, modules, data models, cross-cutting contracts, env, global decisions, testing |
| [001-backend-api.md](001-backend-api.md) | Spring Boot layering, REST + SSE contracts, image compression, in-memory session store, validation |
| [002-frontend-angular.md](002-frontend-angular.md) | Angular + Material + ngx-markdown, intake form, custom streaming chat |
| [003-ai-llm-integration.md](003-ai-llm-integration.md) | openai-java + OpenRouter, Chat Completions decision, two-stage pipeline, prompts, structured output, streaming |

**Key cross-cutting decisions:** Chat Completions (GA) over Responses (beta) · openai-java direct (not Spring AI) · no DB / in-memory session store · custom Material chat (not a SaaS chat lib) · separate FE/BE deployables · Java 21 language level on JDK 25 · Spring Boot + Maven (wrapper) · Angular standalone + Material.
