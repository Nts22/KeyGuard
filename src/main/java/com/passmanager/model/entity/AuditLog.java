package com.passmanager.model.entity;

import com.passmanager.config.LocalDateTimeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad para registrar acciones del usuario (audit trail).
 *
 * <p>Permite rastrear:</p>
 * <ul>
 *   <li>Accesos (login, unlock, logout)</li>
 *   <li>Operaciones sobre contraseñas (view, copy, create, update, delete)</li>
 *   <li>Operaciones sensibles (export, recovery, 2FA changes)</li>
 * </ul>
 *
 * <h2>Casos de uso:</h2>
 * <ul>
 *   <li>Detectar accesos no autorizados</li>
 *   <li>Compliance y auditorías</li>
 *   <li>Investigación de incidentes de seguridad</li>
 *   <li>Estadísticas de uso</li>
 * </ul>
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_user_timestamp", columnList = "user_id,timestamp"),
    @Index(name = "idx_audit_action", columnList = "action")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que realizó la acción.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Tipo de acción realizada.
     * Valores: LOGIN, LOGOUT, UNLOCK, VIEW_PASSWORD, COPY_PASSWORD,
     * CREATE_ENTRY, UPDATE_ENTRY, DELETE_ENTRY, EXPORT, IMPORT, etc.
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType action;

    /**
     * Descripción adicional de la acción.
     * Ejemplo: "Viewed password for 'Gmail'"
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * ID de la entrada afectada (si aplica).
     * Ejemplo: al copiar contraseña, registrar el ID de la entrada.
     */
    @Column
    private Long passwordEntryId;

    /**
     * Dirección IP desde donde se realizó la acción.
     * Útil para detectar accesos desde ubicaciones inesperadas.
     * En aplicación de escritorio, puede ser "localhost" o la IP de la máquina.
     */
    @Column
    private String ipAddress;

    /**
     * Timestamp de cuándo ocurrió la acción.
     */
    @Convert(converter = LocalDateTimeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private LocalDateTime timestamp;

    /**
     * Resultado de la acción: SUCCESS, FAILURE, BLOCKED.
     */
    @Column
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ResultType result = ResultType.SUCCESS;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Tipos de acciones auditables.
     */
    public enum ActionType {
        // Autenticación
        LOGIN,
        LOGIN_FAILED,
        LOGOUT,
        UNLOCK,
        ACCOUNT_LOCKED,

        // Operaciones sobre contraseñas
        VIEW_PASSWORD,
        COPY_PASSWORD,
        REVEAL_PASSWORD,
        CREATE_ENTRY,
        UPDATE_ENTRY,
        DELETE_ENTRY,

        // Operaciones sensibles
        EXPORT_VAULT,
        IMPORT_VAULT,
        BACKUP_CREATED,
        RECOVERY_KEY_GENERATED,
        RECOVERY_KEY_USED,
        TOTP_ENABLED,
        TOTP_DISABLED,
        TOTP_VERIFIED,

        // Gestión de categorías/tags
        CREATE_CATEGORY,
        DELETE_CATEGORY,
        CREATE_TAG,
        DELETE_TAG,

        // Verificaciones
        BREACH_CHECK_RUN,
        PASSWORD_GENERATOR_USED
    }

    /**
     * Resultado de la acción.
     */
    public enum ResultType {
        SUCCESS,
        FAILURE,
        BLOCKED
    }
}
