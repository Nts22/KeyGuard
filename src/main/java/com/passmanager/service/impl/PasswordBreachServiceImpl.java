package com.passmanager.service.impl;

import com.passmanager.service.PasswordBreachService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

/**
 * Implementación del servicio de verificación de contraseñas filtradas usando Have I Been Pwned API.
 *
 * <h2>Decisiones de Implementación</h2>
 *
 * <h3>1. ¿Por qué usar SHA-1 si es inseguro?</h3>
 * SHA-1 NO es seguro para almacenar contraseñas (debemos usar bcrypt, Argon2, etc.).
 * PERO es perfectamente válido aquí porque:
 * - No lo usamos para autenticación
 * - Solo lo usamos para buscar en una base de datos pública
 * - HIBP API requiere SHA-1 específicamente
 * - El hash SHA-1 nunca se almacena, solo se calcula en memoria
 *
 * <h3>2. ¿Por qué HttpClient de Java 11+ en vez de otras librerías?</h3>
 * - Es parte del JDK, no necesita dependencias externas
 * - Soporte nativo para operaciones asíncronas
 * - API moderna y fácil de usar
 * - Mejor rendimiento que HttpURLConnection antiguo
 *
 * <h3>3. ¿Por qué k-anonymity?</h3>
 * Protege la privacidad del usuario:
 * - El servidor NUNCA ve la contraseña real
 * - El servidor NUNCA ve el hash completo
 * - Solo enviamos 5 caracteres del hash
 * - Hay ~16^5 = 1,048,576 posibles prefijos
 * - Cada prefijo devuelve ~500-800 hashes en promedio
 * - Imposible para HIBP saber qué contraseña específica estamos verificando
 *
 * <h3>4. Manejo de Errores</h3>
 * Si la API falla:
 * - No bloqueamos al usuario
 * - Mostramos advertencia pero permitimos continuar
 * - Log del error para debugging
 *
 * @author KeyGuard Team
 */
@Service
public class PasswordBreachServiceImpl implements PasswordBreachService {

    // API de Have I Been Pwned - Completamente GRATUITA
    // No requiere API key para búsqueda de contraseñas
    private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";

    // Timeout razonable: 10 segundos
    // Si la API no responde en 10s, mejor fallar rápido que bloquear al usuario
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // Cliente HTTP reutilizable (mejor rendimiento que crear uno nuevo cada vez)
    private final HttpClient httpClient;

    /**
     * Constructor que inicializa el HttpClient.
     *
     * ¿Por qué configurar el HttpClient así?
     * - followRedirects(NORMAL): Sigue redirecciones HTTP automáticamente
     * - connectTimeout: Evita que la app se cuelgue si HIBP está caído
     */
    public PasswordBreachServiceImpl() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    @Override
    public BreachCheckResult checkPassword(String password) throws PasswordBreachCheckException {
        // Validación: La contraseña no puede ser null o vacía
        if (password == null || password.isEmpty()) {
            throw new PasswordBreachCheckException("La contraseña no puede estar vacía");
        }

        try {
            // PASO 1: Calcular el hash SHA-1 de la contraseña
            String sha1Hash = calculateSHA1(password);

            // PASO 2: Dividir el hash en dos partes para k-anonymity
            // Ejemplo: "482C811DA5D5B4BC6D497FFA98491E38"
            //          prefix = "482C8"
            //          suffix = "11DA5D5B4BC6D497FFA98491E38"
            String prefix = sha1Hash.substring(0, 5);  // Primeros 5 caracteres
            String suffix = sha1Hash.substring(5);     // El resto (35 caracteres)

            // PASO 3: Hacer la petición HTTP a la API
            // Solo enviamos el prefix, NUNCA el hash completo
            String apiResponse = queryHIBPAPI(prefix);

            // PASO 4: Buscar el suffix en la respuesta
            // La API devuelve líneas como: "11DA5D5B4BC6D497FFA98491E38:12345"
            // donde 12345 es el número de veces que apareció en brechas
            int occurrences = parseAPIResponse(apiResponse, suffix);

            // PASO 5: Retornar el resultado
            boolean isBreached = occurrences > 0;
            return new BreachCheckResult(isBreached, occurrences);

        } catch (NoSuchAlgorithmException e) {
            // Esto nunca debería pasar (SHA-1 siempre está disponible en Java)
            throw new PasswordBreachCheckException("Error interno: SHA-1 no disponible", e);
        } catch (IOException | InterruptedException e) {
            // Error de red o timeout
            throw new PasswordBreachCheckException(
                    "No se pudo conectar con el servicio de verificación. " +
                    "Por favor, verifica tu conexión a internet.", e);
        }
    }

    /**
     * Calcula el hash SHA-1 de una contraseña.
     *
     * ¿Por qué en mayúsculas?
     * - La API de HIBP trabaja con hashes en mayúsculas
     * - Es case-insensitive pero por convención se usa uppercase
     *
     * @param password Contraseña en texto plano
     * @return Hash SHA-1 en formato hexadecimal mayúsculas (40 caracteres)
     * @throws NoSuchAlgorithmException Si SHA-1 no está disponible (nunca debería pasar)
     */
    private String calculateSHA1(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        // Convertir la contraseña a bytes UTF-8 y calcular el hash
        byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        // Convertir los bytes a hexadecimal
        // Ejemplo: [0x48, 0x2C, 0x81] -> "482C81"
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            // %02X = formato hexadecimal de 2 dígitos en mayúsculas
            // Ejemplo: byte 15 (0x0F) -> "0F" (no solo "F")
            String hex = String.format("%02X", b);
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Hace una petición HTTP GET a la API de Have I Been Pwned.
     *
     * ¿Por qué GET y no POST?
     * - La API de HIBP solo acepta GET
     * - Es seguro porque solo enviamos 5 caracteres del hash
     * - GET permite cacheo en CDN para mejor rendimiento
     *
     * ¿Por qué el User-Agent?
     * - Es cortesía identificar nuestra aplicación
     * - Ayuda a HIBP con estadísticas
     * - Recomendación oficial de la documentación de HIBP
     *
     * @param hashPrefix Primeros 5 caracteres del hash SHA-1
     * @return Respuesta de la API (texto plano con lista de hashes)
     * @throws IOException Si hay error de red
     * @throws InterruptedException Si la petición es interrumpida
     */
    private String queryHIBPAPI(String hashPrefix) throws IOException, InterruptedException {
        // Construir la URL: https://api.pwnedpasswords.com/range/482C8
        String url = HIBP_API_URL + hashPrefix;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                // User-Agent recomendado por HIBP para identificar la app
                .header("User-Agent", "KeyGuard-PasswordManager/1.0")
                // Add-Padding: añade ruido a la respuesta para mayor privacidad (opcional)
                // .header("Add-Padding", "true")
                .GET()
                .build();

        // Enviar la petición y obtener la respuesta
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Verificar que la respuesta fue exitosa (código 200)
        if (response.statusCode() != 200) {
            throw new IOException("API devolvió código " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Parsea la respuesta de la API para buscar el hash suffix.
     *
     * Formato de respuesta de HIBP:
     * ```
     * 003D68EB55068C33ACE09247EE4C639306B:3
     * 00D4F6E8FA6EECAD2A3AA415EEC418D38EC:2
     * 011053FD0102E94D6AE2F8B83D76FAF94F6:1
     * ...
     * ```
     *
     * Cada línea contiene:
     * - Suffix del hash (35 caracteres)
     * - Dos puntos ':'
     * - Número de ocurrencias en brechas
     *
     * ¿Por qué ignorar mayúsculas/minúsculas?
     * - Aunque HIBP devuelve uppercase, mejor ser defensivo
     * - No cuesta nada y previene bugs
     *
     * @param apiResponse Respuesta completa de la API
     * @param targetSuffix Suffix que estamos buscando (35 caracteres)
     * @return Número de ocurrencias, o 0 si no se encontró
     */
    private int parseAPIResponse(String apiResponse, String targetSuffix) {
        // Dividir la respuesta en líneas
        String[] lines = apiResponse.split("\n");

        // Buscar nuestro suffix en cada línea
        for (String line : lines) {
            // Limpiar espacios en blanco
            line = line.trim();

            // Ignorar líneas vacías
            if (line.isEmpty()) {
                continue;
            }

            // Dividir en suffix:count
            String[] parts = line.split(":");
            if (parts.length != 2) {
                // Línea malformada, ignorar
                continue;
            }

            String responseSuffix = parts[0].trim();
            String countString = parts[1].trim();

            // Comparar ignorando mayúsculas/minúsculas
            if (responseSuffix.equalsIgnoreCase(targetSuffix)) {
                try {
                    return Integer.parseInt(countString);
                } catch (NumberFormatException e) {
                    // El count no es un número válido, asumir 0
                    return 0;
                }
            }
        }

        // No se encontró el suffix en la respuesta = contraseña segura
        return 0;
    }
}
