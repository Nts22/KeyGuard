package com.passmanager.controller;

import com.passmanager.model.dto.CategoryDTO;
import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.AuthService;
import com.passmanager.service.CategoryService;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.util.ClipboardUtil;
import com.passmanager.util.DialogUtil;
import com.passmanager.util.FxmlLoaderUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class MainController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<PasswordEntryDTO> passwordTable;
    @FXML private TableColumn<PasswordEntryDTO, String> titleColumn;
    @FXML private TableColumn<PasswordEntryDTO, String> usernameColumn;
    @FXML private TableColumn<PasswordEntryDTO, String> emailColumn;
    @FXML private TableColumn<PasswordEntryDTO, String> categoryColumn;
    @FXML private TableColumn<PasswordEntryDTO, Void> actionsColumn;
    @FXML private VBox categoriesContainer;
    @FXML private Button allCategoriesBtn;
    @FXML private Label currentUserLabel;
    @FXML private Label userInitialsLabel;
    @FXML private VBox sidebar;
    @FXML private Button toggleSidebarBtn;
    @FXML private Pagination pagination;

    private boolean sidebarVisible = true;
    private static final int ITEMS_PER_PAGE = 20;

    private final PasswordEntryService passwordEntryService;
    private final CategoryService categoryService;
    private final AuthService authService;
    private final ClipboardUtil clipboardUtil;
    private final DialogUtil dialogUtil;
    private final FxmlLoaderUtil fxmlLoaderUtil;

    private ObservableList<PasswordEntryDTO> passwordList = FXCollections.observableArrayList();
    private List<PasswordEntryDTO> allPasswords = new ArrayList<>();
    private Long selectedCategoryId = null;

    public MainController(PasswordEntryService passwordEntryService,
                          CategoryService categoryService,
                          AuthService authService,
                          ClipboardUtil clipboardUtil,
                          DialogUtil dialogUtil,
                          FxmlLoaderUtil fxmlLoaderUtil) {
        this.passwordEntryService = passwordEntryService;
        this.categoryService = categoryService;
        this.authService = authService;
        this.clipboardUtil = clipboardUtil;
        this.dialogUtil = dialogUtil;
        this.fxmlLoaderUtil = fxmlLoaderUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCurrentUser();
        setupTable();
        setupPagination();
        loadCategories();
        loadPasswords();
    }

    private void loadCurrentUser() {
        String username = authService.getCurrentUser().getUsername();
        currentUserLabel.setText(username);
        userInitialsLabel.setText(getInitials(username));
    }

    private String getInitials(String username) {
        if (username == null || username.isEmpty()) {
            return "?";
        }
        String[] parts = username.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return username.substring(0, Math.min(2, username.length())).toUpperCase();
    }

    private void setupTable() {
        // Configurar columnas responsivas - se expanden para llenar todo el espacio
        passwordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Solo minWidth para evitar que se hagan muy pequeñas
        titleColumn.setMinWidth(100);
        usernameColumn.setMinWidth(80);
        emailColumn.setMinWidth(120);
        categoryColumn.setMinWidth(80);
        actionsColumn.setMinWidth(180);
        actionsColumn.setPrefWidth(200);

        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        emailColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCategoryName() != null ? data.getValue().getCategoryName() : "-"));

        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Ver");
            private final Button copyBtn = new Button("Copiar");
            private final Button editBtn = new Button("Editar");
            private final Button deleteBtn = new Button("X");
            private final HBox buttons = new HBox(5, viewBtn, copyBtn, editBtn, deleteBtn);

            {
                // Alinear botones a la derecha
                buttons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

                viewBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-view");
                copyBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-copy");
                editBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-edit");
                deleteBtn.getStyleClass().addAll("table-action-btn", "table-action-btn-delete");

                // Tooltips para mostrar la descripción al pasar el mouse
                viewBtn.setTooltip(new Tooltip("Ver detalle de la entrada"));
                copyBtn.setTooltip(new Tooltip("Copiar contraseña al portapapeles"));
                editBtn.setTooltip(new Tooltip("Editar esta entrada"));
                deleteBtn.setTooltip(new Tooltip("Eliminar esta entrada"));

                viewBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    openPasswordDetail(entry);
                });

                copyBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    clipboardUtil.copyToClipboardWithAutoClear(entry.getPassword());
                    showNotification("Contraseña copiada (se borrará en " + clipboardUtil.getClearDelaySeconds() + "s)");
                });

                editBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    openPasswordForm(entry);
                });

                deleteBtn.setOnAction(event -> {
                    PasswordEntryDTO entry = getTableView().getItems().get(getIndex());
                    confirmDelete(entry);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        passwordTable.setItems(passwordList);

        // Doble-click para abrir detalle
        passwordTable.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                PasswordEntryDTO selected = passwordTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openPasswordDetail(selected);
                }
            }
        });
    }

    private void loadCategories() {
        categoriesContainer.getChildren().clear();
        List<CategoryDTO> categories = categoryService.findAll();

        for (CategoryDTO category : categories) {
            // Contenedor HBox para la categoría con botón de eliminar
            HBox categoryRow = new HBox();
            categoryRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            categoryRow.getStyleClass().add("category-row");

            Button categoryBtn = new Button(category.getName() + " (" + category.getEntryCount() + ")");
            categoryBtn.getStyleClass().add("category-item");
            categoryBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(categoryBtn, javafx.scene.layout.Priority.ALWAYS);
            categoryBtn.setOnAction(e -> selectCategory(category.getId(), categoryBtn));

            // Botón de eliminar (oculto por defecto, visible al hover)
            Button deleteBtn = new Button("×");
            deleteBtn.getStyleClass().add("category-delete-btn");
            deleteBtn.setTooltip(new Tooltip("Eliminar categoría"));
            deleteBtn.setVisible(false);
            deleteBtn.setOnAction(e -> handleDeleteCategory(category));

            // Mostrar/ocultar botón eliminar al pasar el mouse
            categoryRow.setOnMouseEntered(e -> deleteBtn.setVisible(true));
            categoryRow.setOnMouseExited(e -> deleteBtn.setVisible(false));

            // Menú contextual para editar
            ContextMenu contextMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("Editar");
            editItem.setOnAction(e -> handleEditCategory(category));
            contextMenu.getItems().add(editItem);
            categoryBtn.setContextMenu(contextMenu);

            categoryRow.getChildren().addAll(categoryBtn, deleteBtn);
            categoriesContainer.getChildren().add(categoryRow);
        }
    }

    private void handleEditCategory(CategoryDTO category) {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/category-form.fxml");
            Parent formView = loader.load();

            CategoryFormController formController = loader.getController();
            formController.setCategory(category);
            formController.setOnSaveCallback(this::loadCategories);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Editar Categoría");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(formView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            formController.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void handleDeleteCategory(CategoryDTO category) {
        // Verificar si la categoría tiene entradas
        String message = category.getEntryCount() > 0
                ? "Esta categoría tiene " + category.getEntryCount() + " entrada(s). Las entradas quedarán sin categoría."
                : "¿Eliminar la categoría \"" + category.getName() + "\"?";

        boolean confirmed = dialogUtil.showConfirmDialog(
                passwordTable.getScene().getWindow(),
                "Eliminar Categoría",
                "¿Eliminar \"" + category.getName() + "\"?",
                message
        );

        if (confirmed) {
            categoryService.delete(category.getId());
            // Si la categoría eliminada era la seleccionada, volver a "Todas"
            if (category.getId().equals(selectedCategoryId)) {
                selectedCategoryId = null;
                updateCategorySelection(allCategoriesBtn);
            }
            loadCategories();
            loadPasswords();
        }
    }

    @FXML
    private void handleToggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
        toggleSidebarBtn.setText(sidebarVisible ? "☰" : "☰");
    }

    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
    }

    private VBox createPage(int pageIndex) {
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, allPasswords.size());

        passwordList.setAll(allPasswords.subList(fromIndex, toIndex));

        return new VBox();
    }

    private void loadPasswords() {
        List<PasswordEntryDTO> entries;
        String search = searchField.getText();

        if (selectedCategoryId == null) {
            entries = (search == null || search.isEmpty())
                    ? passwordEntryService.findAll()
                    : passwordEntryService.search(search);
        } else {
            entries = (search == null || search.isEmpty())
                    ? passwordEntryService.findByCategory(selectedCategoryId)
                    : passwordEntryService.searchByCategory(selectedCategoryId, search);
        }

        allPasswords = entries;

        int pageCount = (int) Math.ceil((double) allPasswords.size() / ITEMS_PER_PAGE);
        pagination.setPageCount(Math.max(1, pageCount));
        pagination.setCurrentPageIndex(0);
    }

    @FXML
    private void handleSearch() {
        loadPasswords();
    }

    @FXML
    private void handleAllCategories() {
        selectedCategoryId = null;
        updateCategorySelection(allCategoriesBtn);
        loadPasswords();
    }

    private void selectCategory(Long categoryId, Button button) {
        selectedCategoryId = categoryId;
        updateCategorySelection(button);
        loadPasswords();
    }

    private void updateCategorySelection(Button selectedButton) {
        allCategoriesBtn.getStyleClass().remove("category-item-selected");
        categoriesContainer.getChildren().forEach(node -> {
            if (node instanceof HBox) {
                // Las categorías ahora están en HBox, buscar el botón dentro
                ((HBox) node).getChildren().forEach(child -> {
                    if (child instanceof Button) {
                        child.getStyleClass().remove("category-item-selected");
                    }
                });
            } else if (node instanceof Button) {
                node.getStyleClass().remove("category-item-selected");
            }
        });
        selectedButton.getStyleClass().add("category-item-selected");
    }

    @FXML
    private void handleNewEntry() {
        openPasswordForm(null);
    }

    @FXML
    private void handleNewCategory() {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/category-form.fxml");
            Parent formView = loader.load();

            CategoryFormController formController = loader.getController();
            formController.setOnSaveCallback(this::loadCategories);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Nueva Categoría");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(formView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            formController.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void openPasswordDetail(PasswordEntryDTO entry) {
        try {
            // Cargar la entrada completa con campos personalizados
            PasswordEntryDTO fullEntry = passwordEntryService.findById(entry.getId()).orElse(entry);

            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/password-detail.fxml");
            Parent detailView = loader.load();

            PasswordDetailController detailController = loader.getController();
            detailController.setEntry(fullEntry);
            detailController.setOnEditCallback(() -> openPasswordForm(fullEntry));

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Detalle: " + entry.getTitle());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(detailView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            detailController.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            // Recargar datos por si se editó
            loadPasswords();
            loadCategories();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el detalle: " + e.getMessage());
        }
    }

    private void openPasswordForm(PasswordEntryDTO entry) {
        try {
            FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/password-form.fxml");
            Parent formView = loader.load();

            PasswordFormController formController = loader.getController();
            formController.setEntry(entry);
            formController.setOnSaveCallback(() -> {
                loadPasswords();
                loadCategories();
            });

            Stage dialogStage = new Stage();
            dialogStage.setTitle(entry == null ? "Nueva Entrada" : "Editar Entrada");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(passwordTable.getScene().getWindow());

            Scene scene = new Scene(formView);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setMinWidth(550);
            dialogStage.setMinHeight(500);

            formController.setDialogStage(dialogStage);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void confirmDelete(PasswordEntryDTO entry) {
        boolean confirmed = dialogUtil.showDeleteConfirmDialog(
                passwordTable.getScene().getWindow(),
                entry.getTitle()
        );

        if (confirmed) {
            passwordEntryService.delete(entry.getId());
            loadPasswords();
            loadCategories();
        }
    }

    private void showNotification(String message) {
        dialogUtil.showNotification(passwordTable.getScene().getWindow(), message);
    }

    private void showError(String title, String message) {
        dialogUtil.showErrorDialog(passwordTable.getScene().getWindow(), title, message);
    }

    @FXML
    private void handleLogout() {
        boolean confirmed = dialogUtil.showConfirmDialog(
                passwordTable.getScene().getWindow(),
                "Cerrar Sesión",
                "¿Cerrar sesión?",
                "Volverás a la pantalla de inicio de sesión."
        );

        if (confirmed) {
            try {
                authService.logout();

                FXMLLoader loader = fxmlLoaderUtil.getLoader("/fxml/login.fxml");
                Parent loginView = loader.load();

                Stage stage = (Stage) passwordTable.getScene().getWindow();
                Scene scene = new Scene(loginView);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                stage.setScene(scene);
                stage.setTitle("KeyGuard - Login");
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error", "No se pudo cerrar sesión: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExit() {
        boolean confirmed = dialogUtil.showConfirmDialog(
                passwordTable.getScene().getWindow(),
                "Salir",
                "¿Salir de la aplicación?",
                "Se cerrarán todas las ventanas."
        );

        if (confirmed) {
            javafx.application.Platform.exit();
        }
    }
}
