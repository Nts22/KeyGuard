package com.passmanager.model.entity;

import com.passmanager.config.LocalDateTimeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para rastrear versiones del schema y migraciones aplicadas.
 *
 * <p>Esto es necesario para migraciones complejas que Hibernate ddl-auto=update
 * no puede manejar automáticamente, como:</p>
 * <ul>
 *   <li>Re-cifrado de datos existentes (v1→v2 triple-key)</li>
 *   <li>Transformaciones de datos con lógica de negocio</li>
 *   <li>Cálculo de valores derivados sobre datos cifrados</li>
 *   <li>Fusión/división de columnas con datos existentes</li>
 * </ul>
 *
 * <p>Cada migración se registra aquí para evitar re-ejecutarla.</p>
 */
@Entity
@Table(name = "schema_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número de versión de la migración (1, 2, 3, ...).
     * Debe ser único y secuencial.
     */
    @Column(nullable = false, unique = true)
    private Integer version;

    /**
     * Descripción corta de qué hace esta migración.
     * Ejemplo: "Triple-key encryption hierarchy migration"
     */
    @Column(nullable = false)
    private String description;

    /**
     * Fecha y hora cuando se aplicó esta migración.
     */
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDateTime appliedAt;

    /**
     * Notas adicionales sobre la migración.
     * Ejemplo: detalles técnicos, advertencias, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }
}
