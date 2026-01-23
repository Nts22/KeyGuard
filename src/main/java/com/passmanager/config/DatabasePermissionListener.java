package com.passmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabasePermissionListener {

    private static final Logger log = LoggerFactory.getLogger(DatabasePermissionListener.class);

    private final DatabaseConfig databaseConfig;

    public DatabasePermissionListener(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.debug("Aplicando permisos restrictivos al archivo de base de datos...");
        databaseConfig.secureDatabaseFile();
    }
}
