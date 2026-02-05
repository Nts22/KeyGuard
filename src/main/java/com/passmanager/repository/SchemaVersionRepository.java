package com.passmanager.repository;

import com.passmanager.model.entity.SchemaVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemaVersionRepository extends JpaRepository<SchemaVersion, Long> {

    /**
     * Encuentra una versión específica del schema.
     */
    Optional<SchemaVersion> findByVersion(Integer version);

    /**
     * Verifica si una versión ya fue aplicada.
     */
    boolean existsByVersion(Integer version);

    /**
     * Obtiene la versión actual del schema (la más alta aplicada).
     * Retorna 0 si no hay migraciones aplicadas.
     */
    @Query("SELECT COALESCE(MAX(s.version), 0) FROM SchemaVersion s")
    Integer getCurrentVersion();
}
