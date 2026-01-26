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
 *
 * <h2>Decisiones de Implementación</h2>
 *
 * <h3>1. ¿Por qué Gson y no Jackson?</h3>
 * - Gson es más simple para nuestro caso de uso
 * - Mejor manejo de LocalDateTime con adaptadores
 * - Menos configuración necesaria
 * - Más legible para archivos JSON que el usuario podría ver
 *
 * <h3>2. ¿Por qué PBKDF2 con 100,000 iteraciones?</h3>
 * - Mismo estándar que usamos para la contraseña maestra (consistencia)
 * - Protege contra ataques de fuerza bruta
 * - OWASP recomienda mínimo 100,000 iteraciones en 2023
 * - 100,000 iteraciones = ~100ms en hardware moderno (aceptable para UX)
 *
 * <h3>3. ¿Por qué AES-256-GCM?</h3>
 * - GCM proporciona autenticación (detecta manipulación del archivo)
 * - Más seguro que CBC (no necesita padding, no vulnerable a padding oracle)
 * - Estándar de la industria (usado por TLS 1.3, Signal, WhatsApp)
 * - Hardware acceleration en CPUs modernos
 *
 * <h3>4. ¿Por qué no incluir IDs en el backup?</h3>
 * - Los IDs son específicos de cada base de datos
 * - Al importar a otra instalación, los IDs pueden estar ocupados
 * - Mejor usar títulos + categorías para identificar duplicados
 * - Más portable entre diferentes versiones de KeyGuard
 *
 * <h3>5. Formato JSON vs binario</h3>
 * - JSON es legible por humanos (el usuario puede ver la estructura cifrada)
 * - Facilita debugging
 * - Portable entre plataformas
 * - Tamaño similar al binario después de cifrar + base64
 *
 * @author KeyGuard Team
 */
@Service
public class BackupServiceImpl implements BackupService {

    // Constantes de cifrado (idénticas a las de EncryptionUtil para consistencia)
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits)
    private static final int SALT_LENGTH = 16; // bytes (128 bits)
    private static final int KEY_LENGTH = 256; // bits
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final String BACKUP_VERSION = "1.0";
    private static final String APP_VERSION = "1.0.0";

    private final PasswordEntryService passwordEntryService;
    private final CategoryService categoryService;
    private final Gson gson;

    public BackupServiceImpl(PasswordEntryService passwordEntryService,
                             CategoryService categoryService) {
        this.passwordEntryService = passwordEntryService;
        this.categoryService = categoryService;

        // Configurar Gson con formato legible y soporte para LocalDateTime
        this.gson = new GsonBuilder()
                .setPrettyPrinting() // JSON formateado (más legible)
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

            // PASO 2: Convertir a formato de backup (sin IDs, con nombres de categorías)
            // CADA entrada cifra su contraseña individualmente con su propio salt/IV
            List<BackupDTO.BackupEntryDTO> backupEntries = new ArrayList<>();
            for (PasswordEntryDTO entry : allPasswords) {
                // Obtener nombre de categoría si existe
                String categoryName = null;
                if (entry.getCategoryId() != null) {
                    try {
                        // findAll retorna List<CategoryDTO>, más simple que convertir Optional<Category>
                        categoryName = categoryService.findAll().stream()
                                .filter(c -> c.getId().equals(entry.getCategoryId()))
                                .map(CategoryDTO::getName)
                                .findFirst()
                                .orElse(null);
                    } catch (Exception e) {
                        // Si no se encuentra la categoría, usar null (se asignará "Otros" al importar)
                        categoryName = null;
                    }
                }

                // PASO 3: Cifrar SOLO la contraseña de esta entrada
                byte[] entrySalt = generateRandomBytes(SALT_LENGTH);
                byte[] entryIv = generateRandomBytes(GCM_IV_LENGTH);
                SecretKey entryKey = deriveKey(backupPassword, entrySalt);
                byte[] encryptedPassword = encrypt(entry.getPassword().getBytes(StandardCharsets.UTF_8), entryKey, entryIv);

                BackupDTO.BackupEntryDTO backupEntry = BackupDTO.BackupEntryDTO.builder()
                        .title(entry.getTitle())
                        .username(entry.getUsername())
                        .email(entry.getEmail())
                        .url(entry.getUrl())
                        .notes(entry.getNotes())
                        .categoryName(categoryName)
                        .customFields(entry.getCustomFields())
                        // Campos cifrados (cada entrada tiene su propio salt/IV)
                        .encryptedPassword(Base64.getEncoder().encodeToString(encryptedPassword))
                        .salt(Base64.getEncoder().encodeToString(entrySalt))
                        .iv(Base64.getEncoder().encodeToString(entryIv))
                        .build();

                backupEntries.add(backupEntry);
            }

            // PASO 4: Crear el DTO de backup con metadata y entradas
            BackupDTO backup = BackupDTO.builder()
                    .version(BACKUP_VERSION)
                    .exportDate(LocalDateTime.now())
                    .entryCount(backupEntries.size())
                    .appVersion(APP_VERSION)
                    .entries(backupEntries)  // Las entradas ahora están visibles (excepto password)
                    .build();

            // PASO 5: Guardar en archivo JSON
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

            if (backup.getVersion() == null || !backup.getVersion().equals(BACKUP_VERSION)) {
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

            // Importar cada entrada
            for (BackupDTO.BackupEntryDTO backupEntry : backup.getEntries()) {
                try {
                    // Verificar duplicado
                    if (!replaceExisting && existingTitles.contains(backupEntry.getTitle().toLowerCase())) {
                        skippedEntries++;
                        continue;
                    }

                    // Descifrar la contraseña de esta entrada específica
                    String decryptedPassword;
                    try {
                        byte[] entrySalt = Base64.getDecoder().decode(backupEntry.getSalt());
                        byte[] entryIv = Base64.getDecoder().decode(backupEntry.getIv());
                        byte[] encryptedPassword = Base64.getDecoder().decode(backupEntry.getEncryptedPassword());

                        SecretKey entryKey = deriveKey(backupPassword, entrySalt);
                        byte[] decryptedBytes = decrypt(encryptedPassword, entryKey, entryIv);
                        decryptedPassword = new String(decryptedBytes, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        throw new BackupException("Contraseña de backup incorrecta");
                    }

                    // Obtener o crear categoría
                    Long categoryId = null;
                    if (backupEntry.getCategoryName() != null && !backupEntry.getCategoryName().isEmpty()) {
                        categoryId = getOrCreateCategory(backupEntry.getCategoryName());
                    }

                    // Crear la entrada con la contraseña descifrada
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
            try {
                byte[] entrySalt = Base64.getDecoder().decode(firstEntry.getSalt());
                byte[] entryIv = Base64.getDecoder().decode(firstEntry.getIv());
                byte[] encryptedPassword = Base64.getDecoder().decode(firstEntry.getEncryptedPassword());

                SecretKey entryKey = deriveKey(backupPassword, entrySalt);
                decrypt(encryptedPassword, entryKey, entryIv);
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
     *
     * ¿Por qué crear automáticamente?
     * - Mejor UX: el usuario no tiene que crear categorías manualmente
     * - Evita errores si el backup viene de otra instalación
     * - Mantiene la organización del backup original
     *
     * @param categoryName Nombre de la categoría
     * @return ID de la categoría
     */
    private Long getOrCreateCategory(String categoryName) {
        // Buscar categoría existente
        List<CategoryDTO> categories = categoryService.findAll();
        for (CategoryDTO category : categories) {
            if (category.getName().equalsIgnoreCase(categoryName)) {
                return category.getId();
            }
        }

        // Crear nueva categoría (sin icono específico, se asignará uno por defecto)
        CategoryDTO created = categoryService.create(categoryName, "📁");
        return created.getId();
    }

    /**
     * Genera bytes aleatorios criptográficamente seguros.
     *
     * ¿Por qué SecureRandom?
     * - Random normal NO es criptográficamente seguro
     * - SecureRandom usa entropía del sistema operativo
     * - Impredecible incluso conociendo salidas anteriores
     *
     * @param length Número de bytes a generar
     * @return Array de bytes aleatorios
     */
    private byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * Deriva una clave de cifrado desde una contraseña usando PBKDF2.
     *
     * ¿Por qué PBKDF2?
     * - Estándar NIST para derivación de claves
     * - Las iteraciones hacen lenta la fuerza bruta
     * - Compatible con Java sin librerías externas
     *
     * @param password Contraseña del usuario
     * @param salt Salt aleatorio
     * @return Clave AES-256
     * @throws Exception Si hay error en la derivación
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
     *
     * ¿Por qué GCM?
     * - Autenticación integrada (detecta manipulación)
     * - No necesita padding (más simple)
     * - Paralelizable (más rápido en hardware moderno)
     *
     * @param data Datos a cifrar
     * @param key Clave de cifrado
     * @param iv Vector de inicialización
     * @return Datos cifrados
     * @throws Exception Si hay error en el cifrado
     */
    private byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(data);
    }

    /**
     * Descifra datos usando AES-256-GCM.
     *
     * @param encryptedData Datos cifrados
     * @param key Clave de descifrado
     * @param iv Vector de inicialización
     * @return Datos descifrados
     * @throws Exception Si hay error en el descifrado o la contraseña es incorrecta
     */
    private byte[] decrypt(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Adaptador para serializar/deserializar LocalDateTime con Gson.
     *
     * ¿Por qué necesitamos esto?
     * - Gson no soporta LocalDateTime por defecto
     * - ISO-8601 es el estándar internacional para fechas
     * - Permite parsear fechas entre diferentes locales
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
