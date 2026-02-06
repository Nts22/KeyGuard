package com.passmanager.controller;

import com.passmanager.model.entity.Tag;
import com.passmanager.model.entity.User;
import com.passmanager.service.TagService;
import com.passmanager.service.UserService;
import com.passmanager.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class TagManagerController implements Initializable {

    @FXML private ListView<Tag> tagListView;
    @FXML private TextField tagNameField;
    @FXML private ColorPicker colorPicker;
    @FXML private Button addTagBtn;
    @FXML private Button updateTagBtn;
    @FXML private Button deleteTagBtn;
    @FXML private Button closeBtn;

    private final TagService tagService;
    private final UserService userService;
    private final DialogUtil dialogUtil;

    private ObservableList<Tag> tagList = FXCollections.observableArrayList();
    private Tag selectedTag = null;

    public TagManagerController(TagService tagService, UserService userService, DialogUtil dialogUtil) {
        this.tagService = tagService;
        this.userService = userService;
        this.dialogUtil = dialogUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTagList();
        loadTags();
        updateTagBtn.setDisable(true);
        deleteTagBtn.setDisable(true);

        // Listener para selección de tag
        tagListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedTag = newVal;
            if (newVal != null) {
                tagNameField.setText(newVal.getName());
                try {
                    colorPicker.setValue(javafx.scene.paint.Color.web(newVal.getColor()));
                } catch (Exception e) {
                    colorPicker.setValue(javafx.scene.paint.Color.web("#3B82F6"));
                }
                updateTagBtn.setDisable(false);
                deleteTagBtn.setDisable(false);
                addTagBtn.setDisable(true);
            } else {
                clearForm();
            }
        });

        // Listener para cambios en el campo de nombre
        tagNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            addTagBtn.setDisable(newVal == null || newVal.trim().isEmpty());
        });
    }

    private void setupTagList() {
        tagListView.setItems(tagList);
        tagListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                if (empty || tag == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // Pill de color
                    Label colorLabel = new Label(" ");
                    colorLabel.setStyle(
                            "-fx-background-color: " + tag.getColor() + ";" +
                            "-fx-padding: 8 16;" +
                            "-fx-background-radius: 12;" +
                            "-fx-min-width: 30;"
                    );

                    Label nameLabel = new Label(tag.getName());
                    nameLabel.setStyle("-fx-font-size: 14;");

                    hbox.getChildren().addAll(colorLabel, nameLabel);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void loadTags() {
        User currentUser = userService.getCurrentUser();
        List<Tag> tags = tagService.findAllByUser(currentUser);
        tagList.setAll(tags);
    }

    @FXML
    private void handleAddTag() {
        String name = tagNameField.getText().trim();
        if (name.isEmpty()) {
            dialogUtil.showErrorDialog(tagNameField.getScene().getWindow(), "Campo requerido", "El nombre del tag no puede estar vacío.");
            return;
        }

        User currentUser = userService.getCurrentUser();
        if (tagService.existsByNameAndUser(name, currentUser)) {
            dialogUtil.showErrorDialog(tagNameField.getScene().getWindow(), "Tag duplicado", "Ya existe un tag con este nombre.");
            return;
        }

        String color = toHexString(colorPicker.getValue());
        tagService.createTag(name, color, currentUser);
        loadTags();
        clearForm();
    }

    @FXML
    private void handleUpdateTag() {
        if (selectedTag == null) {
            return;
        }

        String name = tagNameField.getText().trim();
        if (name.isEmpty()) {
            dialogUtil.showErrorDialog(tagNameField.getScene().getWindow(), "Campo requerido", "El nombre del tag no puede estar vacío.");
            return;
        }

        String color = toHexString(colorPicker.getValue());
        try {
            tagService.updateTag(selectedTag.getId(), name, color);
            loadTags();
            clearForm();
        } catch (IllegalArgumentException e) {
            dialogUtil.showErrorDialog(tagNameField.getScene().getWindow(), "Tag duplicado", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteTag() {
        if (selectedTag == null) {
            return;
        }

        boolean confirmed = dialogUtil.showConfirmDialog(
                tagNameField.getScene().getWindow(),
                "Eliminar Tag",
                "¿Estás seguro de eliminar el tag '" + selectedTag.getName() + "'?",
                "Se eliminará de todas las contraseñas asociadas."
        );

        if (confirmed) {
            tagService.deleteTag(selectedTag.getId());
            loadTags();
            clearForm();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }

    private void clearForm() {
        tagNameField.clear();
        colorPicker.setValue(javafx.scene.paint.Color.web("#3B82F6"));
        selectedTag = null;
        tagListView.getSelectionModel().clearSelection();
        updateTagBtn.setDisable(true);
        deleteTagBtn.setDisable(true);
        addTagBtn.setDisable(false);
    }

    private String toHexString(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
