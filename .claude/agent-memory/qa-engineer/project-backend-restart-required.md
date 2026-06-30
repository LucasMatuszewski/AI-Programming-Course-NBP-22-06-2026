---
name: project-backend-restart-required
description: Backend must be restarted when controllers are compiled after startup — PID 9056 was started without web controllers
metadata:
  type: project
---

The backend running on port 8080 (PID 9056, started 2026-06-25 ~10:28) was started BEFORE the web controller code was compiled into `target/classes`. The JVM loads classes at startup and does NOT hot-reload new `.class` files.

Result: `POST /api/cases` returns 404 (Spring default), not 201/4xx from our controllers.

**Why:** The controller code (`CaseController`, `ChatController`, `GlobalExceptionHandler`) was committed and compiled after the backend started. The running JVM has no mapping for `/api/cases`.

**Fix:** Kill PID 9056 and restart with proper env vars from `.env`:
```
OPENROUTER_API_KEY=<from .env> ./mvnw spring-boot:run
```

**How to apply:** Before any E2E run, verify `GET /actuator/health` returns UP AND `POST /api/cases` returns a proper 4xx (our ErrorDto format `{code, message}`) not Spring's default 404.
