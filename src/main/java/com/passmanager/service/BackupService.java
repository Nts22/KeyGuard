package com.passmanager.service;

import java.io.File;
import java.util.List;

/**
 * Servicio para exportar e importar contraseñas con cifrado.
 *
 * <h2>¿Por qué necesitamos backups?</h2>
 * - Protección contra pérdida de datos (disco duro dañado, borrado accidental)
 * - Migración a otro dispositivo
 * - Compartir contraseñas con familia (usando contraseña compartida)
 * - Cumplimiento de políticas de backup corporativas
 *
 * <h2>Seguridad del Backup</h2>
 *
 * <h3>Cifrado AES-256-GCM</h3>
 * - El backup SIEMPRE está cifrado
 * - Usa la misma tecnología de grado militar que las contraseñas en la BD
 * - Imposible leer el contenido sin la contraseña de backup
 *
 * <h3>Contraseña de Backup</h3>
 * - Puede ser la misma que la contraseña maestra
 * - O puede ser diferente (más seguridad)
 * - Se deriva con PBKDF2-SHA256 (100,000 iteraciones)
 * - Nunca se almacena, solo se usa para cifrar/descifrar
 *
 * <h3>Salt e IV únicos</h3>
 * - Cada backup tiene salt e IV aleatorios
 * - Incluso con la misma contraseña, el cifrado es diferente
 * - Protege contra ataques de diccionario precomputados
 *
 * <h2>Formato de archivo</h2>
 * ```json
 * {
 *   "version": "1.0",
 *   "exportDate": "2024-01-26T10:30:00",
 *   "entryCount": 25,
 *   "appVersion": "1.0.0",
 *   "salt": "base64_salt",
 *   "iv": "base64_iv",
 *   "encryptedData": "base64_encrypted_json"
 * }
 * ```
 *
 * @author KeyGuard Team
 */
public interface BackupService {

    /**
     * Exporta todas las contraseñas del usuario actual a un archivo JSON cifrado.
     *
     * <h3>Proceso</h3>
     * 1. Obtiene todas las contraseñas del usuario actual
     * 2. Convierte a formato BackupEntryDTO (sin IDs, con nombres de categoría)
     * 3. Serializa a JSON
     * 4. Cifra el JSON con AES-256-GCM usando la contraseña proporcionada
     * 5. Crea BackupDTO con metadata + datos cifrados
     * 6. Guarda en el archivo especificado
     *
     * <h3>Notas importantes</h3>
     * - Las contraseñas se descifran primero (de la BD) y luego se cifran con la clave de backup
     * - NO se exportan los IDs internos (para evitar conflictos al importar)
     * - Se exportan nombres de categorías, no IDs
     * - Se incluyen campos personalizados
     *
     * @param backupPassword Contraseña para cifrar el backup (mínimo 8 caracteres)
     * @param outputFile Archivo donde guardar el backup (.json)
     * @throws BackupException Si hay error al crear el backup
     */
    void exportPasswords(String backupPassword, File outputFile) throws BackupException;

    /**
     * Importa contraseñas desde un archivo JSON cifrado.
     *
     * <h3>Proceso</h3>
     * 1. Lee el archivo JSON
     * 2. Descifra los datos usando la contraseña proporcionada
     * 3. Valida el formato y los datos
     * 4. Para cada entrada:
     *    - Si replaceExisting=true: elimina todas las contraseñas actuales primero
     *    - Si replaceExisting=false: solo agrega las que no existen (por título)
     * 5. Crea las categorías si no existen
     * 6. Importa todas las entradas
     *
     * <h3>Manejo de duplicados</h3>
     * - Si replaceExisting=false, compara por título
     * - Si existe una entrada con el mismo título, se omite
     * - Se retorna un resumen: cuántas se importaron, cuántas se omitieron
     *
     * <h3>Categorías</h3>
     * - Si una categoría no existe, se crea automáticamente
     * - Si el nombre de categoría es null, usa "Otros"
     *
     * @param backupPassword Contraseña para descifrar el backup
     * @param inputFile Archivo de backup a importar (.json)
     * @param replaceExisting Si true, elimina todas las contraseñas actuales antes de importar
     * @return Resultado de la importación con estadísticas
     * @throws BackupException Si hay error al importar (contraseña incorrecta, formato inválido, etc.)
     */
    ImportResult importPasswords(String backupPassword, File inputFile, boolean replaceExisting) throws BackupException;

    /**
     * Valida un archivo de backup sin importarlo.
     * Útil para verificar la contraseña antes de importar.
     *
     * @param backupPassword Contraseña para descifrar el backup
     * @param inputFile Archivo de backup a validar
     * @return Información del backup (versión, fecha, número de entradas)
     * @throws BackupException Si la contraseña es incorrecta o el formato es inválido
     */
    BackupInfo validateBackup(String backupPassword, File inputFile) throws BackupException;

    /**
     * Información de un backup (sin importarlo).
     */
    class BackupInfo {
        private final String version;
        private final String exportDate;
        private final int entryCount;
        private final String appVersion;

        public BackupInfo(String version, String exportDate, int entryCount, String appVersion) {
            this.version = version;
            this.exportDate = exportDate;
            this.entryCount = entryCount;
            this.appVersion = appVersion;
        }

        public String getVersion() { return version; }
        public String getExportDate() { return exportDate; }
        public int getEntryCount() { return entryCount; }
        public String getAppVersion() { return appVersion; }
    }

    /**
     * Resultado de una operación de importación.
     */
    class ImportResult {
        private final int totalEntries;
        private final int importedEntries;
        private final int skippedEntries;
        private final List<String> errors;

        public ImportResult(int totalEntries, int importedEntries, int skippedEntries, List<String> errors) {
            this.totalEntries = totalEntries;
            this.importedEntries = importedEntries;
            this.skippedEntries = skippedEntries;
            this.errors = errors;
        }

        public int getTotalEntries() { return totalEntries; }
        public int getImportedEntries() { return importedEntries; }
        public int getSkippedEntries() { return skippedEntries; }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    /**
     * Excepción lanzada cuando hay error en operaciones de backup.
     */
    class BackupException extends Exception {
        public BackupException(String message) {
            super(message);
        }

        public BackupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
