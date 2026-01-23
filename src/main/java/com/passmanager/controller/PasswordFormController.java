package com.passmanager.controller;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.CategoryService;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.service.PasswordGeneratorService;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
@org.springframework.context.annotation.Scope("prototype")
public class PasswordFormController implements Initializable {

    @FXML private TextField titleField;
    @FXML private ComboBox<CategoryDTO> categoryCombo;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordFieldVisible;
    @FXML private TextField urlField;
    @FXML private TextArea notesField;
    @FXML private ProgressBar strengthBar;
    @FXML private Label strengthLabel;
    @FXML private Button togglePasswordBtn;
    @FXML private VBox customFieldsContainer;
    @FXML private Label errorLabel;

    private final PasswordEntryService passwordEntryService;
    private final CategoryService categoryService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final FxmlLoaderUtil fxmlLoaderUtil;

    private Stage dialogStage;
    private PasswordEntryDTO currentEntry;
    private Runnable onSaveCallback;
    private boolean passwordVisible = false;
    private List<CustomFieldRow> customFieldRows = new ArrayList<>();

    public PasswordFormController(PasswordEntryService passwordEntryService,
                                   CategoryService categoryService,
                                   PasswordGeneratorService passwordGeneratorService,
                                   FxmlLoaderUtil fxmlLoaderUtil) {
        this.passwordEntryService = passwordEntryService;
        this.categoryService = categoryService;
        this.passwordGeneratorService = passwordGeneratorService;
        this.fxmlLoaderUtil = fxmlLoaderUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCategories();
        setupPasswordStrength();
    }

    private void loadCategories() {
        List<CategoryDTO> categories = categoryService.findAll();
        categoryCombo.setItems(FXCollections.observableArrayList(categories));
        categoryCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CategoryDTO category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public CategoryDTO fromString(String string) {
                return null;
            }
        });
    }

    private void setupPasswordStrength() {
        // Sincronizar ambos campos de contraseña
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordFieldVisible.getText().equals(newVal)) {
                passwordFieldVisible.setText(newVal);
            }
            updateStrength(newVal);
        });

        passwordFieldVisible.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!passwordField.getText().equals(newVal)) {
                passwordField.setText(newVal);
            }
            updateStrength(newVal);
        });
    }

    private void updateStrength(String password) {
        int strength = passwordGeneratorService.calculateStrength(password);
        strengthBar.setProgress(strength / 100.0);

        strengthBar.getStyleClass().removeAll("strength-weak", "strength-medium", "strength-strong");

        if (strength < 40) {
            strengthBar.getStyleClass().add("strength-weak");
            strengthLabel.setText("Débil");
        } else if (strength < 70) {
            strengthBar.getStyleClass().add("strength-medium");
            strengthLabel.setText("Media");
        } else {
            strengthBar.getStyleClass().add("strength-strong");
            strengthLabel.setText("Fuerte");
        }
    }

    public void setEntry(PasswordEntryDTO entry) {
        this.currentEntry = entry;

        if (entry != null) {
            titleField.setText(entry.getTitle());
            usernameField.setText(entry.getUsername());
            emailField.setText(entry.getEmail());
            passwordField.setText(entry.getPassword());
            urlField.setText(entry.getUrl());
            notesField.setText(entry.getNotes());

            if (entry.getCategoryId() != null) {
                categoryCombo.getItems().stream()
                        .filter(c -> c.getId().equals(entry.getCategoryId()))
                        .findFirst()
                        .ifPresent(categoryCombo::setValue);
            }

            // Cargar campos personalizados
            if (entry.getCustomFields() != null) {
                for (PasswordEntryDTO.CustomFieldDTO field : entry.getCustomFields()) {
                    addCustomFieldRow(field.getFieldName(), field.getFieldValue(), field.isSensitive());
                }
            }

            updateStrength(entry.getPassword());
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            // Mostrar contraseña (TextField visible)
            passwordFieldVisible.setText(passwordField.getText());
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordFieldVisible.setVisible(true);
            passwordFieldVisible.setManaged(true);
            togglePasswordBtn.setText("🔒");
        } else {
            // Ocultar contraseña (PasswordField visible)
            passwordField.setText(passwordFieldVisible.getText());
            passwordFieldVisible.setVisible(false);
            passwordFieldVisible.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            togglePasswordBtn.setText("👁");
        }
    }

    @FXML
    private void handleOpenGenerator() {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/password-generator.fxml");
            Parent generatorView = loader.load();

            PasswordGeneratorController generatorController = loader.getController();
            generatorController.setOnSelectCallback(password -> {
                passwordField.setText(password);
                passwordFieldVisible.setText(password);
            });

            Stage generatorStage = new Stage();
            generatorStage.setTitle("Generador de Contraseñas");
            generatorStage.initModality(Modality.APPLICATION_MODAL);
            generatorStage.initOwner(dialogStage);

            Scene scene = new Scene(generatorView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            generatorStage.setScene(scene);
            generatorStage.setResizable(false);

            generatorController.setDialogStage(generatorStage);
            generatorStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error al abrir el generador: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddCustomField() {
        addCustomFieldRow("", "", false);
    }

    private void addCustomFieldRow(String name, String value, boolean sensitive) {
        HBox row = new HBox(10);
        row.setStyle("-fx-padding: 5 0;");

        TextField nameField = new TextField(name);
        nameField.setPromptText("Nombre del campo");
        nameField.setPrefWidth(150);

        TextField valueField = new TextField(value);
        valueField.setPromptText("Valor");
        HBox.setHgrow(valueField, Priority.ALWAYS);

        CheckBox sensitiveCheck = new CheckBox("Sensible");
        sensitiveCheck.setSelected(sensitive);

        Button removeBtn = new Button("X");
        removeBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 2 6;");

        CustomFieldRow fieldRow = new CustomFieldRow(row, nameField, valueField, sensitiveCheck);
        customFieldRows.add(fieldRow);

        removeBtn.setOnAction(e -> {
            customFieldsContainer.getChildren().remove(row);
            customFieldRows.remove(fieldRow);
        });

        row.getChildren().addAll(nameField, valueField, sensitiveCheck, removeBtn);
        customFieldsContainer.getChildren().add(row);
    }

    @FXML
    private void handleSave() {
        hideError();

        String title = titleField.getText().trim();
        String password = passwordField.getText();

        if (title.isEmpty()) {
            showError("El título es obligatorio");
            return;
        }

        if (password.isEmpty()) {
            showError("La contraseña es obligatoria");
            return;
        }

        try {
            List<PasswordEntryDTO.CustomFieldDTO> customFields = new ArrayList<>();
            for (CustomFieldRow row : customFieldRows) {
                String fieldName = row.nameField.getText().trim();
                String fieldValue = row.valueField.getText();
                if (!fieldName.isEmpty()) {
                    customFields.add(PasswordEntryDTO.CustomFieldDTO.builder()
                            .fieldName(fieldName)
                            .fieldValue(fieldValue)
                            .sensitive(row.sensitiveCheck.isSelected())
                            .build());
                }
            }

            PasswordEntryDTO dto = PasswordEntryDTO.builder()
                    .title(title)
                    .username(usernameField.getText().trim())
                    .email(emailField.getText().trim())
                    .password(password)
                    .url(urlField.getText().trim())
                    .notes(notesField.getText())
                    .categoryId(categoryCombo.getValue() != null ? categoryCombo.getValue().getId() : null)
                    .customFields(customFields)
                    .build();

            if (currentEntry != null) {
                passwordEntryService.update(currentEntry.getId(), dto);
            } else {
                passwordEntryService.create(dto);
            }

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (Exception e) {
            showError("Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
    }

    private static class CustomFieldRow {
        HBox container;
        TextField nameField;
        TextField valueField;
        CheckBox sensitiveCheck;

        CustomFieldRow(HBox container, TextField nameField, TextField valueField, CheckBox sensitiveCheck) {
            this.container = container;
            this.nameField = nameField;
            this.valueField = valueField;
            this.sensitiveCheck = sensitiveCheck;
        }
    }
}
