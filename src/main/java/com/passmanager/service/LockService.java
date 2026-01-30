package com.passmanager.service;

/**
 * Servicio para gestionar el bloqueo automático de la aplicación.
 *
 * <h2>¿Bloqueo vs Logout?</h2>
 *
 * <h3>Logout (comportamiento anterior)</h3>
 * - Cierra sesión completamente
 * - Vuelve a la pantalla de login
 * - Pierde el contexto de la aplicación
 * - Requiere volver a autenticarse desde cero
 *
 * <h3>Bloqueo (nuevo comportamiento)</h3>
 * - Solo bloquea la pantalla
 * - Mantiene la sesión activa en segundo plano
 * - Desbloqueo rápido con contraseña
 * - Mejor UX para inactividad temporal
 *
 * <h2>¿Cuándo bloquear?</h2>
 *
 * <h3>1. Inactividad</h3>
 * - Después de X minutos sin interacción
 * - Configurable (por defecto: 2 minutos)
 * - Más corto que el auto-logout (3 minutos)
 *
 * <h3>2. Ventana minimizada</h3>
 * - Al minimizar la aplicación
 * - Configurable (puede desactivarse)
 * - Protege si el usuario se aleja de la PC
 *
 * <h2>Seguridad al bloquear</h2>
 * - Limpia contraseñas en memoria (caches, variables)
 * - Muestra pantalla de bloqueo opaca
 * - Detiene timers y tareas en segundo plano
 * - Mantiene solo lo necesario para desbloquear
 *
 * @author KeyGuard Team
 */
public interface LockService {

    /**
     * Inicia el monitoreo de inactividad y bloqueo.
     * Debe llamarse al iniciar la aplicación.
     *
     * @param onLockCallback Callback que se ejecutará cuando se deba bloquear
     */
    void startMonitoring(Runnable onLockCallback);

    /**
     * Detiene el monitoreo de bloqueo.
     * Debe llamarse al cerrar sesión o salir de la aplicación.
     */
    void stopMonitoring();

    /**
     * Resetea el timer de inactividad.
     * Debe llamarse en cada interacción del usuario.
     */
    void resetTimer();

    /**
     * Bloquea inmediatamente la aplicación.
     */
    void lockNow();

    /**
     * Verifica si el monitoreo está activo.
     *
     * @return true si está monitoreando, false si no
     */
    boolean isMonitoring();

    /**
     * Configura el tiempo de inactividad antes del bloqueo.
     *
     * @param minutes Minutos de inactividad (mínimo 1, máximo 30)
     */
    void setInactivityTimeout(int minutes);

    /**
     * Configura si debe bloquear al minimizar la ventana.
     *
     * @param enabled true para bloquear al minimizar, false para no hacerlo
     */
    void setLockOnMinimize(boolean enabled);

    /**
     * Obtiene el tiempo de inactividad configurado.
     *
     * @return Minutos de inactividad antes del bloqueo
     */
    int getInactivityTimeout();

    /**
     * Verifica si está configurado para bloquear al minimizar.
     *
     * @return true si bloquea al minimizar, false si no
     */
    boolean isLockOnMinimizeEnabled();
}
