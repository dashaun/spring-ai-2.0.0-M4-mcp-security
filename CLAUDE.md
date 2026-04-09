# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring AI 2.0.0-M4 demo with Model Context Protocol (MCP) secured by OAuth2. Three independent Spring Boot 4.0.5 applications (Java 25) demonstrating enterprise AI tool integration with security.

## Architecture

Three-app OAuth2 security pattern:

- **authz/** (port 9090) - OAuth2 Authorization Server. Issues JWTs, manages users via `JdbcUserDetailsManager` with PostgreSQL. Uses `mcpAuthorizationServer()` configurer from mcp-security library.
- **service/** (port 8081) - MCP Server + Resource Server. Exposes `@McpTool` methods over Streamable HTTP transport. Validates JWT Bearer tokens via JWKS from authz. User identity available via `SecurityContextHolder`.
- **client/** (port 8080) - MCP Client + OAuth2 Client. User-facing app with `/ask` endpoint. Uses `ChatClient` (OpenAI) with `ToolCallbackProvider` to invoke MCP tools. `mcpClientOAuth2()` configurer handles automatic Bearer token injection on MCP calls.

**Token flow:** User -> client (authorization_code grant) -> authz (JWT issued) -> client calls service with JWT -> service validates via JWKS -> tool executes with authenticated user identity.

## Build Commands

Each module is independent (no root pom.xml). Build from within each module directory:

```bash
# Build a single module
cd authz && ./mvnw clean package
cd service && ./mvnw clean package
cd client && ./mvnw clean package

# Run a single module
cd authz && ./mvnw spring-boot:run
cd service && ./mvnw spring-boot:run
cd client && ./mvnw spring-boot:run

# Run unit tests for a single module
cd service && ./mvnw test

# Run e2e tests (requires all three apps running, or CF URLs)
cd e2e && ./mvnw clean test
# Against CF deployment:
cd e2e && ./mvnw clean test -Dauthz.url=https://... -Dservice.url=https://... -Dclient.url=https://...
```

## Local Development Prerequisites

- Java 25 (minimum 21+)
- PostgreSQL running on localhost (database: `mydatabase`, for authz)
- OpenAI API key or local Ollama instance (for client)
- All three apps must run simultaneously for full functionality

## Key Dependencies

- `spring-ai-community/mcp-security` 0.1.6-SNAPSHOT: provides `mcpAuthorizationServer()`, `mcpServerSecurity()`, `mcpClientOAuth2()` configurers
- `spring-ai-starter-mcp-server-webmvc`: MCP server runtime
- `spring-ai-starter-mcp-client`: MCP client runtime
- `spring-ai-starter-model-openai`: LLM integration
- Snapshot repositories are required (configured in each pom.xml)

## Testing

- **Unit tests:** Basic `contextLoads()` tests in each module under `src/test/java`
- **E2E tests** (`e2e/`): 31 ordered JUnit 5 tests covering authz endpoints, MCP protocol (JSON-RPC 2.0, protocol version "2025-03-26"), OAuth2 flows, cross-service token validation, and SSE response parsing. Uses raw `HttpClient` (no Spring test context).

## Deployment

Cloud Foundry deployment via `manifest.yaml`. All three apps deploy as separate CF apps with `java_buildpack_offline`. Authz binds to PostgreSQL service (`ws-authz-db`), client binds to GenAI service (`genai-service`).

## Notable Details

- Demo credentials: user `jdoe` / password `password`, client ID/secret `spring`/`spring`
- `{noop}` password encoder used (demo only)
- MCP connection name `scheduler` maps to the service's `/mcp` endpoint
- `initialized=false` on client MCP config enables lazy initialization
- Flat package structure: all classes in `com.example.{authz,service,client}`
