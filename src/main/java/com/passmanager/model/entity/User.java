package com.passmanager.model.entity;

import com.passmanager.config.LocalDateTimeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String salt;

    @Column(columnDefinition = "TEXT")
    private String recoveryKeyHash;

    @Column(columnDefinition = "TEXT")
    private String encryptedMasterPassword;

    @Column
    private Integer keyVersion;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PasswordEntry> passwordEntries = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(updatable = false, columnDefinition = "TEXT")
    private LocalDateTime createdAt;

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDateTime lastLoginAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
