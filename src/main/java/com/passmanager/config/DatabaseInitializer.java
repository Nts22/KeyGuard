package com.passmanager.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inicializador estático que crea el directorio de la base de datos
 * antes de que Spring Boot intente conectar.
 */
public class DatabaseInitializer {

    static {
        createDatabaseDirectory();
    }

    private static void createDatabaseDirectory() {
        Path dbPath = Paths.get(System.getProperty("user.home"), AppConfig.DB_FOLDER);
        if (!Files.exists(dbPath)) {
            try {
                Files.createDirectories(dbPath);
                System.out.println("Directorio de base de datos creado: " + dbPath);
            } catch (IOException e) {
                throw new RuntimeException("No se pudo crear el directorio de la base de datos: " + dbPath, e);
            }
        }
    }

    public static void init() {
        // El bloque static ya se ejecutó
    }
}
