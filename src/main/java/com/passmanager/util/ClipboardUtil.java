package com.passmanager.util;

import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ClipboardUtil {

    private static final int CLIPBOARD_CLEAR_DELAY_SECONDS = 30;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "clipboard-cleaner");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> pendingClear;
    private String lastCopiedText;

    public void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    public void copyToClipboardWithAutoClear(String text) {
        copyToClipboard(text);
        lastCopiedText = text;
        scheduleClear();
    }

    private void scheduleClear() {
        if (pendingClear != null && !pendingClear.isDone()) {
            pendingClear.cancel(false);
        }

        pendingClear = scheduler.schedule(() -> {
            Platform.runLater(this::clearIfUnchanged);
        }, CLIPBOARD_CLEAR_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void clearIfUnchanged() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String currentContent = clipboard.getString();
            if (currentContent != null && currentContent.equals(lastCopiedText)) {
                ClipboardContent empty = new ClipboardContent();
                empty.putString("");
                clipboard.setContent(empty);
            }
        }
        lastCopiedText = null;
    }

    public String getFromClipboard() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        return clipboard.hasString() ? clipboard.getString() : "";
    }

    public int getClearDelaySeconds() {
        return CLIPBOARD_CLEAR_DELAY_SECONDS;
    }
}
