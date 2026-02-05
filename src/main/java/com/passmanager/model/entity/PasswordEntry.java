package com.passmanager.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.passmanager.config.LocalDateTimeConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "password_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String username;

    private String email;

    @Column(nullable = false)
    private String password;

    private String url;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String hmacTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "passwordEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CustomField> customFields = new ArrayList<>();

    @OneToMany(mappedBy = "passwordEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt DESC")
    @Builder.Default
    private List<PasswordHistory> passwordHistory = new ArrayList<>();

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(updatable = false, columnDefinition = "TEXT")
    private LocalDateTime createdAt;

    @Convert(converter = LocalDateTimeConverter.class)
    @Column(columnDefinition = "TEXT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addCustomField(CustomField field) {
        customFields.add(field);
        field.setPasswordEntry(this);
    }
}
