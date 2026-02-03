package com.passmanager.controller;

import com.passmanager.model.dto.PasswordEntryDTO;
import com.passmanager.service.PasswordBreachService;
import com.passmanager.service.PasswordEntryService;
import com.passmanager.util.PasswordGeneratorUtil;
import com.passmanager.util.PasswordGeneratorUtil.PasswordStrength;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Component
@org.springframework.context.annotation.Scope("prototype")
public class BreachCheckController implements Initializable {

    // ── Resumen ──────────────────────────────────────
    @FXML private Label totalLabel;
    @FXML private Label breachedLabel;
    @FXML private Label duplicatesLabel;
    @FXML private Label weakLabel;

    // ── Progreso ─────────────────────────────────────
    @FXML private VBox progressContainer;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;

    // ── Tabla: Brechas ───────────────────────────────
    @FXML private TableView<BreachResult> breachTable;
    @FXML private TableColumn<BreachResult, String> breachStatusColumn;
    @FXML private TableColumn<BreachResult, String> breachTitleColumn;
    @FXML private TableColumn<BreachResult, String> breachUsernameColumn;
    @FXML private TableColumn<BreachResult, String> breachSeverityColumn;
    @FXML private TableColumn<BreachResult, String> breachOccurrencesColumn;
    @FXML private TableColumn<BreachResult, String> breachRecommendationColumn;

    // ── Tabla: Duplicadas ────────────────────────────
    @FXML private TableView<DuplicateResult> duplicatesTable;
    @FXML private TableColumn<DuplicateResult, String> dupTitleColumn;
    @FXML private TableColumn<DuplicateResult, String> dupUsernameColumn;
    @FXML private TableColumn<DuplicateResult, String> dupEmailColumn;
    @FXML private TableColumn<DuplicateResult, String> dupGroupColumn;

    // ── Tabla: Débiles ───────────────────────────────
    @FXML private TableView<WeakResult> weakTable;
    @FXML private TableColumn<WeakResult, String> weakTitleColumn;
    @FXML private TableColumn<WeakResult, String> weakUsernameColumn;
    @FXML private TableColumn<WeakResult, String> weakStrengthColumn;
    @FXML private TableColumn<WeakResult, String> weakLengthColumn;

    // ── Servicios ────────────────────────────────────
    private final PasswordEntryService passwordEntryService;
    private final PasswordBreachService passwordBreachService;
    private final PasswordGeneratorUtil passwordGeneratorUtil;

    // ── Estado ───────────────────────────────────────
    private Stage dialogStage;
    private Task<Void> breachTask;

    private final ObservableList<BreachResult> breachResults = FXCollections.observableArrayList();
    private final ObservableList<DuplicateResult> duplicateResults = FXCollections.observableArrayList();
    private final ObservableList<WeakResult> weakResults = FXCollections.observableArrayList();

    public BreachCheckController(PasswordEntryService passwordEntryService,
                                 PasswordBreachService passwordBreachService,
                                 PasswordGeneratorUtil passwordGeneratorUtil) {
        this.passwordEntryService = passwordEntryService;
        this.passwordBreachService = passwordBreachService;
        this.passwordGeneratorUtil = passwordGeneratorUtil;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        breachTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        duplicatesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        weakTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        setupBreachTable();
        setupDuplicatesTable();
        setupWeakTable();
    }

    // ═══════════════════════════════════════════════════
    //  SETUP DE TABLAS
    // ═══════════════════════════════════════════════════

    private void setupBreachTable() {
        breachStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isBreached() ? "⚠️" : "✅"));
        breachStatusColumn.setStyle("-fx-alignment: CENTER;");

        breachTitleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEntry().getTitle()));

        breachUsernameColumn.setCellValueFactory(data -> {
            String u = data.getValue().getEntry().getUsername();
            return new SimpleStringProperty(u != null && !u.isEmpty() ? u : "—");
        });

        breachSeverityColumn.setCellValueFactory(data -> {
            BreachResult r = data.getValue();
            return new SimpleStringProperty(r.getError() != null ? "Error" : r.getSeverity().getLabel());
        });

        // Celda de severidad con color según nivel
        breachSeverityColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Segura"         -> setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                    case "Riesgo Bajo"    -> setStyle("-fx-text-fill: #eab308; -fx-font-weight: bold;");
                    case "Riesgo Medio"   -> setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold;");
                    case "Riesgo Alto"    -> setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    case "Riesgo Crítico" -> setStyle("-fx-text-fill: #991b1b; -fx-font-weight: bold;");
                    default               -> setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic;");
                }
            }
        });

        breachOccurrencesColumn.setCellValueFactory(data -> {
            BreachResult r = data.getValue();
            if (r.getError() != null)  return new SimpleStringProperty("—");
            if (!r.isBreached())       return new SimpleStringProperty("0");
            return new SimpleStringProperty(String.format("%,d", r.getOccurrences()));
        });
        breachOccurrencesColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        breachRecommendationColumn.setCellValueFactory(data -> {
            BreachResult r = data.getValue();
            if (r.getError() != null)  return new SimpleStringProperty("No se pudo verificar");
            if (!r.isBreached())       return new SimpleStringProperty("Mantener contraseña");
            String rec = switch (r.getSeverity()) {
                case CRITICAL -> "⛔ CAMBIAR INMEDIATAMENTE";
                case HIGH     -> "⚠️ Cambiar urgentemente";
                case MEDIUM   -> "⚠️ Cambiar pronto";
                case LOW      -> "ℹ️ Considerar cambio";
                default       -> "Revisar";
            };
            return new SimpleStringProperty(rec);
        });

        breachTable.setItems(breachResults);
    }

    private void setupDuplicatesTable() {
        dupTitleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEntry().getTitle()));

        dupUsernameColumn.setCellValueFactory(data -> {
            String u = data.getValue().getEntry().getUsername();
            return new SimpleStringProperty(u != null && !u.isEmpty() ? u : "—");
        });

        dupEmailColumn.setCellValueFactory(data -> {
            String e = data.getValue().getEntry().getEmail();
            return new SimpleStringProperty(e != null && !e.isEmpty() ? e : "—");
        });

        dupGroupColumn.setCellValueFactory(data ->
                new SimpleStringProperty("Grupo " + data.getValue().getGroup()));
        dupGroupColumn.setStyle("-fx-alignment: CENTER;");

        // Filas coloreadas por grupo para agrupar visualmente
        duplicatesTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(DuplicateResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setStyle(""); return; }
                String[] colors = {
                    "rgba(239, 68, 68, 0.1)",
                    "rgba(249, 115, 22, 0.1)",
                    "rgba(234, 179, 8, 0.1)",
                    "rgba(34, 197, 94, 0.1)",
                    "rgba(59, 130, 246, 0.1)"
                };
                setStyle("-fx-background-color: " + colors[(item.getGroup() - 1) % colors.length] + ";");
            }
        });

        duplicatesTable.setItems(duplicateResults);
    }

    private void setupWeakTable() {
        weakTitleColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEntry().getTitle()));

        weakUsernameColumn.setCellValueFactory(data -> {
            String u = data.getValue().getEntry().getUsername();
            return new SimpleStringProperty(u != null && !u.isEmpty() ? u : "—");
        });

        weakStrengthColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStrength().getLabel()));

        // Celda de fortaleza con color según nivel
        weakStrengthColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("Muy débil".equals(item)) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else if ("Débil".equals(item)) {
                    setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold;");
                }
            }
        });

        weakLengthColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getEntry().getPassword().length())));
        weakLengthColumn.setStyle("-fx-alignment: CENTER;");

        weakTable.setItems(weakResults);
    }

    // ═══════════════════════════════════════════════════
    //  ENTRY POINT & ANÁLISIS
    // ═══════════════════════════════════════════════════

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        dialogStage.setOnCloseRequest(event -> {
            if (breachTask != null && breachTask.isRunning()) {
                breachTask.cancel();
            }
        });
    }

    /**
     * Punto de entrada llamado desde MainController tras abrir el diálogo.
     * Ejecuta duplicados y débiles de forma inmediata (local),
     * y lanza la verificación de brechas en segundo plano (API).
     */
    public void startVerification() {
        List<PasswordEntryDTO> allPasswords = passwordEntryService.findAll();

        if (allPasswords.isEmpty()) {
            progressContainer.setVisible(true);
            progressLabel.setText("No hay contraseñas para analizar");
            return;
        }

        totalLabel.setText(String.valueOf(allPasswords.size()));

        // Análisis local — inmediato
        analyzeDuplicates(allPasswords);
        analyzeWeakPasswords(allPasswords);

        // Verificación de brechas — asíncrona (necesita API)
        startBreachCheck(allPasswords);
    }

    private void analyzeDuplicates(List<PasswordEntryDTO> passwords) {
        Map<String, List<PasswordEntryDTO>> groups = passwords.stream()
                .filter(p -> p.getPassword() != null && !p.getPassword().isEmpty())
                .collect(Collectors.groupingBy(PasswordEntryDTO::getPassword));

        int groupNumber = 1;
        for (List<PasswordEntryDTO> group : groups.values()) {
            if (group.size() > 1) {
                int gn = groupNumber;
                group.forEach(entry -> duplicateResults.add(new DuplicateResult(entry, gn)));
                groupNumber++;
            }
        }

        // Ordenar por grupo para que aparezcan juntas visualmente
        duplicateResults.sort(Comparator.comparingInt(DuplicateResult::getGroup));
        duplicatesLabel.setText(String.valueOf(duplicateResults.size()));
    }

    private void analyzeWeakPasswords(List<PasswordEntryDTO> passwords) {
        for (PasswordEntryDTO entry : passwords) {
            if (entry.getPassword() == null || entry.getPassword().isEmpty()) continue;

            PasswordStrength strength = passwordGeneratorUtil.evaluateStrength(entry.getPassword());
            if (strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK) {
                weakResults.add(new WeakResult(entry, strength));
            }
        }
        weakLabel.setText(String.valueOf(weakResults.size()));
    }

    // ═══════════════════════════════════════════════════
    //  VERIFICACIÓN DE BRECHAS (asíncrona)
    // ═══════════════════════════════════════════════════

    private void startBreachCheck(List<PasswordEntryDTO> allPasswords) {
        progressContainer.setVisible(true);

        breachTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int total = allPasswords.size();
                int processed = 0;
                int breachedCount = 0;

                for (PasswordEntryDTO entry : allPasswords) {
                    if (isCancelled()) break;

                    updateMessage("Verificando: " + entry.getTitle() + " (" + (processed + 1) + "/" + total + ")");

                    try {
                        PasswordBreachService.BreachCheckResult result =
                                passwordBreachService.checkPassword(entry.getPassword());

                        BreachResult breachResult = new BreachResult(
                                entry,
                                result.isBreached(),
                                result.getOccurrences(),
                                result.getSeverityLevel(),
                                null
                        );

                        Platform.runLater(() -> breachResults.add(breachResult));

                        if (result.isBreached()) breachedCount++;

                        Thread.sleep(100); // respetar rate-limit de HIBP

                    } catch (PasswordBreachService.PasswordBreachCheckException e) {
                        BreachResult errorResult = new BreachResult(
                                entry, false, 0,
                                PasswordBreachService.SeverityLevel.SAFE, e.getMessage());
                        Platform.runLater(() -> breachResults.add(errorResult));
                    }

                    processed++;
                    updateProgress(processed, total);

                    final int count = breachedCount;
                    Platform.runLater(() -> breachedLabel.setText(String.valueOf(count)));
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(breachTask.progressProperty());
        progressLabel.textProperty().bind(breachTask.messageProperty());

        breachTask.setOnSucceeded(e -> onBreachCheckComplete());
        breachTask.setOnCancelled(e -> onBreachCheckComplete());
        breachTask.setOnFailed(e -> {
            onBreachCheckComplete();
            progressLabel.setText("Error: " + breachTask.getException().getMessage());
        });

        Thread thread = new Thread(breachTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void onBreachCheckComplete() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        progressBar.setProgress(1.0);
        progressLabel.setText("Análisis completo");
    }

    @FXML
    private void handleClose() {
        if (breachTask != null && breachTask.isRunning()) {
            breachTask.cancel();
        }
        dialogStage.close();
    }

    // ═══════════════════════════════════════════════════
    //  CLASES INTERNAS DE RESULTADO
    // ═══════════════════════════════════════════════════

    public static class BreachResult {
        private final PasswordEntryDTO entry;
        private final boolean breached;
        private final int occurrences;
        private final PasswordBreachService.SeverityLevel severity;
        private final String error;

        public BreachResult(PasswordEntryDTO entry, boolean breached, int occurrences,
                            PasswordBreachService.SeverityLevel severity, String error) {
            this.entry = entry;
            this.breached = breached;
            this.occurrences = occurrences;
            this.severity = severity;
            this.error = error;
        }

        public PasswordEntryDTO getEntry()                          { return entry; }
        public boolean isBreached()                                 { return breached; }
        public int getOccurrences()                                 { return occurrences; }
        public PasswordBreachService.SeverityLevel getSeverity()    { return severity; }
        public String getError()                                    { return error; }
    }

    public static class DuplicateResult {
        private final PasswordEntryDTO entry;
        private final int group;

        public DuplicateResult(PasswordEntryDTO entry, int group) {
            this.entry = entry;
            this.group = group;
        }

        public PasswordEntryDTO getEntry() { return entry; }
        public int getGroup()              { return group; }
    }

    public static class WeakResult {
        private final PasswordEntryDTO entry;
        private final PasswordStrength strength;

        public WeakResult(PasswordEntryDTO entry, PasswordStrength strength) {
            this.entry = entry;
            this.strength = strength;
        }

        public PasswordEntryDTO getEntry()    { return entry; }
        public PasswordStrength getStrength() { return strength; }
    }
}
