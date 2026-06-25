# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot application built with Maven. Application code lives under `src/main/java`, with the main class at `com.dc.ai.SpringAiTestApplication`. The `com.dc.ai` package is organized by responsibility: `controller` for HTTP endpoints, `service` for business logic, `provider` for AI provider integration, `repository` for Spring Data access, `domain` for JPA entities/enums, and `dto` for request/response objects. Configuration classes are in `src/main/java/com/dc/config`. Runtime configuration, schema, and seed data are in `src/main/resources`. Tests belong in `src/test/java`, mirroring the production package structure.

## Build, Test, and Development Commands

Use the Maven wrapper so contributors run the same Maven version:

- `.\mvnw.cmd test` runs the JUnit/Spring Boot test suite.
- `.\mvnw.cmd spring-boot:run` starts the API locally on port `8888`.
- `.\mvnw.cmd package` compiles, tests, and builds the application artifact in `target/`.

On Unix-like shells, use `./mvnw` instead of `.\mvnw.cmd`.

## Coding Style & Naming Conventions

Follow standard Java and Spring conventions: 4-space indentation, one public top-level type per file, `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep controllers thin; place orchestration and validation logic in services. Use DTOs for API payloads instead of exposing JPA entities directly. Prefer constructor injection for new Spring components.

## Testing Guidelines

Tests use JUnit 5 through `spring-boot-starter-test`. Name test classes after the unit or slice under test, such as `ChatServiceTest` or `ModelControllerTest`. Keep integration tests in matching packages under `src/test/java`; use the default H2-backed configuration unless a test specifically requires MySQL behavior. Run `.\mvnw.cmd test` before submitting changes.

## Commit & Pull Request Guidelines

This checkout does not include usable Git history, so follow a simple imperative commit style, for example `Add provider sync endpoint` or `Fix chat response mapping`. Keep each commit focused. Pull requests should include a short summary, test results, linked issues when applicable, and screenshots or sample API responses for user-visible endpoint changes.

## Security & Configuration Tips

Do not commit real API keys, database passwords, or provider tokens. Configure secrets through environment variables such as `AI_API_KEY`, `GROK_API_KEY`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Keep local overrides out of version control.
