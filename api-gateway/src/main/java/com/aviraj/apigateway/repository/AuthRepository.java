package com.aviraj.apigateway.repository;

import com.aviraj.apigateway.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<UserCredential, Long> {

    // Spring Data JPA generates:
    // SELECT * FROM user_credentials WHERE username = ?
    Optional<UserCredential> findByUsername(String username);

    // Check if username already taken during registration
    // SELECT COUNT(*) > 0 FROM user_credentials WHERE username = ?
    boolean existsByUsername(String username);
}
