package com.aviraj.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    // Secret key injected from application.properties
    // Never hardcode secrets in code — always externalize
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Convert string secret → cryptographic key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ─── Generate Token ───────────────────────────────────────────
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)                          // who this token is for
                .issuedAt(new Date())                       // when created
                .expiration(new Date(System.currentTimeMillis() + expiration)) // when expires
                .signWith(getSigningKey())                  // sign with HS256
                .compact();                                 // build the token string
    }

    // ─── Validate Token ───────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            getClaims(token); // throws exception if invalid/expired
            return true;
        } catch (Exception e) {
            return false;     // expired, tampered, or malformed
        }
    }

    // ─── Extract Username ─────────────────────────────────────────
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // ─── Extract All Claims ───────────────────────────────────────
    // Claims = all data inside the JWT payload
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // verify signature
                .build()
                .parseSignedClaims(token)      // parse and validate
                .getPayload();                 // get the payload data
    }
}
