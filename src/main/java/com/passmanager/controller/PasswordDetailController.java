package com.passmanager.controller;

import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.util.ClipboardUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

@Component
@org.springframework.context.annotation.Scope("prototype")
public class PasswordDetailController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label categoryLabel;
    @FXML private VBox usernameContainer;
    @FXML private Label usernameLabel;
    @FXML private VBox emailContainer;
    @FXML private Label emailLabel;
    @FXML private Label passwordLabel;
    @FXML private Button togglePasswordBtn;
    @FXML private VBox urlContainer;
    @FXML private Hyperlink urlLink;
    @FXML private VBox notesContainer;
    @FXML private Label notesLabel;
    @FXML private VBox customFieldsContainer;
    @FXML private Label createdAtLabel;
    @FXML private Label updatedAtLabel;

    private final ClipboardUtil clipboardUtil;

    private Stage dialogStage;
    private PasswordEntryDTO entry;
    private boolean passwordVisible = false;
    private Runnable onEditCallback;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PasswordDetailController(ClipboardUtil clipboardUtil) {
        this.clipboardUtil = clipboardUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setEntry(PasswordEntryDTO entry) {
        this.entry = entry;
        populateFields();
    }

    public void setOnEditCallback(Runnable callback) {
        this.onEditCallback = callback;
    }

    private void populateFields() {
        if (entry == null) return;

        titleLabel.setText(entry.getTitle());
        categoryLabel.setText(entry.getCategoryName() != null ? entry.getCategoryName() : "Sin categoría");

        // Usuario
        if (entry.getUsername() != null && !entry.getUsername().isEmpty()) {
            usernameLabel.setText(entry.getUsername());
            usernameContainer.setVisible(true);
            usernameContainer.setManaged(true);
        } else {
            usernameContainer.setVisible(false);
            usernameContainer.setManaged(false);
        }

        // Email
        if (entry.getEmail() != null && !entry.getEmail().isEmpty()) {
            emailLabel.setText(entry.getEmail());
            emailContainer.setVisible(true);
            emailContainer.setManaged(true);
        } else {
            emailContainer.setVisible(false);
            emailContainer.setManaged(false);
        }

        // Contraseña (oculta por defecto)
        passwordLabel.setText("••••••••••••");

        // URL
        if (entry.getUrl() != null && !entry.getUrl().isEmpty()) {
            urlLink.setText(entry.getUrl());
            urlContainer.setVisible(true);
            urlContainer.setManaged(true);
        } else {
            urlContainer.setVisible(false);
            urlContainer.setManaged(false);
        }

        // Notas
        if (entry.getNotes() != null && !entry.getNotes().isEmpty()) {
            notesLabel.setText(entry.getNotes());
            notesContainer.setVisible(true);
            notesContainer.setManaged(true);
        } else {
            notesContainer.setVisible(false);
            notesContainer.setManaged(false);
        }

        // Campos personalizados
        if (entry.getCustomFields() != null && !entry.getCustomFields().isEmpty()) {
            customFieldsContainer.setVisible(true);
            customFieldsContainer.setManaged(true);

            for (PasswordEntryDTO.CustomFieldDTO field : entry.getCustomFields()) {
                VBox fieldBox = new VBox(4);
                Label nameLabel = new Label(field.getFieldName());
                nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

                HBox valueBox = new HBox(10);
                Label valueLabel = new Label(field.isSensitive() ? "••••••••" : field.getFieldValue());
                valueLabel.setStyle("-fx-font-size: 14px;");

                Button copyBtn = new Button("Copiar");
                copyBtn.getStyleClass().add("button-copy");
                copyBtn.setOnAction(e -> {
                    if (field.isSensitive()) {
                        clipboardUtil.copyToClipboardWithAutoClear(field.getFieldValue());
                    } else {
                        clipboardUtil.copyToClipboard(field.getFieldValue());
                    }
                    showCopiedFeedback(copyBtn);
                });

                if (field.isSensitive()) {
                    Button toggleBtn = new Button("👁");
                    toggleBtn.getStyleClass().add("button-copy");
                    final boolean[] visible = {false};
                    toggleBtn.setOnAction(e -> {
                        visible[0] = !visible[0];
                        valueLabel.setText(visible[0] ? field.getFieldValue() : "••••••••");
                        toggleBtn.setText(visible[0] ? "🔒" : "👁");
                    });
                    valueBox.getChildren().addAll(valueLabel, toggleBtn, copyBtn);
                } else {
                    valueBox.getChildren().addAll(valueLabel, copyBtn);
                }

                fieldBox.getChildren().addAll(nameLabel, valueBox);
                customFieldsContainer.getChildren().add(fieldBox);
            }
        } else {
            customFieldsContainer.setVisible(false);
            customFieldsContainer.setManaged(false);
        }

        // Fechas
        if (entry.getCreatedAt() != null) {
            createdAtLabel.setText("Creado: " + entry.getCreatedAt().format(DATE_FORMATTER));
        }
        if (entry.getUpdatedAt() != null) {
            updatedAtLabel.setText("Actualizado: " + entry.getUpdatedAt().format(DATE_FORMATTER));
        }
    }

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordLabel.setText(entry.getPassword());
            togglePasswordBtn.setText("🔒");
        } else {
            passwordLabel.setText("••••••••••••");
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void handleCopyUsername() {
        if (entry.getUsername() != null) {
            clipboardUtil.copyToClipboard(entry.getUsername());
        }
    }

    @FXML
    private void handleCopyEmail() {
        if (entry.getEmail() != null) {
            clipboardUtil.copyToClipboard(entry.getEmail());
        }
    }

    @FXML
    private void handleCopyPassword() {
        if (entry.getPassword() != null) {
            clipboardUtil.copyToClipboardWithAutoClear(entry.getPassword());
        }
    }

    @FXML
    private void handleCopyUrl() {
        if (entry.getUrl() != null) {
            clipboardUtil.copyToClipboard(entry.getUrl());
        }
    }

    @FXML
    private void handleOpenUrl() {
        if (entry.getUrl() != null && !entry.getUrl().isEmpty()) {
            try {
                String url = entry.getUrl();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleEdit() {
        dialogStage.close();
        if (onEditCallback != null) {
            // Ejecutar el callback después de que el diálogo se cierre completamente
            javafx.application.Platform.runLater(onEditCallback::run);
        }
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }

    private void showCopiedFeedback(Button button) {
        String originalText = button.getText();
        button.setText("✓");
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                javafx.application.Platform.runLater(() -> button.setText(originalText));
            } catch (InterruptedException ignored) {}
        }).start();
    }
}
