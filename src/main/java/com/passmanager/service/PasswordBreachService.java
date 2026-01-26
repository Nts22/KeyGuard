package com.passmanager.service;

/**
 * Servicio para verificar si las contraseñas han sido comprometidas en brechas de seguridad.
 *
 * <h2>¿Por qué es importante?</h2>
 * Millones de contraseñas han sido filtradas en brechas de datos (LinkedIn, Yahoo, Facebook, etc.).
 * Los atacantes usan estas contraseñas en ataques de "credential stuffing" para acceder a otras cuentas.
 *
 * <h2>¿Cómo funciona?</h2>
 * Utilizamos la API gratuita de Have I Been Pwned (HIBP) creada por Troy Hunt.
 * Esta API contiene más de 12 mil millones de contraseñas comprometidas reales.
 *
 * <h2>¿Es seguro?</h2>
 * SÍ. Usamos el método de "k-anonymity":
 * 1. Nunca enviamos la contraseña completa al servidor
 * 2. Convertimos la contraseña a SHA-1 hash localmente
 * 3. Solo enviamos los primeros 5 caracteres del hash
 * 4. El servidor devuelve todos los hashes que empiezan con esos 5 caracteres
 * 5. Comparamos localmente para ver si hay coincidencia
 *
 * Ejemplo:
 * - Contraseña: "password123"
 * - SHA-1: "482C811DA5D5B4BC6D497FFA98491E38"
 * - Enviamos solo: "482C8"
 * - Recibimos ~500 hashes que empiezan con "482C8"
 * - Buscamos localmente si "11DA5D5B4BC6D497FFA98491E38" está en la lista
 *
 * <h2>Referencias</h2>
 * - API: https://haveibeenpwned.com/API/v3#PwnedPasswords
 * - Documentación k-anonymity: https://haveibeenpwned.com/API/v3#SearchingPwnedPasswordsByRange
 * - Paper académico: https://www.troyhunt.com/ive-just-launched-pwned-passwords-version-2/
 */
public interface PasswordBreachService {

    /**
     * Verifica si una contraseña ha sido comprometida en alguna brecha de seguridad.
     *
     * IMPORTANTE: Este método NO envía la contraseña al servidor. Solo envía los primeros
     * 5 caracteres del hash SHA-1 para proteger la privacidad.
     *
     * @param password La contraseña a verificar (texto plano)
     * @return BreachCheckResult con información sobre si fue encontrada y cuántas veces
     * @throws PasswordBreachCheckException si hay un error al comunicarse con la API
     */
    BreachCheckResult checkPassword(String password) throws PasswordBreachCheckException;

    /**
     * Resultado de la verificación de una contraseña contra la base de datos de brechas.
     */
    class BreachCheckResult {
        private final boolean breached;
        private final int occurrences;

        public BreachCheckResult(boolean breached, int occurrences) {
            this.breached = breached;
            this.occurrences = occurrences;
        }

        /**
         * @return true si la contraseña fue encontrada en brechas de seguridad
         */
        public boolean isBreached() {
            return breached;
        }

        /**
         * @return Número de veces que la contraseña aparece en brechas conocidas.
         *         Un número alto indica que es muy común y vulnerable.
         */
        public int getOccurrences() {
            return occurrences;
        }

        /**
         * @return Nivel de severidad basado en el número de ocurrencias
         */
        public SeverityLevel getSeverityLevel() {
            if (!breached) return SeverityLevel.SAFE;
            if (occurrences < 10) return SeverityLevel.LOW;
            if (occurrences < 100) return SeverityLevel.MEDIUM;
            if (occurrences < 1000) return SeverityLevel.HIGH;
            return SeverityLevel.CRITICAL;
        }
    }

    /**
     * Nivel de severidad para contraseñas comprometidas.
     */
    enum SeverityLevel {
        SAFE("Segura", "La contraseña no ha sido encontrada en brechas"),
        LOW("Riesgo Bajo", "Encontrada pocas veces (<10)"),
        MEDIUM("Riesgo Medio", "Encontrada varias veces (<100)"),
        HIGH("Riesgo Alto", "Encontrada muchas veces (<1000)"),
        CRITICAL("Riesgo Crítico", "Contraseña muy común (1000+)");

        private final String label;
        private final String description;

        SeverityLevel(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Excepción lanzada cuando hay un error al verificar contraseñas.
     */
    class PasswordBreachCheckException extends Exception {
        public PasswordBreachCheckException(String message, Throwable cause) {
            super(message, cause);
        }

        public PasswordBreachCheckException(String message) {
            super(message);
        }
    }
}
