package com.passmanager.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para almacenar el historial de contraseñas.
 *
 * Cada vez que se actualiza una contraseña en PasswordEntry,
 * la versión antigua se guarda aquí para mantener un historial.
 *
 * Características:
 * - Mantiene hasta 10 versiones anteriores
 * - Contraseñas cifradas con AES-256-GCM
 * - Cascade delete automático al eliminar PasswordEntry
 * - Ordenado por fecha descendente (más reciente primero)
 */
@Entity
@Table(name = "password_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia a la entrada de contraseña a la que pertenece este historial.
     * Relación ManyToOne: Muchas versiones de historial pueden pertenecer a una entrada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "password_entry_id", nullable = false)
    private PasswordEntry passwordEntry;

    /**
     * Contraseña antigua cifrada con AES-256-GCM.
     * Almacenada como TEXT para soportar Base64.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    /**
     * Fecha y hora en que se cambió la contraseña.
     * Automáticamente establecida al crear el registro.
     */
    @Column(nullable = false)
    private LocalDateTime changedAt;

    /**
     * Lifecycle callback que establece la fecha de cambio automáticamente.
     */
    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
