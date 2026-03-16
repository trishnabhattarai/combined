package com.agrisystem.model;

public class Loan {
    private String id;
    private double amount;
    private String userId;
    private String status; // PENDING, APPROVED, REJECTED, REPAID
    private String coopId;
    private String officerId;
    private long requestDate;
    private long dueDate;

    public Loan() {}

    public Loan(String id, double amount, String userId, String status,
                String coopId, String officerId, long requestDate, long dueDate) {
        this.id = id;
        this.amount = amount;
        this.userId = userId;
        this.status = status;
        this.coopId = coopId;
        this.officerId = officerId;
        this.requestDate = requestDate;
        this.dueDate = dueDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCoopId() { return coopId; }
    public void setCoopId(String coopId) { this.coopId = coopId; }
    public String getOfficerId() { return officerId; }
    public void setOfficerId(String officerId) { this.officerId = officerId; }
    public long getRequestDate() { return requestDate; }
    public void setRequestDate(long requestDate) { this.requestDate = requestDate; }
    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String toCsv() {
        return String.join(",", id, String.valueOf(amount), userId, status,
                coopId != null ? coopId : "",
                officerId != null ? officerId : "",
                String.valueOf(requestDate),
                String.valueOf(dueDate));
    }

    public static Loan fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 8) return null;
        double amt = 0;
        long rd = 0, dd = 0;
        try { amt = Double.parseDouble(p[1]); } catch (NumberFormatException ignored) {}
        try { rd = Long.parseLong(p[6]); } catch (NumberFormatException ignored) {}
        try { dd = Long.parseLong(p[7]); } catch (NumberFormatException ignored) {}
        return new Loan(p[0], amt, p[2], p[3], p[4], p[5], rd, dd);
    }
}
