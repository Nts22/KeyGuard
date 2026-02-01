package com.passmanager.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

/**
 * Utilidad para mostrar notificaciones tipo "toast" estilo Bitwarden/1Password.
 *
 * <h2>Características</h2>
 * - Notificaciones no intrusivas en la parte superior de la ventana
 * - Fade in/out automático
 * - Auto-desaparece después de un tiempo configurable
 * - Diseño moderno con íconos
 *
 * @author KeyGuard Team
 */
@Component
public class ToastUtil {

    private static final double TOAST_DISPLAY_SECONDS = 3.5;

    /**
     * Muestra una notificación de éxito (verde).
     *
     * @param owner Ventana padre
     * @param message Mensaje a mostrar
     */
    public void showSuccess(Window owner, String message) {
        showToast(owner, message, ToastType.SUCCESS);
    }

    /**
     * Muestra una notificación informativa (azul).
     *
     * @param owner Ventana padre
     * @param message Mensaje a mostrar
     */
    public void showInfo(Window owner, String message) {
        showToast(owner, message, ToastType.INFO);
    }

    /**
     * Muestra una notificación de advertencia (amarillo).
     *
     * @param owner Ventana padre
     * @param message Mensaje a mostrar
     */
    public void showWarning(Window owner, String message) {
        showToast(owner, message, ToastType.WARNING);
    }

    /**
     * Muestra un toast con duración personalizada.
     *
     * @param owner Ventana padre
     * @param message Mensaje a mostrar
     * @param type Tipo de toast
     * @param durationSeconds Duración en segundos
     */
    public void showToast(Window owner, String message, ToastType type, double durationSeconds) {
        if (owner == null) return;

        Popup popup = new Popup();

        // Crear contenedor del toast
        StackPane toastContainer = new StackPane();
        toastContainer.setStyle(
            "-fx-background-color: " + type.backgroundColor + ";" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 12 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"
        );

        // Icono + mensaje
        VBox content = new VBox(0);
        content.setAlignment(Pos.CENTER);

        Label messageLabel = new Label(type.icon + "  " + message);
        messageLabel.setStyle(
            "-fx-text-fill: " + type.textColor + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: bold;"
        );

        content.getChildren().add(messageLabel);
        toastContainer.getChildren().add(content);

        popup.getContent().add(toastContainer);

        // Posicionar en la parte superior-central de la ventana
        double x = owner.getX() + (owner.getWidth() - 300) / 2;
        double y = owner.getY() + 60;

        // Fade in
        toastContainer.setOpacity(0);
        popup.show(owner, x, y);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Auto-cerrar después de la duración especificada
        PauseTransition pause = new PauseTransition(Duration.seconds(durationSeconds));
        pause.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastContainer);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> popup.hide());
            fadeOut.play();
        });
        pause.play();
    }

    /**
     * Muestra un toast con duración por defecto.
     */
    public void showToast(Window owner, String message, ToastType type) {
        showToast(owner, message, type, TOAST_DISPLAY_SECONDS);
    }

    /**
     * Tipos de notificación toast.
     */
    public enum ToastType {
        SUCCESS("✓", "#10b981", "white"),
        INFO("ℹ", "#3b82f6", "white"),
        WARNING("⚠", "#f59e0b", "white");

        private final String icon;
        private final String backgroundColor;
        private final String textColor;

        ToastType(String icon, String backgroundColor, String textColor) {
            this.icon = icon;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
        }
    }
}
