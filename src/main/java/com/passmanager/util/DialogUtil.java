package com.passmanager.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DialogUtil {

    public boolean showConfirmDialog(Window owner, String title, String message, String details) {
        AtomicBoolean result = new AtomicBoolean(false);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox container = new VBox(20);
        container.getStyleClass().add("custom-dialog");
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);

        // Icono
        Label iconLabel = new Label("⚠");
        iconLabel.getStyleClass().add("dialog-icon-warning");

        // Título
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        // Mensaje
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);

        // Detalles
        VBox textContainer = new VBox(8);
        textContainer.setAlignment(Pos.CENTER);
        textContainer.getChildren().addAll(titleLabel, messageLabel);

        if (details != null && !details.isEmpty()) {
            Label detailsLabel = new Label(details);
            detailsLabel.getStyleClass().add("dialog-details");
            detailsLabel.setWrapText(true);
            textContainer.getChildren().add(detailsLabel);
        }

        // Botones
        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("dialog-btn-secondary");
        cancelBtn.setOnAction(e -> {
            result.set(false);
            dialog.close();
        });

        Button confirmBtn = new Button("Confirmar");
        confirmBtn.getStyleClass().add("dialog-btn-primary");
        confirmBtn.setOnAction(e -> {
            result.set(true);
            dialog.close();
        });

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(cancelBtn, confirmBtn);

        container.getChildren().addAll(iconLabel, textContainer, buttonBox);

        Scene scene = new Scene(container);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.setFill(null);

        dialog.setScene(scene);
        dialog.showAndWait();

        return result.get();
    }

    public boolean showDeleteConfirmDialog(Window owner, String itemName) {
        AtomicBoolean result = new AtomicBoolean(false);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox container = new VBox(20);
        container.getStyleClass().add("custom-dialog");
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);

        // Icono
        Label iconLabel = new Label("🗑");
        iconLabel.getStyleClass().add("dialog-icon-danger");

        // Título
        Label titleLabel = new Label("Eliminar");
        titleLabel.getStyleClass().add("dialog-title");

        // Mensaje
        Label messageLabel = new Label("¿Eliminar \"" + itemName + "\"?");
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);

        Label detailsLabel = new Label("Esta acción no se puede deshacer.");
        detailsLabel.getStyleClass().add("dialog-details");

        VBox textContainer = new VBox(8);
        textContainer.setAlignment(Pos.CENTER);
        textContainer.getChildren().addAll(titleLabel, messageLabel, detailsLabel);

        // Botones
        Button cancelBtn = new Button("Cancelar");
        cancelBtn.getStyleClass().add("dialog-btn-secondary");
        cancelBtn.setOnAction(e -> {
            result.set(false);
            dialog.close();
        });

        Button deleteBtn = new Button("Eliminar");
        deleteBtn.getStyleClass().add("dialog-btn-danger");
        deleteBtn.setOnAction(e -> {
            result.set(true);
            dialog.close();
        });

        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(cancelBtn, deleteBtn);

        container.getChildren().addAll(iconLabel, textContainer, buttonBox);

        Scene scene = new Scene(container);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.setFill(null);

        dialog.setScene(scene);
        dialog.showAndWait();

        return result.get();
    }

    public void showNotification(Window owner, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.NONE);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        HBox container = new HBox(12);
        container.getStyleClass().add("notification-toast");
        container.setPadding(new Insets(15, 20, 15, 20));
        container.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label("✓");
        iconLabel.getStyleClass().add("notification-icon");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("notification-message");

        container.getChildren().addAll(iconLabel, messageLabel);

        Scene scene = new Scene(container);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.setFill(null);

        dialog.setScene(scene);

        // Posicionar en la parte superior derecha
        if (owner != null) {
            dialog.setX(owner.getX() + owner.getWidth() - 320);
            dialog.setY(owner.getY() + 80);
        }

        dialog.show();

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(dialog::close);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    public void showErrorDialog(Window owner, String title, String message) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox container = new VBox(20);
        container.getStyleClass().add("custom-dialog");
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("✕");
        iconLabel.getStyleClass().add("dialog-icon-error");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);

        VBox textContainer = new VBox(8);
        textContainer.setAlignment(Pos.CENTER);
        textContainer.getChildren().addAll(titleLabel, messageLabel);

        Button okBtn = new Button("Aceptar");
        okBtn.getStyleClass().add("dialog-btn-primary");
        okBtn.setOnAction(e -> dialog.close());

        container.getChildren().addAll(iconLabel, textContainer, okBtn);

        Scene scene = new Scene(container);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        scene.setFill(null);

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
