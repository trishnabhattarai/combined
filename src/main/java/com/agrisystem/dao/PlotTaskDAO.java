package com.agrisystem.dao;

import com.agrisystem.model.PlotTask;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data Access Object for PlotTask persistence.
 * Stores tasks in data/plot_tasks.csv using BufferedReader / BufferedWriter.
 *
 * CSV format: id,fieldId,plotNumber,title,description,status
 */
public class PlotTaskDAO {

    private static PlotTaskDAO instance;
    private static final String FILE_PATH = "data/plot_tasks.csv";

    private PlotTaskDAO() {
        new File("data").mkdirs();
    }

    public static PlotTaskDAO getInstance() {
        if (instance == null) instance = new PlotTaskDAO();
        return instance;
    }

    // ── Read all tasks ────────────────────────────────────────────────────────

    public List<PlotTask> findAll() {
        List<PlotTask> tasks = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return tasks;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                PlotTask t = PlotTask.fromCsv(line);
                if (t != null) tasks.add(t);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    /** Returns all tasks belonging to a specific field. */
    public List<PlotTask> findByField(String fieldId) {
        return findAll().stream()
                .filter(t -> fieldId.equals(t.getFieldId()))
                .collect(Collectors.toList());
    }

    /** Returns all tasks for a specific plot within a field. */
    public List<PlotTask> findByFieldAndPlot(String fieldId, int plotNumber) {
        return findAll().stream()
                .filter(t -> fieldId.equals(t.getFieldId()) && t.getPlotNumber() == plotNumber)
                .collect(Collectors.toList());
    }

    // ── Write helpers ─────────────────────────────────────────────────────────

    /** Saves (adds) a new task. Generates an ID automatically. */
    public PlotTask save(PlotTask task) {
        if (task.getId() == null || task.getId().isBlank()) {
            task.setId("TSK-" + generateShortId());
        }
        List<PlotTask> all = findAll();
        all.add(task);
        writeAll(all);
        return task;
    }

    /** Updates an existing task (matched by ID). */
    public void update(PlotTask updated) {
        List<PlotTask> all = findAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(updated.getId())) {
                all.set(i, updated);
                break;
            }
        }
        writeAll(all);
    }

    /** Deletes a task by ID. */
    public void delete(String taskId) {
        List<PlotTask> all = findAll();
        all.removeIf(t -> t.getId().equals(taskId));
        writeAll(all);
    }

    /** Deletes all tasks belonging to a field (used when a field is deleted). */
    public void deleteByField(String fieldId) {
        List<PlotTask> all = findAll();
        all.removeIf(t -> fieldId.equals(t.getFieldId()));
        writeAll(all);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void writeAll(List<PlotTask> tasks) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (PlotTask t : tasks) {
                bw.write(t.toCsv());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Generates a short unique ID suffix. */
    private String generateShortId() {
        List<PlotTask> all = findAll();
        int max = 0;
        for (PlotTask t : all) {
            try {
                String suffix = t.getId().replace("TSK-", "");
                int num = Integer.parseInt(suffix);
                if (num > max) max = num;
            } catch (NumberFormatException ignored) {}
        }
        return String.format("%03d", max + 1);
    }
}
