package com.aviraj.apigateway.controller;

import com.aviraj.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    // ─── Login ────────────────────────────────────────────────────
    // In real system → validate against user DB
    // For now → hardcoded credentials (we'll integrate with user-service later)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // TODO: replace with real user validation via user-service
        if ("aviraj".equals(username) && "pass123".equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", username,
                    "message", "Login successful"
            ));
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials"));
    }

    // ─── Validate Token ───────────────────────────────────────────
    // Useful for debugging — check if your token is valid
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7); // remove "Bearer "

        if (jwtUtil.validateToken(token)) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", jwtUtil.extractUsername(token)
            ));
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "error", "Invalid or expired token"));
    }
}
