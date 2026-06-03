package com.aviraj.apigateway.service;

import com.aviraj.apigateway.entity.UserCredential;
import com.aviraj.apigateway.repository.AuthRepository;
import com.aviraj.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtUtil jwtUtil;

    // BCryptPasswordEncoder with strength 12
    // strength = cost factor — higher = slower = harder to brute force
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    // ─── Register ─────────────────────────────────────────────────
    public Map<String, String> register(String username, String password) {

        // Check username not already taken
        if (authRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }

        // Hash password with BCrypt — never store plain text
        // BCrypt automatically generates random salt + embeds in hash
        String hashedPassword = passwordEncoder.encode(password);
        log.debug("Password hashed successfully for user: {}", username);

        // Build and save credential
        UserCredential credential = UserCredential.builder()
                .username(username)
                .password(hashedPassword)   // storing hash, not "password"
                .role("USER")               // default role
                .build();

        authRepository.save(credential);
        log.info("New user registered: {}", username);

        return Map.of(
                "message", "User registered successfully",
                "username", username
        );
    }

    // ─── Login ────────────────────────────────────────────────────
    public Map<String, String> login(String username, String password) {

        // Find user by username
        Optional<UserCredential> credentialOpt = authRepository.findByUsername(username);

        if (credentialOpt.isEmpty()) {
            // Don't say "username not found" — security best practice
            // Always say "invalid credentials" to prevent username enumeration
            throw new RuntimeException("Invalid credentials");
        }

        UserCredential credential = credentialOpt.get();

        // BCrypt extracts salt from stored hash
        // hashes entered password with same salt
        // compares — returns true/false
        if (!passwordEncoder.matches(password, credential.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Credentials valid — generate JWT
        String token = jwtUtil.generateToken(username);
        log.info("User logged in successfully: {}", username);

        return Map.of(
                "token", token,
                "username", username,
                "role", credential.getRole(),
                "message", "Login successful"
        );
    }
}
