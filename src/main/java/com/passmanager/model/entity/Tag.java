package com.passmanager.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad para tags/etiquetas que permiten organización flexible de entradas.
 *
 * <p>A diferencia de categorías (1 por entrada), una entrada puede tener múltiples tags.
 * Ejemplos: "urgente", "trabajo", "compartida", "2fa-enabled", etc.</p>
 */
@Entity
@Table(name = "tags", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Color del tag en formato hex (#ff5733).
     * Usado para mostrar pills de colores en la UI.
     */
    @Column
    private String color;

    /**
     * Usuario propietario del tag.
     * Cada usuario tiene su propio conjunto de tags.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Entradas que tienen este tag.
     * Relación many-to-many (una entrada puede tener múltiples tags,
     * un tag puede estar en múltiples entradas).
     */
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PasswordEntry> entries = new ArrayList<>();
}