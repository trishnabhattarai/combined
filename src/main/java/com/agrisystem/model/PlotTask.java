package com.agrisystem.model;

/**
 * Represents a farming task assigned to a specific plot within a field.
 * Tasks follow a Kanban-style flow: TODO → DOING → DONE
 *
 * CSV format: id,fieldId,plotNumber,title,description,status
 */
public class PlotTask {

    public enum Status { TODO, DOING, DONE }

    private String id;
    private String fieldId;
    private int    plotNumber;   // 1-based plot number within the field
    private String title;
    private String description;
    private Status status;

    public PlotTask() {}

    public PlotTask(String id, String fieldId, int plotNumber,
                    String title, String description, Status status) {
        this.id          = id;
        this.fieldId     = fieldId;
        this.plotNumber  = plotNumber;
        this.title       = title;
        this.description = description;
        this.status      = status;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()              { return id; }
    public void   setId(String id)     { this.id = id; }

    public String getFieldId()                 { return fieldId; }
    public void   setFieldId(String fieldId)   { this.fieldId = fieldId; }

    public int  getPlotNumber()              { return plotNumber; }
    public void setPlotNumber(int plotNumber){ this.plotNumber = plotNumber; }

    public String getTitle()               { return title; }
    public void   setTitle(String title)   { this.title = title; }

    public String getDescription()                   { return description; }
    public void   setDescription(String description) { this.description = description; }

    public Status getStatus()               { return status; }
    public void   setStatus(Status status)  { this.status = status; }

    // ── Advance status: TODO → DOING → DONE ──────────────────────────────────

    /** Moves this task one step forward in the workflow. */
    public void advance() {
        if (status == Status.TODO)   status = Status.DOING;
        else if (status == Status.DOING) status = Status.DONE;
        // DONE tasks stay DONE
    }

    // ── CSV Serialization ─────────────────────────────────────────────────────

    public String toCsv() {
        return String.join(",",
                id,
                fieldId,
                String.valueOf(plotNumber),
                escape(title),
                escape(description),
                status.name());
    }

    public static PlotTask fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 6) return null;
        try {
            int    plot   = Integer.parseInt(p[2].trim());
            Status status = Status.valueOf(p[5].trim().toUpperCase());
            return new PlotTask(p[0].trim(), p[1].trim(), plot,
                                unescape(p[3]), unescape(p[4]), status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace(",", "；").replace("\n", " ");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("；", ",");
    }

    @Override
    public String toString() { return title + " [" + status + "]"; }
}
