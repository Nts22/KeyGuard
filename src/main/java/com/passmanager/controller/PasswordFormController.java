package com.passmanager.controller;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.CategoryService;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.service.PasswordBreachService;
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
    private final PasswordBreachService passwordBreachService;
    private final FxmlLoaderUtil fxmlLoaderUtil;

    private Stage dialogStage;
    private PasswordEntryDTO currentEntry;
    private Runnable onSaveCallback;
    private boolean passwordVisible = false;
    private List<CustomFieldRow> customFieldRows = new ArrayList<>();

    public PasswordFormController(PasswordEntryService passwordEntryService,
                                   CategoryService categoryService,
                                   PasswordGeneratorService passwordGeneratorService,
                                   PasswordBreachService passwordBreachService,
                                   FxmlLoaderUtil fxmlLoaderUtil) {
        this.passwordEntryService = passwordEntryService;
        this.categoryService = categoryService;
        this.passwordGeneratorService = passwordGeneratorService;
        this.passwordBreachService = passwordBreachService;
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

        // PASO 1: Verificar si la contraseña ha sido comprometida
        // ¿Por qué hacerlo aquí y no después de guardar?
        // - Mejor UX: avisar al usuario ANTES de guardar
        // - Le damos opción de cambiar la contraseña inmediatamente
        // - Evitamos guardar y luego mostrar advertencia
        if (!checkPasswordBreach(password)) {
            // El usuario canceló después de ver la advertencia
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

    /**
     * Verifica si una contraseña ha sido comprometida usando Have I Been Pwned API.
     *
     * ¿Por qué verificar automáticamente?
     * - El 65% de usuarios reutiliza contraseñas entre sitios
     * - Si una contraseña fue filtrada en LinkedIn, puede usarse para atacar otras cuentas
     * - Es mejor prevenir que el usuario use contraseñas conocidas por hackers
     *
     * ¿Por qué no forzar el cambio?
     * - Puede haber falsos positivos (contraseñas comunes legítimas)
     * - El usuario puede tener razones válidas (contraseña temporal, cuenta de prueba)
     * - Mejor educar que forzar
     *
     * ¿Qué pasa si la API falla?
     * - No bloqueamos al usuario
     * - Mostramos advertencia pero permitimos continuar
     * - El servicio ya maneja timeouts y errores de red
     *
     * @param password La contraseña a verificar
     * @return true si el usuario quiere continuar (contraseña segura o acepta riesgo),
     *         false si el usuario cancela
     */
    private boolean checkPasswordBreach(String password) {
        try {
            // Llamar al servicio de verificación de brechas
            PasswordBreachService.BreachCheckResult result = passwordBreachService.checkPassword(password);

            // Si la contraseña NO está comprometida, todo bien
            if (!result.isBreached()) {
                return true;
            }

            // La contraseña FUE encontrada en brechas de seguridad
            // Mostrar advertencia al usuario con detalles

            PasswordBreachService.SeverityLevel severity = result.getSeverityLevel();
            int occurrences = result.getOccurrences();

            // Construir mensaje informativo basado en la severidad
            StringBuilder message = new StringBuilder();
            message.append("⚠️ ADVERTENCIA DE SEGURIDAD ⚠️\n\n");
            message.append("Esta contraseña ha sido encontrada en ").append(occurrences);
            message.append(occurrences == 1 ? " brecha" : " brechas").append(" de seguridad conocidas.\n\n");

            message.append("Nivel de riesgo: ").append(severity.getLabel()).append("\n");
            message.append(severity.getDescription()).append("\n\n");

            // Explicar el riesgo según la severidad
            if (severity == PasswordBreachService.SeverityLevel.CRITICAL) {
                message.append("⛔ ALTO RIESGO: Esta contraseña es extremadamente común y conocida por hackers. ");
                message.append("Es MUY RECOMENDABLE que uses una contraseña diferente.\n\n");
            } else if (severity == PasswordBreachService.SeverityLevel.HIGH) {
                message.append("⚠️ RIESGO CONSIDERABLE: Esta contraseña ha sido comprometida múltiples veces. ");
                message.append("Se recomienda encarecidamente usar una contraseña diferente.\n\n");
            } else if (severity == PasswordBreachService.SeverityLevel.MEDIUM) {
                message.append("⚠️ RIESGO MODERADO: Esta contraseña ha aparecido en algunas brechas. ");
                message.append("Considera usar una contraseña más segura.\n\n");
            } else {
                message.append("ℹ️ RIESGO BAJO: Esta contraseña fue encontrada pocas veces. ");
                message.append("Aún así, considera cambiarla por seguridad.\n\n");
            }

            message.append("Fuente: Have I Been Pwned (base de datos de 12+ mil millones de contraseñas filtradas)\n\n");
            message.append("¿Deseas continuar de todas formas?");

            // Mostrar diálogo de confirmación
            // ButtonType.NO es el default para que el usuario tenga que pensar antes de aceptar
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Contraseña Comprometida");
            alert.setHeaderText("Esta contraseña ha sido filtrada en brechas de seguridad");
            alert.setContentText(message.toString());

            // Botones personalizados
            ButtonType buttonContinue = new ButtonType("Continuar de todas formas");
            ButtonType buttonCancel = new ButtonType("Cancelar y cambiar contraseña", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(buttonContinue, buttonCancel);

            // El botón por defecto es "Cancelar" (más seguro)
            alert.getDialogPane().lookupButton(buttonContinue).setStyle("-fx-background-color: #ef4444;");

            // Mostrar y esperar respuesta
            return alert.showAndWait()
                    .map(response -> response == buttonContinue)
                    .orElse(false);

        } catch (PasswordBreachService.PasswordBreachCheckException e) {
            // Error al verificar (red, timeout, etc.)
            // ¿Qué hacer? No bloqueamos al usuario, pero mostramos advertencia

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Verificación no disponible");
            alert.setHeaderText("No se pudo verificar la contraseña");
            alert.setContentText(
                    "No fue posible verificar si la contraseña ha sido comprometida:\n\n" +
                    e.getMessage() + "\n\n" +
                    "Puedes continuar guardando la contraseña, pero ten en cuenta que no " +
                    "pudimos verificar su seguridad.\n\n" +
                    "¿Deseas continuar de todas formas?"
            );

            ButtonType buttonContinue = new ButtonType("Continuar");
            ButtonType buttonCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonContinue, buttonCancel);

            return alert.showAndWait()
                    .map(response -> response == buttonContinue)
                    .orElse(false);
        }
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
