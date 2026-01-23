package com.passmanager.controller;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.service.CategoryService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
@org.springframework.context.annotation.Scope("prototype")
public class CategoryFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> iconCombo;
    @FXML private Label errorLabel;

    private final CategoryService categoryService;

    private Stage dialogStage;
    private CategoryDTO currentCategory;
    private Runnable onSaveCallback;
    private boolean saved = false;

    public CategoryFormController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        iconCombo.setItems(FXCollections.observableArrayList(
                "social", "bank", "email", "video", "work", "shopping",
                "gaming", "health", "travel", "education", "other"
        ));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCategory(CategoryDTO category) {
        this.currentCategory = category;
        if (category != null) {
            dialogTitle.setText("Editar Categoría");
            nameField.setText(category.getName());
            if (category.getIcon() != null) {
                iconCombo.setValue(category.getIcon());
            }
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        hideError();

        String name = nameField.getText().trim();

        if (name.isEmpty()) {
            showError("El nombre es obligatorio");
            return;
        }

        try {
            String icon = iconCombo.getValue();

            if (currentCategory != null) {
                categoryService.update(currentCategory.getId(), name, icon);
            } else {
                categoryService.create(name, icon);
            }

            saved = true;

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            dialogStage.close();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
