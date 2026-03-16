package com.agrisystem.controller.admin;

import com.agrisystem.service.AuditLogService;
import com.agrisystem.service.AuditLogService.AuditEntry;
import com.agrisystem.service.DataService;
import com.agrisystem.util.AlertUtil;
import com.agrisystem.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminBackupController {

    @FXML private Label backupStatusLabel;
    @FXML private ListView<String> backupListView;

    @FXML private TableView<AuditEntry>              auditTable;
    @FXML private TableColumn<AuditEntry, String>    colAuditTime;
    @FXML private TableColumn<AuditEntry, String>    colAuditUser;
    @FXML private TableColumn<AuditEntry, String>    colAuditAction;
    @FXML private TableColumn<AuditEntry, String>    colAuditDetail;

    private static final String DATA_DIR   = "data/";
    private static final String BACKUP_DIR = "backups/";

    @FXML
    public void initialize() {
        setupAuditTable();
        refreshBackupList();
        refreshAuditLog();
    }

    // ── Backup ────────────────────────────────────────────────────────────────

    @FXML
    private void createBackup() {
        new File(BACKUP_DIR).mkdirs();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFolder = BACKUP_DIR + "backup_" + timestamp;
        File dest = new File(backupFolder);
        dest.mkdirs();

        try {
            File dataDir = new File(DATA_DIR);
            File[] csvFiles = dataDir.listFiles((d, name) -> name.endsWith(".csv"));
            if (csvFiles != null) {
                for (File src : csvFiles) {
                    Files.copy(src.toPath(),
                            new File(dest, src.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            var user = SessionManager.getInstance().getCurrentUser();
            AuditLogService.getInstance().log(user.getId(), user.getName(),
                    "BACKUP_CREATED", "Created backup: backup_" + timestamp);
            refreshBackupList();
            backupStatusLabel.setText("✅ Backup created: backup_" + timestamp);
            backupStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } catch (IOException e) {
            backupStatusLabel.setText("❌ Backup failed: " + e.getMessage());
            backupStatusLabel.setStyle("-fx-text-fill: #F44336;");
        }
    }

    @FXML
    private void restoreBackup() {
        String selected = backupListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("No Selection", "Please select a backup from the list to restore.");
            return;
        }

        if (!AlertUtil.showConfirm(
                "Confirm Restore",
                "Restore backup: " + selected + "?\n\nThis will overwrite all current data. This cannot be undone.")) return;

        File backupFolder = new File(BACKUP_DIR + selected);
        if (!backupFolder.exists()) {
            AlertUtil.showError("Not Found", "Backup folder not found: " + backupFolder.getAbsolutePath());
            return;
        }

        try {
            File[] csvFiles = backupFolder.listFiles((d, name) -> name.endsWith(".csv"));
            if (csvFiles != null) {
                for (File src : csvFiles) {
                    Files.copy(src.toPath(),
                            new File(DATA_DIR + src.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            // Reload all data in memory
            DataService.getInstance().reloadAll();

            var user = SessionManager.getInstance().getCurrentUser();
            AuditLogService.getInstance().log(user.getId(), user.getName(),
                    "BACKUP_RESTORED", "Restored backup: " + selected);
            refreshAuditLog();

            backupStatusLabel.setText("✅ Restored: " + selected);
            backupStatusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            AlertUtil.showInfo("Restore Successful", "Data has been restored from: " + selected);
        } catch (IOException e) {
            AlertUtil.showError("Restore Failed", "Could not restore backup: " + e.getMessage());
        }
    }

    private void refreshBackupList() {
        File dir = new File(BACKUP_DIR);
        List<String> backups = new ArrayList<>();
        if (dir.exists()) {
            File[] folders = dir.listFiles(File::isDirectory);
            if (folders != null) {
                Arrays.sort(folders, Comparator.comparingLong(File::lastModified).reversed());
                for (File f : folders) backups.add(f.getName());
            }
        }
        backupListView.setItems(FXCollections.observableArrayList(backups));
        if (backups.isEmpty()) {
            backupStatusLabel.setText("No backups found. Create one to get started.");
            backupStatusLabel.setStyle("-fx-text-fill: #888;");
        }
    }

    // ── Audit Log ─────────────────────────────────────────────────────────────

    private void setupAuditTable() {
        colAuditTime.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().timestamp()));
        colAuditUser.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().userName() + " (" + c.getValue().userId() + ")"));
        colAuditAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().action()));
        colAuditDetail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().detail()));
    }

    private void refreshAuditLog() {
        List<AuditEntry> entries = AuditLogService.getInstance().readAllEntries();
        auditTable.setItems(FXCollections.observableArrayList(entries));
    }

    @FXML
    private void refreshAuditLogAction() {
        refreshAuditLog();
    }
}
