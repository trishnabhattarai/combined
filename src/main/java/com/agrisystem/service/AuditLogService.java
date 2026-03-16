package com.agrisystem.service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton service that records user logins and activities to an audit log CSV.
 * Format: timestamp,userId,userName,action,detail
 */
public class AuditLogService {
    private static AuditLogService instance;

    private static final String DATA_DIR = "data/";
    private static final String AUDIT_FILE = DATA_DIR + "audit_log.csv";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogService() {
        new File(DATA_DIR).mkdirs();
    }

    public static AuditLogService getInstance() {
        if (instance == null) instance = new AuditLogService();
        return instance;
    }

    /** Log a user action. */
    public void log(String userId, String userName, String action, String detail) {
        String timestamp = LocalDateTime.now().format(FMT);
        String line = String.join(",",
                timestamp,
                escape(userId),
                escape(userName),
                escape(action),
                escape(detail));
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(AUDIT_FILE, true))) {
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Return all audit entries as raw lines (most recent first). */
    public List<String> readAll() {
        List<String> lines = new ArrayList<>();
        File file = new File(AUDIT_FILE);
        if (!file.exists()) return lines;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(0, line.trim()); // reverse order
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    /** Return all audit entries as structured AuditEntry objects. */
    public List<AuditEntry> readAllEntries() {
        List<AuditEntry> entries = new ArrayList<>();
        File file = new File(AUDIT_FILE);
        if (!file.exists()) return entries;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.trim().split(",", -1);
                if (p.length >= 5) {
                    entries.add(0, new AuditEntry(p[0], p[1], p[2], p[3], p[4]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace(",", ";").replace("\n", " ");
    }

    /** Immutable record for one audit log entry. */
    public record AuditEntry(String timestamp, String userId, String userName, String action, String detail) {}
}
