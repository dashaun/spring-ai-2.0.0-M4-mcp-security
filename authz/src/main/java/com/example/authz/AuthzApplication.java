package com.example.authz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

import static org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer.mcpAuthorizationServer;

@SpringBootApplication
public class AuthzApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthzApplication.class, args);
    }

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
                .with(mcpAuthorizationServer(), mcp -> mcp.authorizationServer(springAuthServer -> springAuthServer.oidc(Customizer.withDefaults())))
                .formLogin(Customizer.withDefaults())
                .build();
    }

}
