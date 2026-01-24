package com.passmanager.service.impl;

import com.passmanager.service.InactivityService;
import javafx.application.Platform;
import org.springframework.stereotype.Service;

import java.util.Timer;
import java.util.TimerTask;

@Service
public class InactivityServiceImpl implements InactivityService {

    private static final long INACTIVITY_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutos en milisegundos

    private Timer inactivityTimer;
    private TimerTask currentTask;
    private Runnable onInactivityCallback;
    private boolean isMonitoring;

    @Override
    public void startMonitoring(Runnable onInactivity) {
        if (isMonitoring) {
            stopMonitoring();
        }

        this.onInactivityCallback = onInactivity;
        this.isMonitoring = true;
        this.inactivityTimer = new Timer("InactivityTimer", true);

        resetTimer();
    }

    @Override
    public void stopMonitoring() {
        isMonitoring = false;

        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }

        if (inactivityTimer != null) {
            inactivityTimer.cancel();
            inactivityTimer.purge();
            inactivityTimer = null;
        }

        onInactivityCallback = null;
    }

    @Override
    public void resetTimer() {
        if (!isMonitoring || inactivityTimer == null) {
            return;
        }

        // Cancelar la tarea anterior si existe
        if (currentTask != null) {
            currentTask.cancel();
        }

        // Crear nueva tarea
        currentTask = new TimerTask() {
            @Override
            public void run() {
                if (onInactivityCallback != null) {
                    // Ejecutar el callback en el hilo de JavaFX
                    Platform.runLater(() -> {
                        if (isMonitoring && onInactivityCallback != null) {
                            onInactivityCallback.run();
                        }
                    });
                }
            }
        };

        // Programar la nueva tarea
        inactivityTimer.schedule(currentTask, INACTIVITY_TIMEOUT_MS);
    }

    @Override
    public boolean isMonitoring() {
        return isMonitoring;
    }
}
