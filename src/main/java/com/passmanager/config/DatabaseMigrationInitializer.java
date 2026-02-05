package com.passmanager.config;

import com.passmanager.service.SchemaMigrationService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Inicializador que registra las migraciones de schema disponibles al arrancar la aplicación.
 *
 * <p>Este componente se ejecuta después de que Hibernate cree/actualice las tablas
 * (gracias a {@code @Order(100)}), y registra en {@code schema_version} qué
 * migraciones están implementadas.</p>
 *
 * <h2>¿Por qué registrar migraciones al inicio?</h2>
 * <ul>
 *   <li>Documenta qué versiones de schema existen</li>
 *   <li>Permite ver el historial completo en {@code SELECT * FROM schema_version}</li>
 *   <li>Útil para debugging y auditorías</li>
 * </ul>
 *
 * <h2>Migraciones implementadas:</h2>
 * <ul>
 *   <li><strong>v1:</strong> Schema inicial (usuarios, contraseñas, categorías)</li>
 *   <li><strong>v2:</strong> Jerarquía de cifrado triple-clave (Key A/B/C)</li>
 * </ul>
 *
 * <h2>Cómo agregar una nueva migración:</h2>
 * <ol>
 *   <li>Implementa la lógica de migración en el servicio apropiado</li>
 *   <li>Agrega una llamada a {@code recordMigrationIfNeeded()} en este inicializador</li>
 *   <li>La migración se registrará automáticamente al arrancar</li>
 * </ol>
 *
 * @see SchemaMigrationService
 * @see com.passmanager.service.impl.AuthServiceImpl#performKeyMigration
 */
@Component
@Order(100) // Ejecutar DESPUÉS de que Hibernate inicialice las tablas
public class DatabaseMigrationInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationInitializer.class);

    private final SchemaMigrationService schemaMigrationService;

    public DatabaseMigrationInitializer(SchemaMigrationService schemaMigrationService) {
        this.schemaMigrationService = schemaMigrationService;
    }

    @PostConstruct
    public void registerAvailableMigrations() {
        log.info("Registrando migraciones de schema disponibles...");

        // Migración v1: Schema inicial (implícito, siempre presente)
        recordMigrationIfNeeded(1,
                "Initial schema",
                "Tablas iniciales: users, password_entries, categories, custom_fields, password_history");

        // Migración v2: Triple-key encryption hierarchy
        // Esta migración es PER-USER: cada usuario se migra al hacer login por primera vez.
        // El campo User.keyVersion rastrea el estado por usuario.
        recordMigrationIfNeeded(2,
                "Triple-key encryption hierarchy (Key A/B/C)",
                "Per-user migration: re-encrypts passwords from Key A to Key B, adds HMAC tags with Key C. " +
                "Triggered on first login for legacy users (keyVersion < 2). " +
                "See AuthServiceImpl.performKeyMigration() for implementation.");

        log.info("✓ Registro de migraciones completo. Versión actual: v{}",
                schemaMigrationService.getCurrentVersion());
    }

    /**
     * Registra una migración solo si no fue registrada previamente.
     * Esto permite que el inicializador sea idempotente (se puede ejecutar múltiples veces sin duplicar).
     */
    private void recordMigrationIfNeeded(int version, String description, String notes) {
        if (!schemaMigrationService.isMigrationApplied(version)) {
            schemaMigrationService.recordMigration(version, description, notes);
        }
    }
}
