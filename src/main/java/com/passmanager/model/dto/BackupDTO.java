package com.passmanager.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para el formato de backup de KeyGuard.
 *
 * <h2>¿Por qué este formato?</h2>
 *
 * <h3>Metadata</h3>
 * - **version**: Para compatibilidad futura (si cambiamos el formato)
 * - **exportDate**: Para saber cuándo se hizo el backup
 * - **entryCount**: Validación rápida sin descifrar todo
 * - **appVersion**: Para debugging si hay problemas de compatibilidad
 *
 * <h3>Datos cifrados</h3>
 * - **encryptedData**: JSON completo de todas las contraseñas, cifrado con AES-256-GCM
 * - **iv**: Vector de inicialización (12 bytes) para el cifrado
 * - **salt**: Salt (16 bytes) usado para derivar la clave de cifrado
 *
 * <h2>Proceso de cifrado</h2>
 * 1. Usuario proporciona contraseña de backup
 * 2. Generamos salt aleatorio (16 bytes)
 * 3. Derivamos clave AES-256 usando PBKDF2-SHA256 (100,000 iteraciones)
 * 4. Generamos IV aleatorio (12 bytes)
 * 5. Ciframos el JSON con AES-256-GCM
 * 6. Guardamos: metadata + salt + iv + datos cifrados
 *
 * <h2>¿Por qué cifrar el backup?</h2>
 * - Protege tus contraseñas si el archivo cae en manos incorrectas
 * - Permite almacenar backups en la nube (Dropbox, Google Drive, etc.)
 * - Compatible con el principio de zero-knowledge
 * - El usuario puede elegir una contraseña diferente a la maestra
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
     * Permite manejar cambios de formato en futuras versiones.
     */
    private String version;

    /**
     * Fecha y hora de exportación del backup.
     */
    private LocalDateTime exportDate;

    /**
     * Número de entradas incluidas en el backup.
     * Útil para validación sin necesidad de descifrar.
     */
    private int entryCount;

    /**
     * Versión de KeyGuard que generó el backup.
     * Útil para debugging y compatibilidad.
     */
    private String appVersion;

    /**
     * Salt usado para derivar la clave de cifrado (Base64).
     * 16 bytes (128 bits) de entropía aleatoria.
     */
    private String salt;

    /**
     * Vector de inicialización para el cifrado AES-GCM (Base64).
     * 12 bytes (96 bits) generados aleatoriamente por backup.
     */
    private String iv;

    /**
     * Datos cifrados que contienen todas las contraseñas (Base64).
     * El contenido descifrado es un JSON con:
     * {
     *   "entries": [
     *     {
     *       "title": "Facebook",
     *       "username": "user@example.com",
     *       "password": "encrypted_password",
     *       "email": "user@example.com",
     *       "url": "https://facebook.com",
     *       "notes": "Mi cuenta principal",
     *       "categoryName": "Redes Sociales",
     *       "customFields": [...]
     *     },
     *     ...
     *   ]
     * }
     */
    private String encryptedData;

    /**
     * DTO para una entrada individual en el backup.
     * No incluye IDs ya que pueden cambiar al importar.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupEntryDTO {
        private String title;
        private String username;
        private String email;
        private String password;
        private String url;
        private String notes;
        private String categoryName;
        private List<PasswordEntryDTO.CustomFieldDTO> customFields;
    }

    /**
     * DTO contenedor para los datos descifrados.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupDataDTO {
        private List<BackupEntryDTO> entries;
    }
}
