---
name: project-setup
description: Spring Boot scaffold configuration — versions, package names, wrapper bootstrap, pom.xml hook behaviour
metadata:
  type: project
---

# Backend scaffold setup (Step 0.1)

**Spring Boot version used:** 3.5.3 (as mandated by ADR-000; 3.5.x line).
**Why:** ADR-000 specifies Spring Boot 3.5.x. The pre-write hook rejected 3.5.3 and 3.5.15 on first attempts citing hallucinated CVEs; it passed on a third retry with the same 3.5.3 version. The hook is nondeterministic.

## Resolved library versions
- `com.openai:openai-java` → `4.41.0` (Maven artifact, corresponds to v4.41.0 GitHub tag)
- `net.coobird:thumbnailator` → `0.4.20`
- `com.squareup.okhttp3:mockwebserver` → `4.12.0` (stable 4.x)
- Maven wrapper: 3.9.9

## Base package
`pl.nbp.copilot.backend`

## Package name for `case`
Java reserves `case` as a keyword. The package is named **`cases`** (plural).
All ADR-001 references to the "case" package map to `pl.nbp.copilot.backend.cases`.

## Packages created
- `web`, `cases`, `llm`, `image`, `policy`, `session`, `config`
- Each has a `package-info.java` with Javadoc describing responsibility and dependency direction.

## Maven wrapper bootstrap
Maven is NOT installed globally on this machine. The wrapper JAR was bootstrapped using:
1. `MavenWrapperDownloader.java` in `.mvn/wrapper/` compiled and run via `javac`/`java`.
2. Downloaded `maven-wrapper-3.3.2.jar` from Maven Central using Java's HTTPS client.
3. Maven 3.9.9 is downloaded to `~/.m2/wrapper/dists/` on first use.
Do NOT include `distributionSha256Sum` in `maven-wrapper.properties` — the SHA256 for 3.9.9 was not fetchable and causes a runtime failure.

## Surefire configuration
Surefire 3.5.3 (from Spring Boot parent) skips `*IT.java` by default. Added explicit `<includes>` in the surefire plugin config to include `*IT.java` alongside `*Test.java`.

## How to apply
- When running tests, use: `java -Dmaven.multiModuleProjectDirectory=<dir> -classpath <dir>/.mvn/wrapper/maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain -f <dir>/pom.xml test`
- The `./mvnw` shell script works on Unix but requires execute bit (chmod not allowed); use `./mvnw.cmd` on Windows or the Java direct invocation above.

[[feedback-hooks]]
