package com.passmanager.service.impl;

import com.passmanager.service.LockService;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación del servicio de bloqueo automático.
 *
 * <h2>Arquitectura</h2>
 * - Timer de Java para monitoreo de inactividad
 * - AtomicBoolean para thread-safety
 * - Callback pattern para notificar al UI
 *
 * <h2>Configuración por defecto</h2>
 * - Timeout de inactividad: 2 minutos
 * - Bloqueo al minimizar: activado
 * - Intervalo de verificación: 1 segundo
 *
 * @author KeyGuard Team
 */
@Service
public class LockServiceImpl implements LockService {

    // Configuración por defecto
    private static final int DEFAULT_INACTIVITY_MINUTES = 2;
    private static final int MIN_INACTIVITY_MINUTES = 1;
    private static final int MAX_INACTIVITY_MINUTES = 30;
    private static final long CHECK_INTERVAL_MS = 1000; // 1 segundo

    // Estado del servicio
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean lockOnMinimize = new AtomicBoolean(true);
    private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());

    // Configuración
    private int inactivityTimeoutMinutes = DEFAULT_INACTIVITY_MINUTES;
    private long inactivityTimeoutMillis = DEFAULT_INACTIVITY_MINUTES * 60 * 1000;

    // Timer y callback
    private Timer inactivityTimer;
    private Runnable lockCallback;

    @Override
    public void startMonitoring(Runnable onLockCallback) {
        if (isMonitoring.get()) {
            // Ya está monitoreando, solo actualizar callback
            this.lockCallback = onLockCallback;
            return;
        }

        this.lockCallback = onLockCallback;
        this.isMonitoring.set(true);
        this.lastActivityTime.set(System.currentTimeMillis());

        // Iniciar timer de verificación
        inactivityTimer = new Timer("LockService-InactivityTimer", true);
        inactivityTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkInactivity();
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS);
    }

    @Override
    public void stopMonitoring() {
        isMonitoring.set(false);

        if (inactivityTimer != null) {
            inactivityTimer.cancel();
            inactivityTimer = null;
        }

        lockCallback = null;
    }

    @Override
    public void resetTimer() {
        if (isMonitoring.get()) {
            lastActivityTime.set(System.currentTimeMillis());
        }
    }

    @Override
    public void lockNow() {
        if (isMonitoring.get() && lockCallback != null) {
            // Ejecutar callback en el hilo de JavaFX
            javafx.application.Platform.runLater(() -> {
                if (lockCallback != null) {
                    lockCallback.run();
                }
            });
        }
    }

    @Override
    public boolean isMonitoring() {
        return isMonitoring.get();
    }

    @Override
    public void setInactivityTimeout(int minutes) {
        if (minutes < MIN_INACTIVITY_MINUTES || minutes > MAX_INACTIVITY_MINUTES) {
            throw new IllegalArgumentException(
                    "Timeout debe estar entre " + MIN_INACTIVITY_MINUTES +
                            " y " + MAX_INACTIVITY_MINUTES + " minutos"
            );
        }

        this.inactivityTimeoutMinutes = minutes;
        this.inactivityTimeoutMillis = minutes * 60 * 1000L;
    }

    @Override
    public void setLockOnMinimize(boolean enabled) {
        this.lockOnMinimize.set(enabled);
    }

    @Override
    public int getInactivityTimeout() {
        return inactivityTimeoutMinutes;
    }

    @Override
    public boolean isLockOnMinimizeEnabled() {
        return lockOnMinimize.get();
    }

    /**
     * Verifica si ha pasado el tiempo de inactividad.
     * Se ejecuta cada segundo por el timer.
     */
    private void checkInactivity() {
        if (!isMonitoring.get()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastActivity = lastActivityTime.get();
        long timeSinceLastActivity = currentTime - lastActivity;

        if (timeSinceLastActivity >= inactivityTimeoutMillis) {
            // Tiempo de inactividad excedido, bloquear
            lockNow();
        }
    }
}
