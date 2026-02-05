package com.passmanager.service;

/**
 * Servicio para gestionar migraciones de schema de base de datos.
 *
 * <p>Hibernate ddl-auto=update maneja cambios de ESTRUCTURA automáticamente
 * (agregar columnas, tablas, etc.), pero NO maneja migraciones de DATOS complejas.</p>
 *
 * <p>Este servicio:</p>
 * <ul>
 *   <li>Rastrea qué versión de schema tiene la base de datos</li>
 *   <li>Ejecuta migraciones pendientes en orden secuencial</li>
 *   <li>Registra cada migración para evitar re-ejecutarla</li>
 * </ul>
 *
 * <p>Ejemplo de uso en {@link com.passmanager.service.impl.AuthServiceImpl}:
 * el método {@code performKeyMigration()} ahora es rastreado como Migración v2.</p>
 */
public interface SchemaMigrationService {

    /**
     * Obtiene la versión actual del schema.
     *
     * @return Número de versión (0 si no hay migraciones aplicadas)
     */
    int getCurrentVersion();

    /**
     * Verifica si una migración específica ya fue aplicada.
     *
     * @param version Número de versión a verificar
     * @return true si la migración fue aplicada
     */
    boolean isMigrationApplied(int version);

    /**
     * Registra que una migración fue aplicada exitosamente.
     *
     * @param version Número de versión
     * @param description Descripción de qué hace la migración
     */
    void recordMigration(int version, String description);

    /**
     * Registra una migración con notas adicionales.
     *
     * @param version Número de versión
     * @param description Descripción corta
     * @param notes Notas técnicas detalladas
     */
    void recordMigration(int version, String description, String notes);
}
