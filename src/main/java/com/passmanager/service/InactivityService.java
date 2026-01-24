package com.passmanager.service;

/**
 * Servicio para gestionar el cierre de sesión automático por inactividad.
 */
public interface InactivityService {

    /**
     * Inicia el monitoreo de inactividad.
     *
     * @param onInactivity Callback a ejecutar cuando se detecta inactividad
     */
    void startMonitoring(Runnable onInactivity);

    /**
     * Detiene el monitoreo de inactividad.
     */
    void stopMonitoring();

    /**
     * Resetea el temporizador de inactividad.
     * Debe llamarse cada vez que el usuario realiza alguna acción.
     */
    void resetTimer();

    /**
     * Verifica si el monitoreo está activo.
     *
     * @return true si el monitoreo está activo
     */
    boolean isMonitoring();
}
