package com.passmanager.service.impl;

import com.passmanager.model.entity.SchemaVersion;
import com.passmanager.repository.SchemaVersionRepository;
import com.passmanager.service.SchemaMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación del servicio de migraciones de schema.
 *
 * <p>Mantiene un registro de todas las migraciones aplicadas en la tabla
 * {@code schema_version}, permitiendo rastrear el historial completo de
 * transformaciones de datos.</p>
 */
@Service
public class SchemaMigrationServiceImpl implements SchemaMigrationService {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationServiceImpl.class);

    private final SchemaVersionRepository schemaVersionRepository;

    public SchemaMigrationServiceImpl(SchemaVersionRepository schemaVersionRepository) {
        this.schemaVersionRepository = schemaVersionRepository;
    }

    @Override
    public int getCurrentVersion() {
        return schemaVersionRepository.getCurrentVersion();
    }

    @Override
    public boolean isMigrationApplied(int version) {
        return schemaVersionRepository.existsByVersion(version);
    }

    @Override
    @Transactional
    public void recordMigration(int version, String description) {
        recordMigration(version, description, null);
    }

    @Override
    @Transactional
    public void recordMigration(int version, String description, String notes) {
        if (isMigrationApplied(version)) {
            log.warn("Migración v{} ya fue aplicada previamente", version);
            return;
        }

        SchemaVersion schemaVersion = SchemaVersion.builder()
                .version(version)
                .description(description)
                .notes(notes)
                .appliedAt(LocalDateTime.now())
                .build();

        schemaVersionRepository.save(schemaVersion);
        log.info("✓ Migración v{} aplicada: {}", version, description);
    }
}
