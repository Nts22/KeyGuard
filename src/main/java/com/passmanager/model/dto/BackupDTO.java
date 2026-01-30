package com.passmanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para el formato de backup de KeyGuard versión 1.1.
 *
 * <h2>Nuevo formato (v1.1)</h2>
 *
 * <h3>Estructura del backup</h3>
 * ```json
 * {
 *   "version": "1.1",
 *   "exportDate": "2024-01-30T10:30:00",
 *   "entryCount": 15,
 *   "appVersion": "1.0.0",
 *   "crypto": {
 *     "kdf": "PBKDF2-SHA256",
 *     "iterations": 100000,
 *     "salt": "BASE64_SALT",
 *     "cipher": "AES-256-GCM"
 *   },
 *   "entries": [
 *     {
 *       "id": "uuid-1234-5678",
 *       "title": "Facebook",
 *       "username": "user@email.com",
 *       "email": "user@email.com",
 *       "url": "https://facebook.com",
 *       "notes": "Mi cuenta",
 *       "categoryName": "Redes Sociales",
 *       "customFields": [],
 *       "encryptedPassword": "BASE64_ENCRYPTED",
 *       "iv": "BASE64_IV"
 *     }
 *   ]
 * }
 * ```
 *
 * <h3>Cambios respecto a v1.0</h3>
 * - **crypto**: Metadata de cifrado global (antes no existía)
 * - **salt**: Ahora global para todo el backup (antes era por entrada)
 * - **id**: UUID único para cada entrada (antes no existía)
 * - **iv**: Sigue siendo único por entrada
 *
 * <h3>¿Por qué salt global?</h3>
 * - Una sola derivación de clave (más rápido)
 * - Formato más común en la industria
 * - Sigue siendo seguro (el IV único garantiza unicidad de cifrado)
 *
 * <h3>¿Por qué UUID por entrada?</h3>
 * - Identificación única más allá del título
 * - Facilita tracking y debugging
 * - Compatible con futuros features (sync, merge, etc.)
 *
 * @author KeyGuard Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupDTO {

    /**
     * Versión del formato de backup.
     * v1.1: Nuevo formato con crypto global y UUIDs
     */
    private String version;

    /**
     * Fecha y hora de exportación del backup.
     */
    private LocalDateTime exportDate;

    /**
     * Número de entradas incluidas en el backup.
     */
    private int entryCount;

    /**
     * Versión de KeyGuard que generó el backup.
     */
    private String appVersion;

    /**
     * Metadata de cifrado (nuevo en v1.1).
     * Contiene información sobre el algoritmo y parámetros usados.
     */
    private CryptoMetadata crypto;

    /**
     * Lista de entradas del backup.
     * Todos los campos son visibles excepto la contraseña cifrada.
     */
    private List<BackupEntryDTO> entries;

    /**
     * Metadata de cifrado del backup.
     * Incluye algoritmos usados y salt global.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CryptoMetadata {
        /**
         * Algoritmo de derivación de clave.
         * Valor: "PBKDF2-SHA256"
         */
        private String kdf;

        /**
         * Número de iteraciones para PBKDF2.
         * Valor: 100000
         */
        private int iterations;

        /**
         * Salt global para todo el backup (Base64, 16 bytes).
         * Se usa junto con la contraseña de backup para derivar la clave AES.
         */
        private String salt;

        /**
         * Algoritmo de cifrado usado.
         * Valor: "AES-256-GCM"
         */
        private String cipher;
    }

    /**
     * DTO para una entrada individual en el backup.
     *
     * Campos VISIBLES: title, username, email, url, notes, categoryName, customFields
     * Campos CIFRADOS: encryptedPassword
     * Campos de SEGURIDAD: id, iv
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupEntryDTO {
        /**
         * Identificador único de la entrada (UUID).
         * Generado al exportar, permite identificar entradas más allá del título.
         */
        private String id;

        // Campos visibles (texto claro)
        private String title;
        private String username;
        private String email;
        private String url;
        private String notes;
        private String categoryName;
        private List<PasswordEntryDTO.CustomFieldDTO> customFields;

        // Campos cifrados (solo la contraseña)
        /**
         * Contraseña cifrada con AES-256-GCM (Base64).
         * Se descifra usando: crypto.salt + contraseña de backup + este IV.
         */
        private String encryptedPassword;

        /**
         * Vector de Inicialización único para esta entrada (Base64, 12 bytes).
         * Garantiza que incluso contraseñas idénticas se cifren de forma diferente.
         */
        private String iv;
    }
}
