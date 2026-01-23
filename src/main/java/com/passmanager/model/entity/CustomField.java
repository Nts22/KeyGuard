package com.passmanager.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "custom_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fieldName;

    @Column(nullable = false)
    private String fieldValue;

    @Column(nullable = false)
    @Builder.Default
    private boolean sensitive = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private PasswordEntry passwordEntry;
}
