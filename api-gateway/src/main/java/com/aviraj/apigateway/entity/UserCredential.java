package com.aviraj.apigateway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username must be unique — no duplicate accounts
    @Column(unique = true, nullable = false)
    private String username;

    // Stores BCrypt hash — never plain text
    @Column(nullable = false)
    private String password;

    // USER or ADMIN — for future authorization
    @Column(nullable = false)
    private String role;
}
