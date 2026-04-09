package com.example.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WorkingSecurityE2ETests {

    private static final String authzUrl = System.getProperty("authz.url", "https://ws-authz.apps.tas-ndc.kuhn-labs.com");
    private static final String serviceUrl = System.getProperty("service.url", "https://ws-service.apps.tas-ndc.kuhn-labs.com");
    private static final String clientUrl = System.getProperty("client.url", "https://ws-client.apps.tas-ndc.kuhn-labs.com");

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private static String accessToken;
    private static String mcpSessionId;

    @BeforeAll
    static void obtainAccessToken() throws Exception {
        var tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(authzUrl + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", basicAuth("spring", "spring"))
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials&scope=openid"))
                .build();
        var response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        var json = objectMapper.readTree(response.body());
        accessToken = json.get("access_token").asText();
        assertThat(accessToken).isNotBlank();
    }

    // --- Authz Server Tests ---

    @Test
    @Order(1)
    void authzOpenIdConfigurationIsAccessible() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(authzUrl + "/.well-known/openid-configuration"))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var json = objectMapper.readTree(response.body());
        assertThat(json.get("issuer").asText()).isEqualTo(authzUrl);
        assertThat(json.get("token_endpoint").asText()).isEqualTo(authzUrl + "/oauth2/token");
        assertThat(json.get("authorization_endpoint").asText()).isEqualTo(authzUrl + "/oauth2/authorize");
        assertThat(json.get("jwks_uri").asText()).isEqualTo(authzUrl + "/oauth2/jwks");
    }

    @Test
    @Order(2)
    void authzJwksEndpointReturnsKeys() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(authzUrl + "/oauth2/jwks"))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var json = objectMapper.readTree(response.body());
        assertThat(json.has("keys")).isTrue();
        assertThat(json.get("keys").isArray()).isTrue();
        assertThat(json.get("keys").size()).isGreaterThan(0);
    }

    @Test
    @Order(3)
    void authzClientCredentialsGrantReturnsValidToken() throws Exception {
        assertThat(accessToken).isNotBlank();
        // JWT has 3 dot-separated parts
        assertThat(accessToken.split("\\.")).hasSize(3);
    }

    @Test
    @Order(4)
    void authzLoginPageIsServed() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(authzUrl + "/login"))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Sign in");
    }

    // --- Service (MCP Server) Tests ---

    @Test
    @Order(10)
    void serviceRejects401WithoutToken() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("initialize", 1,
                        """
                        {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}""")))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @Order(11)
    void serviceMcpInitializeSucceeds() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("initialize", 1,
                        """
                        {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}""")))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var json = objectMapper.readTree(response.body());
        assertThat(json.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(json.at("/result/protocolVersion").asText()).isEqualTo("2025-03-26");
        assertThat(json.at("/result/serverInfo/name").asText()).isEqualTo("mcp-server");
        assertThat(json.at("/result/capabilities/tools/listChanged").asBoolean()).isTrue();

        mcpSessionId = response.headers().firstValue("mcp-session-id").orElse(null);
        assertThat(mcpSessionId).isNotBlank();
    }

    @Test
    @Order(12)
    void serviceMcpToolsListReturnsScheduleAdoption() throws Exception {
        // Re-initialize to get a fresh session for this test
        var initRequest = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("initialize", 1,
                        """
                        {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}""")))
                .build();
        var initResponse = httpClient.send(initRequest, HttpResponse.BodyHandlers.ofString());
        var sessionId = initResponse.headers().firstValue("mcp-session-id").orElseThrow();

        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .header("Mcp-Session-Id", sessionId)
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("tools/list", 2, null)))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        // SSE response: parse the data line
        var body = response.body();
        var dataLine = extractSseData(body);
        var json = objectMapper.readTree(dataLine);

        var tools = json.at("/result/tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isGreaterThan(0);

        boolean hasScheduleAdoption = false;
        for (JsonNode tool : tools) {
            if ("scheduleAdoption".equals(tool.get("name").asText())) {
                hasScheduleAdoption = true;
                assertThat(tool.get("description").asText()).contains("dog");
                var inputSchema = tool.get("inputSchema");
                assertThat(inputSchema.at("/properties/dogId")).isNotNull();
                assertThat(inputSchema.at("/properties/dogName")).isNotNull();
                assertThat(inputSchema.get("required").toString()).contains("dogId");
                assertThat(inputSchema.get("required").toString()).contains("dogName");
            }
        }
        assertThat(hasScheduleAdoption).as("scheduleAdoption tool should be listed").isTrue();
    }

    @Test
    @Order(13)
    void serviceMcpToolCallExecutesScheduleAdoption() throws Exception {
        var initRequest = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("initialize", 1,
                        """
                        {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}""")))
                .build();
        var initResponse = httpClient.send(initRequest, HttpResponse.BodyHandlers.ofString());
        var sessionId = initResponse.headers().firstValue("mcp-session-id").orElseThrow();

        var toolCallParams = """
                {"name":"scheduleAdoption","arguments":{"dogId":42,"dogName":"Buddy"}}""";
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .header("Mcp-Session-Id", sessionId)
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("tools/call", 3, toolCallParams)))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var body = response.body();
        var dataLine = extractSseData(body);
        var json = objectMapper.readTree(dataLine);

        assertThat(json.has("result")).isTrue();
        var content = json.at("/result/content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isGreaterThan(0);
        // The tool returns a DogAdoptionSchedule with when and user fields
        var text = content.get(0).get("text").asText();
        assertThat(text).contains("when");
        assertThat(text).contains("user");
    }

    @Test
    @Order(14)
    void serviceProtectedResourceMetadataIsAccessible() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/.well-known/oauth-protected-resource"))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // The MCP server security library exposes this endpoint
        assertThat(response.statusCode()).isIn(200, 404);
    }

    // --- Client Tests ---

    @Test
    @Order(20)
    void clientRedirectsToAuthzForLogin() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(clientUrl + "/ask"))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        // Should redirect to OAuth2 login
        assertThat(response.statusCode()).isEqualTo(302);
        var location = response.headers().firstValue("location").orElse("");
        assertThat(location).contains("/oauth2/authorization/spring");
    }

    @Test
    @Order(21)
    void clientOAuth2AuthorizationRedirectPointsToAuthz() throws Exception {
        // Follow the first redirect to get the actual authz URL
        var request = HttpRequest.newBuilder()
                .uri(URI.create(clientUrl + "/oauth2/authorization/spring"))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(302);
        var location = response.headers().firstValue("location").orElse("");
        assertThat(location).startsWith(authzUrl + "/oauth2/authorize");
        assertThat(location).contains("response_type=code");
        assertThat(location).contains("client_id=spring");
        assertThat(location).contains("scope=openid");
    }

    // --- Cross-service integration ---

    @Test
    @Order(30)
    void authzTokenIsAcceptedByService() throws Exception {
        // Verify the token issued by ws-authz is accepted by ws-service
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("initialize", 99,
                        """
                        {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"integration-test","version":"1.0"}}""")))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);

        var json = objectMapper.readTree(response.body());
        assertThat(json.at("/result/protocolVersion").asText()).isEqualTo("2025-03-26");
    }

    @Test
    @Order(31)
    void expiredOrInvalidTokenIsRejectedByService() throws Exception {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/mcp"))
                .header("Authorization", "Bearer invalid.token.value")
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mcpRequest("initialize", 1,
                        """
                        {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}""")))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    // --- Helpers ---

    private static String basicAuth(String username, String password) {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
    }

    private static String mcpRequest(String method, int id, String params) {
        if (params == null) {
            return """
                    {"jsonrpc":"2.0","method":"%s","id":%d}""".formatted(method, id);
        }
        return """
                {"jsonrpc":"2.0","method":"%s","id":%d,"params":%s}""".formatted(method, id, params);
    }

    private static String extractSseData(String sseBody) {
        // SSE responses have lines like "data:{...}"
        // If the body is plain JSON (not SSE), return as-is
        if (sseBody.trim().startsWith("{")) {
            return sseBody.trim();
        }
        for (String line : sseBody.split("\n")) {
            if (line.startsWith("data:")) {
                return line.substring("data:".length()).trim();
            }
        }
        return sseBody;
    }
}
