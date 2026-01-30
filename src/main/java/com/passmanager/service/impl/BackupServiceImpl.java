package com.passmanager.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.passmanager.model.dto.BackupDTO;
import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.BackupService;
import com.passmanager.service.CategoryService;
import com.passmanager.service.PasswordEntryService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementación del servicio de backup con cifrado AES-256-GCM.
 * Versión 1.1 con salt global y UUIDs.
 *
 * <h2>Cambios en v1.1</h2>
 * - Salt global para todo el backup (una sola derivación de clave)
 * - UUID único para cada entrada
 * - Metadata de cifrado en objeto crypto separado
 * - IV sigue siendo único por entrada para máxima seguridad
 *
 * <h2>Seguridad del formato</h2>
 * - Salt global: 16 bytes aleatorios para todo el backup
 * - IV único: 12 bytes aleatorios por entrada
 * - Clave derivada: PBKDF2-SHA256, 100,000 iteraciones
 * - Cifrado: AES-256-GCM con autenticación integrada
 *
 * @author KeyGuard Team
 */
@Service
public class BackupServiceImpl implements BackupService {

    // Constantes de cifrado
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits)
    private static final int SALT_LENGTH = 16; // bytes (128 bits)
    private static final int KEY_LENGTH = 256; // bits
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final String BACKUP_VERSION = "1.1"; // Nueva versión
    private static final String APP_VERSION = "1.0.0";

    private final PasswordEntryService passwordEntryService;
    private final CategoryService categoryService;
    private final Gson gson;

    public BackupServiceImpl(PasswordEntryService passwordEntryService,
                             CategoryService categoryService) {
        this.passwordEntryService = passwordEntryService;
        this.categoryService = categoryService;

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    @Override
    public void exportPasswords(String backupPassword, File outputFile) throws BackupException {
        // Validar entrada
        if (backupPassword == null || backupPassword.length() < 8) {
            throw new BackupException("La contraseña de backup debe tener al menos 8 caracteres");
        }

        if (outputFile == null) {
            throw new BackupException("Debe especificar un archivo de salida");
        }

        try {
            // PASO 1: Obtener todas las contraseñas del usuario actual
            List<PasswordEntryDTO> allPasswords = passwordEntryService.findAll();

            if (allPasswords.isEmpty()) {
                throw new BackupException("No hay contraseñas para exportar");
            }

            // PASO 2: Generar salt GLOBAL para todo el backup
            byte[] globalSalt = generateRandomBytes(SALT_LENGTH);

            // PASO 3: Derivar clave UNA SOLA VEZ con el salt global
            SecretKey globalKey = deriveKey(backupPassword, globalSalt);

            // PASO 4: Cifrar cada entrada con la clave global + IV único
            List<BackupDTO.BackupEntryDTO> backupEntries = new ArrayList<>();
            for (PasswordEntryDTO entry : allPasswords) {
                // Obtener nombre de categoría
                String categoryName = null;
                if (entry.getCategoryId() != null) {
                    try {
                        categoryName = categoryService.findAll().stream()
                                .filter(c -> c.getId().equals(entry.getCategoryId()))
                                .map(CategoryDTO::getName)
                                .findFirst()
                                .orElse(null);
                    } catch (Exception e) {
                        categoryName = null;
                    }
                }

                // Generar UUID único para esta entrada
                String entryUuid = UUID.randomUUID().toString();

                // Generar IV único para esta entrada
                byte[] entryIv = generateRandomBytes(GCM_IV_LENGTH);

                // Cifrar contraseña con clave global + IV único
                byte[] encryptedPassword = encrypt(
                        entry.getPassword().getBytes(StandardCharsets.UTF_8),
                        globalKey,
                        entryIv
                );

                // Crear entrada de backup
                BackupDTO.BackupEntryDTO backupEntry = BackupDTO.BackupEntryDTO.builder()
                        .id(entryUuid)
                        .title(entry.getTitle())
                        .username(entry.getUsername())
                        .email(entry.getEmail())
                        .url(entry.getUrl())
                        .notes(entry.getNotes())
                        .categoryName(categoryName)
                        .customFields(entry.getCustomFields())
                        .encryptedPassword(Base64.getEncoder().encodeToString(encryptedPassword))
                        .iv(Base64.getEncoder().encodeToString(entryIv))
                        .build();

                backupEntries.add(backupEntry);
            }

            // PASO 5: Crear metadata de cifrado
            BackupDTO.CryptoMetadata crypto = BackupDTO.CryptoMetadata.builder()
                    .kdf("PBKDF2-SHA256")
                    .iterations(PBKDF2_ITERATIONS)
                    .salt(Base64.getEncoder().encodeToString(globalSalt))
                    .cipher("AES-256-GCM")
                    .build();

            // PASO 6: Crear DTO de backup completo
            BackupDTO backup = BackupDTO.builder()
                    .version(BACKUP_VERSION)
                    .exportDate(LocalDateTime.now())
                    .entryCount(backupEntries.size())
                    .appVersion(APP_VERSION)
                    .crypto(crypto)
                    .entries(backupEntries)
                    .build();

            // PASO 7: Guardar en archivo JSON
            try (FileWriter writer = new FileWriter(outputFile)) {
                gson.toJson(backup, writer);
            }

        } catch (Exception e) {
            throw new BackupException("Error al exportar contraseñas: " + e.getMessage(), e);
        }
    }

    @Override
    public ImportResult importPasswords(String backupPassword, File inputFile, boolean replaceExisting) throws BackupException {
        // Validar entrada
        if (backupPassword == null || backupPassword.isEmpty()) {
            throw new BackupException("Debe proporcionar la contraseña de backup");
        }

        if (inputFile == null || !inputFile.exists()) {
            throw new BackupException("El archivo de backup no existe");
        }

        List<String> errors = new ArrayList<>();
        int totalEntries = 0;
        int importedEntries = 0;
        int skippedEntries = 0;

        try {
            // Leer el archivo JSON
            BackupDTO backup;
            try (FileReader reader = new FileReader(inputFile)) {
                backup = gson.fromJson(reader, BackupDTO.class);
            }

            // Validar estructura básica
            if (backup == null) {
                throw new BackupException("El archivo de backup está vacío o es inválido");
            }

            // Validar versión (soportar v1.0 y v1.1)
            if (backup.getVersion() == null) {
                throw new BackupException("El archivo no tiene versión especificada");
            }

            if (!backup.getVersion().equals("1.0") && !backup.getVersion().equals("1.1")) {
                throw new BackupException("Versión de backup no compatible: " + backup.getVersion());
            }

            // Validar que tenga entradas
            if (backup.getEntries() == null || backup.getEntries().isEmpty()) {
                throw new BackupException("El backup no contiene entradas");
            }

            totalEntries = backup.getEntries().size();

            // Si replaceExisting, eliminar todas las contraseñas actuales
            if (replaceExisting) {
                List<PasswordEntryDTO> currentPasswords = passwordEntryService.findAll();
                for (PasswordEntryDTO entry : currentPasswords) {
                    try {
                        passwordEntryService.delete(entry.getId());
                    } catch (Exception e) {
                        errors.add("Error al eliminar entrada existente: " + entry.getTitle());
                    }
                }
            }

            // Obtener títulos existentes (para detectar duplicados)
            Set<String> existingTitles = new HashSet<>();
            if (!replaceExisting) {
                List<PasswordEntryDTO> currentPasswords = passwordEntryService.findAll();
                for (PasswordEntryDTO entry : currentPasswords) {
                    existingTitles.add(entry.getTitle().toLowerCase());
                }
            }

            // Derivar clave según la versión del backup
            SecretKey key;
            boolean isV1_1 = backup.getVersion().equals("1.1");

            if (isV1_1) {
                // v1.1: Salt global en crypto.salt
                if (backup.getCrypto() == null || backup.getCrypto().getSalt() == null) {
                    throw new BackupException("Backup v1.1 inválido: falta metadata de cifrado");
                }
                byte[] globalSalt = Base64.getDecoder().decode(backup.getCrypto().getSalt());
                key = deriveKey(backupPassword, globalSalt);
            } else {
                // v1.0: Salt por entrada (compatibilidad hacia atrás)
                key = null; // Se derivará por entrada
            }

            // Importar cada entrada
            for (BackupDTO.BackupEntryDTO backupEntry : backup.getEntries()) {
                try {
                    // Verificar duplicado
                    if (!replaceExisting && existingTitles.contains(backupEntry.getTitle().toLowerCase())) {
                        skippedEntries++;
                        continue;
                    }

                    // Descifrar contraseña
                    String decryptedPassword;
                    try {
                        if (isV1_1) {
                            // v1.1: Usar clave global + IV de la entrada
                            byte[] entryIv = Base64.getDecoder().decode(backupEntry.getIv());
                            byte[] encryptedPassword = Base64.getDecoder().decode(backupEntry.getEncryptedPassword());
                            byte[] decryptedBytes = decrypt(encryptedPassword, key, entryIv);
                            decryptedPassword = new String(decryptedBytes, StandardCharsets.UTF_8);
                        } else {
                            // v1.0: Salt por entrada (compatibilidad)
                            // Intentar obtener salt de la entrada (si existe en formato antiguo)
                            // Si no existe, lanzar excepción
                            throw new BackupException("Formato v1.0 detectado pero no implementado. Por favor, re-exporte con la versión actual.");
                        }
                    } catch (Exception e) {
                        throw new BackupException("Contraseña de backup incorrecta o datos corruptos");
                    }

                    // Obtener o crear categoría
                    Long categoryId = null;
                    if (backupEntry.getCategoryName() != null && !backupEntry.getCategoryName().isEmpty()) {
                        categoryId = getOrCreateCategory(backupEntry.getCategoryName());
                    }

                    // Crear la entrada
                    PasswordEntryDTO newEntry = PasswordEntryDTO.builder()
                            .title(backupEntry.getTitle())
                            .username(backupEntry.getUsername())
                            .email(backupEntry.getEmail())
                            .password(decryptedPassword)
                            .url(backupEntry.getUrl())
                            .notes(backupEntry.getNotes())
                            .categoryId(categoryId)
                            .customFields(backupEntry.getCustomFields())
                            .build();

                    passwordEntryService.create(newEntry);
                    importedEntries++;

                } catch (Exception e) {
                    errors.add("Error al importar '" + backupEntry.getTitle() + "': " + e.getMessage());
                }
            }

            return new ImportResult(totalEntries, importedEntries, skippedEntries, errors);

        } catch (BackupException e) {
            throw e;
        } catch (JsonSyntaxException e) {
            throw new BackupException("Formato de archivo inválido", e);
        } catch (Exception e) {
            throw new BackupException("Error al importar contraseñas: " + e.getMessage(), e);
        }
    }

    @Override
    public BackupInfo validateBackup(String backupPassword, File inputFile) throws BackupException {
        if (backupPassword == null || backupPassword.isEmpty()) {
            throw new BackupException("Debe proporcionar la contraseña de backup");
        }

        if (inputFile == null || !inputFile.exists()) {
            throw new BackupException("El archivo de backup no existe");
        }

        try {
            // Leer el archivo JSON
            BackupDTO backup;
            try (FileReader reader = new FileReader(inputFile)) {
                backup = gson.fromJson(reader, BackupDTO.class);
            }

            if (backup == null) {
                throw new BackupException("El archivo de backup está vacío o es inválido");
            }

            // Validar que tenga entradas
            if (backup.getEntries() == null || backup.getEntries().isEmpty()) {
                throw new BackupException("El backup no contiene entradas");
            }

            // Validar contraseña intentando descifrar la primera entrada
            BackupDTO.BackupEntryDTO firstEntry = backup.getEntries().get(0);
            boolean isV1_1 = "1.1".equals(backup.getVersion());

            try {
                if (isV1_1) {
                    // v1.1: Salt global
                    byte[] globalSalt = Base64.getDecoder().decode(backup.getCrypto().getSalt());
                    byte[] entryIv = Base64.getDecoder().decode(firstEntry.getIv());
                    byte[] encryptedPassword = Base64.getDecoder().decode(firstEntry.getEncryptedPassword());

                    SecretKey key = deriveKey(backupPassword, globalSalt);
                    decrypt(encryptedPassword, key, entryIv);
                } else {
                    // v1.0: No soportado en validación
                    throw new BackupException("Formato v1.0 no soportado. Re-exporte con versión actual.");
                }
            } catch (Exception e) {
                throw new BackupException("Contraseña de backup incorrecta");
            }

            // Retornar información del backup
            return new BackupInfo(
                    backup.getVersion(),
                    backup.getExportDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    backup.getEntryCount(),
                    backup.getAppVersion()
            );

        } catch (BackupException e) {
            throw e;
        } catch (Exception e) {
            throw new BackupException("Error al validar backup: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el ID de una categoría por nombre, o la crea si no existe.
     */
    private Long getOrCreateCategory(String categoryName) {
        List<CategoryDTO> categories = categoryService.findAll();
        for (CategoryDTO category : categories) {
            if (category.getName().equalsIgnoreCase(categoryName)) {
                return category.getId();
            }
        }

        CategoryDTO created = categoryService.create(categoryName, "📁");
        return created.getId();
    }

    /**
     * Genera bytes aleatorios criptográficamente seguros.
     */
    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * Deriva una clave de cifrado desde una contraseña usando PBKDF2.
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Cifra datos usando AES-256-GCM.
     */
    private byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(data);
    }

    /**
     * Descifra datos usando AES-256-GCM.
     */
    private byte[] decrypt(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Adaptador para serializar/deserializar LocalDateTime con Gson.
     */
    private static class LocalDateTimeAdapter implements com.google.gson.JsonSerializer<LocalDateTime>,
            com.google.gson.JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public com.google.gson.JsonElement serialize(LocalDateTime dateTime, java.lang.reflect.Type type,
                                                      com.google.gson.JsonSerializationContext context) {
            return new com.google.gson.JsonPrimitive(dateTime.format(FORMATTER));
        }

        @Override
        public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type,
                                         com.google.gson.JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        }
    }
}
