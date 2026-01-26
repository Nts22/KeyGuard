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
 * <h3>Metadata visible</h3>
 * - **version**: Para compatibilidad futura (si cambiamos el formato)
 * - **exportDate**: Para saber cuándo se hizo el backup
 * - **entryCount**: Validación rápida
 * - **appVersion**: Para debugging si hay problemas de compatibilidad
 * - **entries**: Lista de entradas con campos visibles excepto la contraseña
 *
 * <h3>Campos visibles vs cifrados</h3>
 * - **Visibles**: title, username, email, url, notes, categoryName, customFields
 * - **Cifrado**: Solo el campo password (encryptedPassword por entrada)
 * - **salt/iv**: Únicos por entrada para máxima seguridad
 *
 * <h2>Proceso de cifrado</h2>
 * 1. Usuario proporciona contraseña de backup
 * 2. Para cada entrada:
 *    - Generamos salt aleatorio (16 bytes)
 *    - Derivamos clave AES-256 usando PBKDF2-SHA256 (100,000 iteraciones)
 *    - Generamos IV aleatorio (12 bytes)
 *    - Ciframos SOLO la contraseña con AES-256-GCM
 * 3. Guardamos: metadata + entries con campos visibles + contraseñas cifradas
 *
 * <h2>¿Por qué este enfoque híbrido?</h2>
 * - Puedes ver qué contraseñas tienes sin descifrar
 * - Las contraseñas reales están protegidas
 * - Fácil de auditar y buscar entradas específicas
 * - Cada contraseña tiene su propio salt/IV para máxima seguridad
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
     * Lista de entradas del backup.
     * Todos los campos son visibles excepto la contraseña que está cifrada.
     */
    private List<BackupEntryDTO> entries;

    /**
     * DTO para una entrada individual en el backup.
     * No incluye IDs ya que pueden cambiar al importar.
     *
     * Campos VISIBLES en el JSON:
     * - title, username, email, url, notes, categoryName, customFields
     *
     * Campos CIFRADOS:
     * - encryptedPassword: La contraseña cifrada con AES-256-GCM
     * - salt: Salt único para esta entrada (16 bytes, Base64)
     * - iv: IV único para esta entrada (12 bytes, Base64)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BackupEntryDTO {
        private String title;
        private String username;
        private String email;
        private String url;
        private String notes;
        private String categoryName;
        private List<PasswordEntryDTO.CustomFieldDTO> customFields;

        // Campos de seguridad (por entrada)
        private String encryptedPassword;  // Contraseña cifrada (Base64)
        private String salt;               // Salt para derivar clave (Base64, 16 bytes)
        private String iv;                 // IV para cifrado (Base64, 12 bytes)
    }
}
