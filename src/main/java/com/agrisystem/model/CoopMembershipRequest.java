package com.agrisystem.model;

/**
 * Represents a farmer's request to join a cooperative.
 * Status: PENDING, APPROVED, REJECTED
 */
public class CoopMembershipRequest {
    private String id;
    private String farmerId;
    private String coopId;
    private String status; // PENDING, APPROVED, REJECTED
    private long requestDate;
    private String note;

    public CoopMembershipRequest() {}

    public CoopMembershipRequest(String id, String farmerId, String coopId,
                                  String status, long requestDate, String note) {
        this.id = id;
        this.farmerId = farmerId;
        this.coopId = coopId;
        this.status = status;
        this.requestDate = requestDate;
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public String getCoopId() { return coopId; }
    public void setCoopId(String coopId) { this.coopId = coopId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getRequestDate() { return requestDate; }
    public void setRequestDate(long requestDate) { this.requestDate = requestDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String toCsv() {
        return String.join(",", id, farmerId, coopId, status,
                String.valueOf(requestDate),
                note != null ? note.replace(",", ";") : "");
    }

    public static CoopMembershipRequest fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 6) return null;
        long date = 0;
        try { date = Long.parseLong(p[4]); } catch (NumberFormatException ignored) {}
        return new CoopMembershipRequest(p[0], p[1], p[2], p[3], date, p[5]);
    }
}
