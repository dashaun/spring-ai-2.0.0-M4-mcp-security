# Spring AI 2.0.0-M4 + MCP + OAuth2 Security

A production-ready demonstration of [Spring AI 2.0.0-M4](https://spring.io/projects/spring-ai) with [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) secured by OAuth2. Three independent Spring Boot 4.0.5 applications show how an LLM can invoke server-side tools through MCP with full OAuth2 token propagation.

## Architecture

```
┌─────────────┐     authorization_code     ┌─────────────┐
│             │◄──────────────────────────► │             │
│   client    │        OAuth2 flow         │    authz    │
│  (port 8080)│                            │ (port 9090) │
│             │                            │             │
└──────┬──────┘                            └──────┬──────┘
       │                                          │
       │  MCP Streamable HTTP                     │ JWKS
       │  + JWT Bearer token                      │
       │                                          │
       ▼                                          │
┌─────────────┐     validates JWT via       ──────┘
│             │◄──────────────────────────
│   service   │
│  (port 8081)│
│             │
└─────────────┘
```

| App | Role | Description |
|-----|------|-------------|
| **authz** | OAuth2 Authorization Server | Issues JWTs, manages users with PostgreSQL, OIDC discovery |
| **service** | MCP Server + Resource Server | Exposes `@McpTool` methods over Streamable HTTP, validates JWT Bearer tokens |
| **client** | MCP Client + OAuth2 Client | User-facing `/ask` endpoint, bridges ChatClient (OpenAI/Ollama) to MCP tools with automatic token injection |

### Token Flow

1. User accesses `/ask` on the **client**
2. Client redirects to **authz** for login (authorization_code grant)
3. User authenticates (`jdoe` / `password`), authz issues a JWT
4. Client sends prompt to OpenAI, which decides to call the `scheduleAdoption` MCP tool
5. Client forwards the tool call to **service** with the JWT Bearer token
6. Service validates the JWT via JWKS from authz, executes the tool with the authenticated user identity
7. Result flows back through the LLM to the user

## Prerequisites

- **Java 25** (minimum 21+)
- **PostgreSQL** running on localhost (database: `mydatabase`, user: `myuser`, password: `secret`)
- **OpenAI API key** or a local [Ollama](https://ollama.ai) instance (defaults to `llama3.2` on `localhost:11434`)

## Quick Start

Start all three apps in separate terminals:

```bash
# Terminal 1 - Authorization Server (requires PostgreSQL)
cd authz && ./mvnw spring-boot:run

# Terminal 2 - MCP Service
cd service && ./mvnw spring-boot:run

# Terminal 3 - Client (with OpenAI)
SPRING_AI_OPENAI_API_KEY=sk-... cd client && ./mvnw spring-boot:run

# Or with local Ollama (default, no env vars needed)
cd client && ./mvnw spring-boot:run
```

Then open http://localhost:8080/ask in a browser. You'll be redirected to login -- use `jdoe` / `password`.

## Project Structure

```
spring-ai-2.0.0-M4-mcp-security/
├── authz/          # OAuth2 Authorization Server
├── service/        # MCP Server + Resource Server
├── client/         # MCP Client + OAuth2 Client
├── e2e/            # End-to-end integration tests
├── docs/           # Presentation slides
└── manifest.yaml   # Cloud Foundry deployment
```

Each module is an independent Maven project with its own `./mvnw` wrapper (no root pom).

## Building

```bash
# Build all modules
for dir in authz service client; do (cd $dir && ./mvnw clean package); done

# Build a single module
cd service && ./mvnw clean package
```

## Testing

```bash
# Unit tests for a single module
cd service && ./mvnw test

# End-to-end tests (requires all three apps running)
cd e2e && ./mvnw clean test

# E2E against a Cloud Foundry deployment
cd e2e && ./mvnw clean test \
  -Dauthz.url=https://ws-authz.apps.example.com \
  -Dservice.url=https://ws-service.apps.example.com \
  -Dclient.url=https://ws-client.apps.example.com
```

The E2E suite contains 31 ordered JUnit 5 tests covering OIDC discovery, JWKS, client credentials, MCP protocol (JSON-RPC 2.0), OAuth2 redirect flows, and cross-service token validation.

## Key Technologies

- **Spring Boot 4.0.5** / Spring Framework 7.0
- **Spring AI 2.0.0-M4** -- ChatClient, `@McpTool`, `@McpToolParam`, ToolCallbackProvider
- **MCP Java SDK** -- Streamable HTTP transport, protocol version `2025-03-26`
- **mcp-security 0.1.6-SNAPSHOT** ([spring-ai-community](https://github.com/spring-ai-community)) -- `mcpAuthorizationServer()`, `mcpServerSecurity()`, `mcpClientOAuth2()` configurers
- **Spring Security** -- OAuth2 Authorization Server, Resource Server, OAuth2 Client

## Cloud Foundry Deployment

```bash
# Build all JARs first
for dir in authz service client; do (cd $dir && ./mvnw clean package -DskipTests); done

# Deploy
cf push
```

The `manifest.yaml` deploys three apps (`ws-authz`, `ws-service`, `ws-client`) with `java_buildpack_offline`. Required services:
- `ws-authz-db` -- PostgreSQL instance bound to authz
- `genai-service` -- OpenAI-compatible API bound to client

## Presentation

```bash
cd docs && jwebserver -p 8000
```

Then open http://localhost:8000 in a browser.

## Demo Credentials

| What | Value |
|------|-------|
| User | `jdoe` / `password` |
| OAuth2 Client ID | `spring` |
| OAuth2 Client Secret | `spring` |

These use the `{noop}` password encoder and are for demonstration only.

## License

This project is a demonstration/reference application.
