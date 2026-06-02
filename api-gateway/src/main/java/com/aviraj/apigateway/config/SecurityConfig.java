package com.aviraj.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity  // Reactive security — NOT @EnableWebSecurity (that's servlet)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF — not needed for stateless JWT APIs
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Disable default login form — we have our own /auth/login
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Disable HTTP Basic auth popup
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Route authorization rules
                .authorizeExchange(exchanges -> exchanges
                        // Public routes — no token needed
                        .pathMatchers("/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        // Everything else — must be authenticated
                        // Note: actual JWT validation done in JwtAuthFilter
                        // This just tells Spring Security to allow the filter to run
                        .anyExchange().permitAll()
                )
                .build();
    }
}
