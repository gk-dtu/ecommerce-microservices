package com.aviraj.apigateway.filter;

import com.aviraj.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Routes that don't need a token
    private static final List<String> PUBLIC_ROUTES = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/validate",
            "/actuator/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ── Skip filter for public routes ─────────────────────────
        if (isPublicRoute(path)) {
            log.debug("Public route — skipping JWT check: {}", path);
            return chain.filter(exchange);
        }

        // ── Check Authorization header exists ─────────────────────
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete(); // stop — return 401
        }

        // ── Extract and validate token ────────────────────────────
        String token = authHeader.substring(7); // remove "Bearer "

        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete(); // stop — return 403
        }

        // ── Token valid — add username to header for downstream ───
        // Downstream services can read X-User-Name header
        // to know who made the request without re-validating JWT
        String username = jwtUtil.extractUsername(token);
        log.debug("Valid JWT for user: {} accessing: {}", username, path);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Name", username)
                        .build())
                .build();

        return chain.filter(mutatedExchange); // pass to next filter/service
    }

    @Override
    public int getOrder() {
        return -1; // run this filter FIRST before all other filters
    }

    private boolean isPublicRoute(String path) {
        return PUBLIC_ROUTES.stream()
                .anyMatch(path::startsWith);
    }
}
