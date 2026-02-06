package com.passmanager.service;

import javafx.application.Platform;
import javafx.scene.Scene;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

@Service
public class ThemeService {

    private static final String PREFS_DIR = System.getProperty("user.home") + "/.passmanager";
    private static final String PREFS_FILE = PREFS_DIR + "/theme.properties";
    private static final String BASE_CSS = "/css/styles.css";

    private Theme currentTheme;
    private final Set<Scene> trackedScenes = Collections.newSetFromMap(new WeakHashMap<>());

    public enum Theme {
        LIGHT("Claro", null),
        DARK("Oscuro", "/css/themes/dark.css");

        private final String displayName;
        private final String cssPath;

        Theme(String displayName, String cssPath) {
            this.displayName = displayName;
            this.cssPath = cssPath;
        }

        public String getDisplayName() { return displayName; }
        public String getCssPath()     { return cssPath; }
    }

    public ThemeService() {
        currentTheme = loadThemeFromPrefs();
    }

    public Theme getCurrentTheme() { return currentTheme; }

    public void setCurrentTheme(Theme theme) {
        this.currentTheme = theme;
        saveThemeToPrefs();
        applyToAllScenes();
    }

    public List<String> getStylesheets() {
        List<String> stylesheets = new ArrayList<>();
        URL baseUrl = ThemeService.class.getResource(BASE_CSS);
        if (baseUrl != null) {
            stylesheets.add(baseUrl.toExternalForm());
        }
        if (currentTheme.getCssPath() != null) {
            URL themeUrl = ThemeService.class.getResource(currentTheme.getCssPath());
            if (themeUrl != null) {
                stylesheets.add(themeUrl.toExternalForm());
            }
        }
        return stylesheets;
    }

    public void applyToScene(Scene scene) {
        scene.getStylesheets().setAll(getStylesheets());
        trackedScenes.add(scene);
    }

    private void applyToAllScenes() {
        Platform.runLater(() -> {
            List<String> stylesheets = getStylesheets();
            for (Scene scene : new ArrayList<>(trackedScenes)) {
                scene.getStylesheets().setAll(stylesheets);
            }
        });
    }

    private Theme loadThemeFromPrefs() {
        try {
            Path path = Paths.get(PREFS_FILE);
            if (Files.exists(path)) {
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(path)) {
                    props.load(is);
                }
                return Theme.valueOf(props.getProperty("theme", "LIGHT"));
            }
        } catch (Exception e) {
            // valor inválido o error de IO — usar claro por defecto
        }
        return Theme.LIGHT;
    }

    private void saveThemeToPrefs() {
        try {
            Files.createDirectories(Paths.get(PREFS_DIR));
            Properties props = new Properties();
            props.setProperty("theme", currentTheme.name());
            try (OutputStream os = Files.newOutputStream(Paths.get(PREFS_FILE))) {
                props.store(os, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
