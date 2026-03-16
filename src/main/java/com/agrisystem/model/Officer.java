package com.agrisystem.model;

public class Officer extends User {
    private String coopId;

    public Officer() {
        this.type = "OFFICER";
    }

    public Officer(String id, String name, String email, String number, String password, String coopId) {
        super(id, name, email, number, password, "OFFICER");
        this.coopId = coopId;
    }

    public String getCoopId() { return coopId; }
    public void setCoopId(String coopId) { this.coopId = coopId; }

    @Override
    public String toCsv() {
        return String.join(",", id, name, email, number, password, type,
                coopId != null ? coopId : "",
                ""); // keep column 7 empty for backward CSV compatibility
    }

    public static Officer fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 7) return null;
        return new Officer(p[0], p[1], p[2], p[3], p[4], p[6]);
    }
}
