package com.example.client;

import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.McpClientRegistrationRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.function.Consumer;

import static org.springaicommunity.mcp.security.client.sync.config.McpClientOAuth2Configurer.mcpClientOAuth2;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return http -> http
                .with(mcpClientOAuth2 ());
    }

}

@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(ToolCallbackProvider tcp, ChatClient.Builder ai) {
        this.ai = ai
                .defaultToolCallbacks(tcp)
                .defaultSystem("""
                        helpo people schedule appointments for picking up dogs from the Pooch Palace adoption shelter.
                        if somebody asks for a date, use the tools to get a valid appointment date and return the response without 
                        further questioning.
                        """)
                .build();
    }

    @GetMapping("/ask")
    String ask() {
        return ai.prompt()
                .user("fantastic. when might i schedule an appointment to pick up or" +
                        " adopt a dog from the San Francisco Pooch Palace location?")
                .call()
                .content();
    }
}