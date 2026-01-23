package com.passmanager.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

@Component
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String DB_FILE_NAME = "passwords.db";

    @PostConstruct
    public void init() {
        createDatabaseDirectory();
    }

    private void createDatabaseDirectory() {
        Path dbDir = Paths.get(System.getProperty("user.home"), AppConfig.DB_FOLDER);
        Path dbFile = dbDir.resolve(DB_FILE_NAME);

        try {
            if (!Files.exists(dbDir)) {
                Files.createDirectories(dbDir);
                setRestrictivePermissions(dbDir, "rwx------");
                log.info("Directorio de base de datos creado con permisos restrictivos: {}", dbDir);
            }

            // Aplicar permisos restrictivos al archivo de base de datos si existe
            if (Files.exists(dbFile)) {
                setRestrictivePermissions(dbFile, "rw-------");
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de la base de datos: " + dbDir, e);
        }
    }

    private void setRestrictivePermissions(Path path, String permissions) {
        try {
            if (isPosixFileSystem()) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString(permissions);
                Files.setPosixFilePermissions(path, perms);
                log.debug("Permisos establecidos para {}: {}", path, permissions);
            }
        } catch (IOException e) {
            log.warn("No se pudieron establecer permisos restrictivos para {}: {}", path, e.getMessage());
        } catch (UnsupportedOperationException e) {
            log.debug("Sistema de archivos no soporta permisos POSIX, omitiendo configuración de permisos");
        }
    }

    private boolean isPosixFileSystem() {
        return !System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public void secureDatabaseFile() {
        Path dbFile = Paths.get(System.getProperty("user.home"), AppConfig.DB_FOLDER, DB_FILE_NAME);
        if (Files.exists(dbFile)) {
            setRestrictivePermissions(dbFile, "rw-------");
        }
    }
}
