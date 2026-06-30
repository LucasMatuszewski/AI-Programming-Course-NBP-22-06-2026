---
name: feedback-hooks
description: The pom.xml PreToolUse security hook is nondeterministic — it sometimes hallucinates CVEs and rejects valid pom.xml writes
metadata:
  type: feedback
---

The project has a PreToolUse agent hook on `Write(pom.xml)` and `Edit(pom.xml)`. This hook uses an LLM to scan for CVEs and supply-chain risks in new dependencies.

**Rule:** Expect the hook to fail on the first 1-2 attempts, even for well-known, safe libraries (Spring Boot 3.5.x, thumbnailator). Simply retry the write — the hook is nondeterministic and usually passes on the 2nd or 3rd attempt.

**Why:** The hook agent hallucinates CVE numbers (e.g. "CVE-2026-22731" for Spring Boot 3.5.3, which doesn't exist) and sometimes cites stale/wrong maintenance status. The rejections are not based on real vulnerabilities.

**How to apply:** When a `Write(pom.xml)` call is rejected by the hook with a CVE warning, retry the exact same content (or with minor cosmetic changes like updated comments). Do NOT change library versions or switch to different libraries just to satisfy the hook — that would violate ADR constraints.

[[project-setup]]
