# Spring AI, MCP & OAuth2 Security

### Spring AI 2.0.0-M4

DaShaun Carter | Spring Developer Advocate

Notes:
- Welcome everyone. Today we're going to cover the most exciting developments in Spring AI, the Model Context Protocol, and how to secure it all with OAuth2.
- This is a 50-minute session. We'll move through concepts, show real code, and do a live demo on Cloud Foundry.
- Ask questions anytime.

---

## Agenda

| Section | Topic | Time |
|---------|-------|------|
| 1 | Spring AI: The Journey to 2.0 | ~8 min |
| 2 | Model Context Protocol (MCP) | ~10 min |
| 3 | Spring AI + MCP Integration | ~10 min |
| 4 | Securing MCP with OAuth2 | ~12 min |
| 5 | Live Demo: Cloud Foundry | ~8 min |
| 6 | Wrap-up & Q&A | ~2 min |

Notes:
- Walk through the agenda quickly so attendees know what to expect.
- The demo is live on Cloud Foundry using Tanzu Application Service.

---

## What You'll Learn Today

- How Spring AI evolved from 1.0 GA to 2.0.0-M4
- What MCP is and why it matters for enterprise AI
- How to build MCP Servers and Clients with Spring Boot
- How to lock it all down with OAuth2
- A working 3-app deployment on Cloud Foundry

Notes:
- Emphasize that everything shown today is running in production on Cloud Foundry right now.

]]]

<!-- .slide: data-background-color="#6db33f" data-background-transition="zoom" -->
# Section 1
## Spring AI: The Journey to 2.0

Notes:
- Start with the big picture: where Spring AI came from, where it is now, and where it's headed.

---

## Spring AI Timeline

| Date       | Milestone                                            |
|------------|------------------------------------------------------|
| May 2025   | Spring AI **1.0 GA** released                        |
| Nov 2025   | Spring AI **1.1 GA** -- full MCP integration         |
| Dec 2025   | Spring AI **2.0.0-M1** -- Spring Boot 4, Framework 7 |
| Jan 2026   | **2.0.0-M2** -- MCP Server Customizers, null-safety  |
| Mar 2026   | **2.0.0-M3** -- Jackson 3, MCP annotation renames    |
| Mar 2026   | **2.0.0-M4** -- Enhanced MCP client auto-config      |
| _Apr 2026_ | _**2.0.0-M5 & 2.0.0-RC1**_                           |
| _May 2026_ | _**2.0.0** GA_                                       |

Notes:
- Spring AI 1.0 GA was a huge milestone: the portable ChatClient API, 20+ model backends, structured advisors.
- Spring AI 2.0 is built on Spring Boot 4.0, Spring Framework 7.0, Jakarta EE 11, and requires Java 17+.
- The project we're demoing today runs on Java 25 with Spring Boot 4.0.5 and Spring AI 2.0.0-M4.

---

## Spring AI 1.0 GA Highlights

- **ChatClient** -- the portable, universal LLM abstraction
- **20+ AI model backends** (OpenAI, Anthropic, AWS Bedrock, Azure, Ollama, etc.)
- **Structured Output** -- parse LLM responses into Java objects
- **Advisors API** -- intercept and enhance AI interactions (RAG, memory)
- **Tool/Function Calling** -- let LLMs invoke your Java methods
- **Vector Store integration** -- 15+ vector databases

Notes:
- ChatClient is the star here -- write once, swap models freely.
- The @Tool annotation was introduced in 1.0 M6 to simplify tool definitions.

---

## Spring AI 2.0.0-M4 -- What's New

- Built on **Spring Boot 4.0.5** / **Spring Framework 7.0**
- **Jakarta EE 11** baseline, **Java 17+** required
- **Jackson 3** migration (new module coordinates)
- **JSpecify null-safety** with NullAway compile-time enforcement
- Enhanced **MCP client auto-configuration**
- Security fixes (CVE-2026-22738, CVE-2026-22742, CVE-2026-22743, CVE-2026-22744)
- **MCP annotation package renames** (breaking change from M3)

Notes:
- If you're migrating from 1.x, the Jackson 3 and MCP annotation renames are the biggest breaking changes.
- The null-safety improvements from JSpecify are a great DX improvement for IDE users.

---

## The Stack We're Using Today

```text
+--------------------------------------------+
|            Spring Boot 4.0.5               |
+--------------------------------------------+
|           Spring AI 2.0.0-M4               |
+--------------------------------------------+
|     MCP Java SDK  |  MCP Security 0.1.6   |
+--------------------------------------------+
|        Spring Security  |  OAuth2          |
+--------------------------------------------+
|              Java 25                       |
+--------------------------------------------+
```

Notes:
- Point out the community mcp-security library at version 0.1.6-SNAPSHOT.
- This stack is bleeding edge but production-ready on Cloud Foundry.

]]]

<!-- .slide: data-background-color="#191e1e" data-background-transition="zoom" -->
# Section 2
## Model Context Protocol (MCP)

Notes:
- Now let's dig into MCP itself -- what it is, why Anthropic created it, and why Spring adopted it.

---

## What is MCP?

> The **Model Context Protocol** is an open protocol that standardizes how applications provide context to LLMs.

- Created by **Anthropic** (November 2024)
- Think of it as **USB-C for AI** -- one standard plug for everything
- Official SDKs: Python, TypeScript, Java, Kotlin
- Java MCP SDK created by and supported by the **Spring** team

Notes:
- Before MCP, every AI integration was bespoke. MCP gives us a standard way to connect LLMs to tools, data, and services.
- The Java SDK started as a Spring experimental project and became the official Anthropic-endorsed implementation.

---

## MCP Architecture

```text
+------------------+        +------------------+
|                  |        |                  |
|    MCP Host      |        |   MCP Server     |
|  (AI App)        |------->|  (Your Service)  |
|                  |  MCP   |                  |
|  +------------+  |Protocol|  +------------+  |
|  | MCP Client |  |------->|  | Tools      |  |
|  +------------+  |        |  | Resources  |  |
|                  |        |  | Prompts    |  |
+------------------+        +------------------+
```

- **Host** -- the user-facing AI application
- **Client** -- handles protocol-level communication
- **Server** -- exposes capabilities (tools, resources, prompts)

Notes:
- A single Host can connect to multiple MCP Servers through multiple Clients.
- Each Client connects to exactly one Server.
- This is the same pattern as language servers in IDEs (LSP).

---

## MCP Capabilities

| Capability | Description | Example |
|------------|-------------|---------|
| **Tools** | Functions the LLM can invoke | Schedule appointment, query DB |
| **Resources** | Standardized data exposure | File contents, API data |
| **Prompts** | Reusable prompt templates | System prompts, workflows |

Notes:
- Tools are the most commonly used capability -- they're what let the LLM actually DO things.
- Resources and Prompts are newer and gaining adoption.

---

## MCP Transport Options

| Transport | Protocol | Use Case |
|-----------|----------|----------|
| **stdio** | Process pipes | Local tools, CLI integrations |
| **SSE** | Server-Sent Events | Legacy HTTP streaming |
| **Streamable HTTP** | HTTP request/response | Modern standard, production |

We're using **Streamable HTTP** today.

Notes:
- Streamable HTTP is the recommended transport for production deployments.
- SSE is still supported for backward compatibility but Streamable HTTP is the future.
- stdio is great for local dev tools like IDE integrations.

---

## Spring.io Blog Coverage of MCP

| Date | Blog Post |
|------|-----------|
| Dec 2024 | [Announcing Spring AI MCP -- Java SDK](https://spring.io/blog/2024/12/11/spring-ai-mcp-announcement/) |
| Feb 2025 | [Introducing the MCP Java SDK](https://spring.io/blog/2025/02/14/mcp-java-sdk-released-2/) |
| Apr 2025 | [Securing MCP Servers with OAuth2](https://spring.io/blog/2025/04/02/mcp-server-oauth2/) |
| May 2025 | [Dynamic Tool Updates in MCP](https://spring.io/blog/2025/05/04/spring-ai-dynamic-tool-updates-with-mcp/) |
| May 2025 | [MCP Client OAuth2 in Practice](https://spring.io/blog/2025/05/19/spring-ai-mcp-client-oauth2/) |
| Sep 2025 | [MCP Boot Starters Introduction](https://spring.io/blog/2025/09/16/spring-ai-mcp-intro-blog/) |
| Sep 2025 | [Securing MCP Servers (mcp-security)](https://spring.io/blog/2025/09/30/spring-ai-mcp-server-security/) |

Notes:
- The Spring team has been very active publishing MCP content.
- Daniel Garnier-Moiroux and Christian Tzolov are the primary authors.
- The spring-ai-community/mcp-security project is the culmination of this work.

]]]

<!-- .slide: data-background-color="#6db33f" data-background-transition="zoom" -->
# Section 3
## Spring AI + MCP Integration

Notes:
- Let's look at real code. Everything here comes from a working project deployed on Cloud Foundry.

---

## Project Structure: Three Applications

```text
demo/
  authz/       <-- OAuth2 Authorization Server (port 9090)
  service/     <-- MCP Server + Resource Server (port 8081)
  client/      <-- MCP Client + OAuth2 Client  (port 8080)
  e2e/         <-- End-to-end tests
```

Each application is a separate Spring Boot app with its own `pom.xml`.

Notes:
- This is a realistic enterprise architecture -- separate concerns, separate deployments.
- The authz server manages identity, the service exposes AI tools, the client orchestrates.

---

## Parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.5</version>
</parent>
<properties>
    <java.version>25</java.version>
    <spring-ai.version>2.0.0-M4</spring-ai.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Notes:
- Spring Boot 4.0.5 is the latest. Spring AI BOM manages all AI dependency versions.
- Java 25 is used here but Java 17+ is the minimum requirement.

---

## Building an MCP Server

### Dependencies (service/pom.xml)

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-server-security-spring-boot</artifactId>
    <version>0.1.6-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Notes:
- spring-ai-starter-mcp-server-webmvc is the official Spring AI MCP server starter.
- mcp-server-security-spring-boot is from the spring-ai-community project.
- These two starters give you everything you need for a secure MCP server.

---

## The @McpTool Annotation

```java
@Service
class SchedulerService {

    @McpTool(description = "schedule an appointment to pick up "
        + "or adopt a dog from a Pooch Palace location")
    DogAdoptionSchedule scheduleAdoption(
            @McpToolParam int dogId,
            @McpToolParam String dogName) {

        var user = Objects.requireNonNull(
            SecurityContextHolder.getContext()
                .getAuthentication())
            .getName();

        var das = new DogAdoptionSchedule(
            Instant.now().plus(3, ChronoUnit.DAYS), user);

        return das;
    }
}

record DogAdoptionSchedule(Instant when, String user) {}
```

Notes:
- @McpTool marks a method as an MCP tool that LLMs can discover and invoke.
- @McpToolParam annotates parameters -- Spring AI auto-generates the JSON schema.
- Notice SecurityContextHolder -- we'll see how OAuth2 populates this in Section 4.
- The method returns a record that gets serialized back through MCP to the LLM.

---

## MCP Server Configuration

```properties
# service/application.properties
spring.application.name=service
server.port=8081
spring.ai.mcp.server.protocol=streamable
spring.security.oauth2.resourceserver.jwt.issuer-uri=
    http://localhost:9090
```

Just **4 lines** of configuration:

1. Name the app
2. Set the port
3. Enable Streamable HTTP protocol
4. Point to the OAuth2 issuer for JWT validation

Notes:
- This is the beauty of Spring Boot auto-configuration.
- The MCP server starter auto-discovers @McpTool annotated beans and registers them.
- The JWT issuer URI tells Spring Security where to validate incoming tokens.

---

## Building an MCP Client

### Simple Client (no MCP tools)

```java
@RestController
class AssistantController {

    private final ChatClient ai;

    AssistantController(ChatClient.Builder ai) {
        this.ai = ai
            .defaultSystem("Help people by answering questions.")
            .build();
    }

    @GetMapping("/ask")
    String ask() {
        return ai.prompt()
            .user("What is Cloud Foundry?")
            .call()
            .content();
    }
}
```

Notes:
- This is a basic ChatClient without MCP integration -- just talking to OpenAI directly.
- The ChatClient.Builder is auto-configured by spring-ai-starter-model-openai.
- This is the starting point before we add MCP tool integration.

---

## Adding MCP + OAuth2: Client Dependencies

To go from simple ChatClient to MCP with security, add these:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-client-security-spring-boot</artifactId>
    <version>0.1.6-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

Notes:
- spring-ai-starter-mcp-client provides ToolCallbackProvider and the MCP client runtime.
- mcp-client-security-spring-boot adds mcpClientOAuth2() for automatic Bearer token attachment.
- The OAuth2 client starter handles the authorization code flow.
- These three starters together give you a secured MCP client.

---

## Secured Client (with MCP tools + OAuth2)

```java
@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(ToolCallbackProvider tcp,
                        ChatClient.Builder ai) {
        this.ai = ai
            .defaultToolCallbacks(tcp)
            .defaultSystem("""
                Help people schedule appointments for
                picking up dogs from the Pooch Palace
                adoption shelter. If somebody asks for
                a date, use the tools to get a valid
                appointment date and return the response
                without further questioning.
                """)
            .build();
    }

    @GetMapping("/ask")
    String ask() {
        return ai.prompt()
            .user("When might I schedule an appointment "
                + "to adopt a dog from the San Francisco "
                + "Pooch Palace location?")
            .call()
            .content();
    }
}
```

Notes:
- ToolCallbackProvider is the key addition -- it auto-discovers MCP tools from connected servers.
- defaultToolCallbacks(tcp) registers all discovered tools with the ChatClient.
- The system prompt guides the LLM to use the tools appropriately.
- When the LLM needs to schedule an appointment, it calls the MCP tool on the service.

---

## MCP Client Configuration

```properties
# client/application.properties (local defaults)
spring.ai.mcp.client.streamable-http
    .connections.scheduler.url=http://localhost:8081
spring.ai.mcp.client.initialized=false

# OAuth2 client registration
spring.security.oauth2.client.provider
    .spring.issuer-uri=http://localhost:9090
spring.security.oauth2.client.registration
    .spring.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration
    .spring.scope=openid,profile
```

Notes:
- The connection name "scheduler" maps to the MCP server's tool namespace.
- initialized=false defers MCP connection until first use -- avoids startup failures if the MCP server isn't up yet.
- On Cloud Foundry, these defaults are overridden via environment variables in the manifest.
- Properties shown wrapped for readability -- they're single lines in the actual file.

---

## The Complete Flow

```text
  User
   |
   v
+---------+    OAuth2    +---------+    JWT     +---------+
|         |   Token      |         |   Token    |         |
| Client  |<------------>|  Authz  |            |         |
| :8080   |              |  :9090  |            |         |
|         |              +---------+            |         |
|         |                                     |         |
|  Chat   |--- prompt --->  OpenAI              |         |
|  Client |<-- tool call --  (LLM)              |         |
|         |                                     |         |
|   MCP   |--- tool exec (+ JWT) ------------->| Service |
|  Client |<-- tool result --------------------| :8081   |
|         |                                     |  MCP    |
|  Chat   |--- result --->  OpenAI              | Server  |
|  Client |<-- response --  (LLM)              |         |
|         |                                     |         |
+---------+                                     +---------+
```

Notes:
- Walk through the flow step by step:
1. User authenticates with authz via OAuth2 authorization code flow
2. Client gets an access token (JWT)
3. User hits /ask on the client
4. Client sends prompt to OpenAI
5. OpenAI recognizes it needs the scheduleAdoption tool
6. Client forwards the tool call to the MCP server with the JWT
7. Service validates the JWT, executes the tool, returns the result
8. Client sends the result back to OpenAI
9. OpenAI generates the natural language response
10. Client returns it to the user

]]]

<!-- .slide: data-background-color="#191e1e" data-background-transition="zoom" -->
# Section 4
## Securing MCP with OAuth2

Notes:
- This is where it gets really interesting. The MCP spec (2025-03-26) mandates OAuth2 for HTTP-exposed servers.
- Let's look at how Spring makes this easy.

---

## Why Secure MCP?

- MCP servers exposed over HTTP **must** be secured (per spec)
- Tools can **modify data**, **schedule actions**, **access user info**
- Without auth, anyone can invoke your tools
- OAuth2 provides:
  - **Identity** -- who is calling?
  - **Authorization** -- what can they do?
  - **Audit trail** -- what did they do?

Notes:
- In our example, the scheduleAdoption tool accesses the authenticated user's identity.
- Without security, there's no way to know WHO is scheduling the appointment.
- The MCP spec mandates Bearer tokens on every request.

---

## The Three-App Security Architecture

```text
+-------------------+
|   Authorization   |
|     Server        |
|    (authz)        |
|                   |
|  Issues JWTs      |
|  Manages users    |
|  OAuth2 flows     |
+--------+----------+
         |
    JWT  |  JWT
  issued |  validated
         |
+--------v----------+        +-------------------+
|                    |  MCP   |                   |
|   MCP Client       |------->|   MCP Server      |
|   (client)         |+ JWT   |   (service)       |
|                    |        |                   |
|  OAuth2 Client     |        |  Resource Server  |
|  ChatClient        |        |  @McpTool         |
+--------------------+        +-------------------+
```

Notes:
- Three distinct security roles, each a separate Spring Boot application.
- Authorization Server issues tokens.
- Client is an OAuth2 client that obtains tokens.
- Service is a Resource Server that validates tokens.

---

## The mcp-security Community Project

Three Spring Boot starters from `spring-ai-community/mcp-security`:

| Module | Role | Artifact |
|--------|------|----------|
| **Authorization Server** | Issues tokens | `mcp-authorization-server-spring-boot` |
| **Server Security** | Validates tokens | `mcp-server-security-spring-boot` |
| **Client Security** | Obtains tokens | `mcp-client-security-spring-boot` |

All at version **0.1.6-SNAPSHOT**

Notes:
- This is a community project incubated by the Spring AI team.
- It provides auto-configuration that integrates MCP with Spring Security.
- Supports OAuth2 authorization code, client credentials, and hybrid flows.
- Also supports API key authentication as an alternative.

---

## Authorization Server Setup

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>
      spring-boot-starter-security-oauth2-authorization-server
    </artifactId>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>mcp-authorization-server-spring-boot</artifactId>
    <version>0.1.6-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

Notes:
- We build on Spring Authorization Server -- the official OAuth2 authorization server for Spring.
- The mcp-authorization-server-spring-boot adds MCP-specific metadata endpoints and configuration.
- PostgreSQL stores user accounts and OAuth2 client registrations.

---

## Authorization Server Code

```java
@SpringBootApplication
public class AuthzApplication {

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(
            DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) {
        return http
            .authorizeHttpRequests(authz ->
                authz.anyRequest().authenticated())
            .with(mcpAuthorizationServer(), mcp ->
                mcp.authorizationServer(springAuthServer ->
                    springAuthServer
                        .oidc(Customizer.withDefaults())))
            .formLogin(Customizer.withDefaults())
            .build();
    }
}
```

Notes:
- The key is .with(mcpAuthorizationServer()) -- this single line adds MCP-specific OAuth2 configuration.
- It configures the MCP metadata endpoint, token endpoints, and OIDC discovery.
- JdbcUserDetailsManager gives us persistent user storage in PostgreSQL.
- formLogin provides the login page for the authorization code flow.

---

## Authorization Server Configuration

```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          spring:
            registration:
              client-id: "spring"
              client-secret: "{noop}spring"
              client-authentication-methods:
                - "client_secret_basic"
              authorization-grant-types:
                - "authorization_code"
                - "client_credentials"
                - "refresh_token"
              redirect-uris:
                - "http://127.0.0.1:8080/login/oauth2/code/spring"
              scopes:
                - "openid"
                - "profile"
  ai:
    mcp:
      authorizationserver:
        dynamic-client-registration:
          enabled: false
```

Notes:
- Standard Spring Authorization Server client registration.
- We support both authorization_code (user-interactive) and client_credentials (machine-to-machine).
- Dynamic client registration (RFC 7591) is disabled -- we manage clients statically.
- The redirect URI points to the client application's OAuth2 callback.

---

## MCP Server as Resource Server

The service validates JWTs on every MCP request:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=
    http://localhost:9090
spring.ai.mcp.server.protocol=streamable
```

That's it. Spring Security + mcp-server-security-spring-boot handles:

- JWT signature validation via JWKS endpoint
- Token expiry checks
- Populating `SecurityContextHolder` with user identity
- Rejecting requests without valid Bearer tokens

Notes:
- The auto-configuration from mcp-server-security-spring-boot sets up a SecurityFilterChain that protects MCP endpoints.
- Every MCP tool invocation gets the authenticated user from the JWT.
- The @McpTool method can access SecurityContextHolder.getContext().getAuthentication().getName().

---

## MCP Client OAuth2 Integration

```java
@SpringBootApplication
public class ClientApplication {

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return http -> http
            .with(mcpClientOAuth2());
    }
}
```

### Client Configuration (local defaults)

```properties
spring.security.oauth2.client.provider
    .spring.issuer-uri=http://localhost:9090
spring.security.oauth2.client.registration
    .spring.client-id=spring
spring.security.oauth2.client.registration
    .spring.client-secret=spring
spring.security.oauth2.client.registration
    .spring.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration
    .spring.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration
    .spring.scope=openid,profile
```

Notes:
- mcpClientOAuth2() configures the MCP client to automatically attach Bearer tokens to MCP requests.
- The client uses authorization_code grant type -- the user logs in via the authz server's form.
- After login, the client stores the access token and uses it for all MCP server calls.
- {baseUrl} and {registrationId} are Spring Security placeholders resolved at runtime.
- On CF, these are overridden to use https:// URLs via manifest environment variables.

---

## OAuth2 Flows Supported

### Authorization Code (User-Interactive)

```text
User --> Client --> Authz (login form) --> Client (token)
                                            |
                                            v
                                    MCP Server (+ JWT)
```

Best for: **User-facing applications** where individual identity matters

### Client Credentials (Machine-to-Machine)

```text
Client --> Authz (client_id + secret) --> Client (token)
                                            |
                                            v
                                    MCP Server (+ JWT)
```

Best for: **Service-to-service** communication, batch processing

Notes:
- Our demo uses authorization_code because we want to know which user is scheduling the adoption.
- Client credentials would be used if the client itself is the actor (no human involved).
- The mcp-security library supports both flows and a hybrid approach.

---

## Security Context Flow

```text
1. User logs in via authz (OAuth2 authorization code)
2. Authz issues JWT with user claims:
   { "sub": "jdoe", "scope": "openid profile" }

3. Client receives JWT, stores in session

4. User asks: "Schedule a dog adoption"

5. Client sends MCP tool call to service
   Authorization: Bearer eyJhbGciOi...

6. Service validates JWT via authz JWKS endpoint

7. @McpTool method reads user from SecurityContext:
   SecurityContextHolder.getContext()
       .getAuthentication().getName()
   --> "jdoe"

8. Adoption scheduled for user "jdoe"
```

Notes:
- This is the key insight: the user's identity flows from login through the LLM tool call all the way to the MCP server.
- The LLM never sees the JWT -- it's handled by the MCP transport layer.
- This means your tools can make authorization decisions based on WHO is calling.

]]]

<!-- .slide: data-background-color="#6db33f" data-background-transition="zoom" -->
# Section 5
## Live Demo: Cloud Foundry

Notes:
- Time for the live demo! We have all three apps deployed on Tanzu Application Service.
- Show the CF apps, then walk through the flow.

---

## Cloud Foundry Deployment

### Three Apps on Tanzu Application Service

| App | URL | Services |
|-----|-----|----------|
| **ws-authz** | ws-authz.apps.tas-ndc.kuhn-labs.com | ws-authz-db |
| **ws-service** | ws-service.apps.tas-ndc.kuhn-labs.com | -- |
| **ws-client** | ws-client.apps.tas-ndc.kuhn-labs.com | genai-service |

Notes:
- All three apps are deployed with the java_buildpack_offline.
- The authz server binds to a PostgreSQL service for user/client storage.
- The client binds to genai-service for OpenAI API access via Tanzu's GenAI tile.
- The "ws-" prefix stands for "working-security" -- the full MCP + OAuth2 integration.

---

## Combined Manifest (working-security)

```yaml
applications:
- name: ws-authz
  memory: 1G
  buildpack: java_buildpack_offline
  path: authz/target/authz-0.0.1-SNAPSHOT.jar
  services:
    - ws-authz-db
  env:
    SERVER_PORT: 8080
    SPRING_AI_MCP_AUTHORIZATIONSERVER_DYNAMIC_CLIENT_REGISTRATION_ENABLED: "false"

- name: ws-service
  memory: 1G
  buildpack: java_buildpack_offline
  path: service/target/service-0.0.1-SNAPSHOT.jar
  env:
    SERVER_PORT: 8080
    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI:
      https://ws-authz.apps.tas-ndc.kuhn-labs.com
    SPRING_AI_MCP_SERVER_PROTOCOL: streamable

- name: ws-client
  memory: 1G
  buildpack: java_buildpack_offline
  path: client/target/client-0.0.1-SNAPSHOT.jar
  services:
    - genai-service
  env:
    SERVER_PORT: 8080
    SPRING_AI_MCP_CLIENT_STREAMABLE_HTTP_CONNECTIONS_SCHEDULER_URL:
      https://ws-service.apps.tas-ndc.kuhn-labs.com
    SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_SPRING_ISSUER_URI:
      https://ws-authz.apps.tas-ndc.kuhn-labs.com
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_SPRING_REDIRECT_URI:
      https://ws-client.apps.tas-ndc.kuhn-labs.com/login/oauth2/code/spring
```

Notes:
- All three apps deployed from a single manifest.yaml -- one `cf push`.
- The ws-authz redirect URI points back to ws-client for the OAuth2 callback.
- ws-service validates JWTs from ws-authz.
- ws-client connects to both ws-authz (OAuth2) and ws-service (MCP).
- genai-service provides OpenAI API access via Tanzu's GenAI tile.

---

## Demo Time!

### What we'll show:

1. `cf apps` -- verify all three apps are running
2. Visit `https://ws-client.apps.tas-ndc.kuhn-labs.com/ask`
3. Get redirected to ws-authz login page (OAuth2 flow)
4. Log in, get redirected back to ws-client
5. See the AI response with scheduled adoption date
6. Check ws-service logs for `@McpTool` execution

```bash
cf apps
cf logs ws-client --recent
cf logs ws-service --recent
```

Notes:
- Run `cf apps` first to show all three ws-* apps are running and their URLs.
- Navigate to the ws-client /ask endpoint in a browser.
- You'll be redirected to the ws-authz login form -- this is the OAuth2 authorization code flow.
- After login, the client gets the JWT, calls OpenAI, which calls the MCP tool, and returns the response.
- Check the ws-service logs to see the tool execution with the authenticated user.
- If the demo fails, show the E2E test output as a backup.

---

## What Just Happened

```text
 1. Browser    --> ws-client/ask
 2. ws-client  --> 302 to ws-authz/oauth2/authorize
 3. Browser    --> ws-authz login form
 4. User       --> submits credentials
 5. ws-authz   --> 302 to ws-client/login/oauth2/code/spring
 6. ws-client  --> exchanges auth code for JWT
 7. ws-client  --> sends prompt to OpenAI
 8. OpenAI     --> returns tool_call: scheduleAdoption
 9. ws-client  --> MCP request to ws-service (+ Bearer JWT)
10. ws-service --> validates JWT, runs @McpTool
11. ws-service --> returns DogAdoptionSchedule
12. ws-client  --> sends tool result to OpenAI
13. OpenAI     --> generates natural language response
14. ws-client  --> returns to browser
```

Notes:
- 14 steps, but the user only sees: login, wait, result.
- The complexity is handled entirely by Spring AI, MCP, and Spring Security.

]]]

<!-- .slide: data-background-color="#191e1e" data-background-transition="zoom" -->
# Recap

Notes:
- Let's summarize what we covered.

---

## Key Takeaways

1. **Spring AI 2.0.0-M4** is built on Spring Boot 4, Framework 7, Java 17+
2. **MCP** standardizes LLM-to-tool communication (like USB-C for AI)
3. **@McpTool** makes it trivial to expose Java methods as AI tools
4. **OAuth2 security** flows through MCP -- user identity reaches your tools
5. **mcp-security** community starters handle authz server, resource server, and client
6. **Cloud Foundry** deployment is straightforward with manifest.yaml

Notes:
- Reinforce these six points.

---

## The Code is Minimal

| Component | Lines of Java | Lines of Config |
|-----------|:------------:|:---------------:|
| Authorization Server | ~15 | ~20 |
| MCP Server (Service) | ~25 | ~4 |
| MCP Client | ~25 | ~10 |
| **Total** | **~65** | **~34** |

**~100 lines** to build a secure, OAuth2-protected, AI-powered application with MCP tool integration.

Notes:
- This is the power of Spring Boot auto-configuration and the mcp-security starters.
- Compare this to building the same thing from scratch -- it would be thousands of lines.

---

## Resources

- **Spring AI Docs**: spring.io/projects/spring-ai
- **MCP Spec**: modelcontextprotocol.io
- **MCP Java SDK**: github.com/modelcontextprotocol/java-sdk
- **MCP Security**: github.com/spring-ai-community/mcp-security
- **Spring AI Blog Posts**: spring.io/blog (search "MCP")

### Key Blog Posts

- "Securing Spring AI MCP Servers with OAuth2" (Apr 2025)
- "MCP Authorization in Practice" (May 2025)
- "Securing MCP Servers with Spring AI" (Sep 2025)
- "Connect Your AI to Everything: MCP Boot Starters" (Sep 2025)

Notes:
- Share these links with attendees.
- The mcp-security GitHub repo has excellent README documentation with examples.

]]]

<!-- .slide: data-background-color="#6db33f" data-background-transition="zoom" -->
# Thank You!

### Questions?

DaShaun Carter | @dashaun

Notes:
- Open the floor for Q&A.
- If time permits, offer to show additional code or configuration details.
- Remind attendees that the code is available and running on Cloud Foundry.
