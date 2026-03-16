package com.agrisystem.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Farmer extends User {
    /** Primary / most recent cooperative. */
    private String coopId;
    private List<String> cropIds;
    private String districtId;
    private String marketId;
    private double balance;
    /** Loan responsibility score 0–100. Increases on timely repayment, decreases on late. */
    private double responsibilityScore;

    public Farmer() {
        this.type = "FARMER";
        this.cropIds = new ArrayList<>();
        this.balance = 5000.0;
        this.responsibilityScore = 50.0;
    }

    public Farmer(String id, String name, String email, String number, String password,
                  String coopId, List<String> cropIds, String districtId, String marketId,
                  double balance, double responsibilityScore) {
        super(id, name, email, number, password, "FARMER");
        this.coopId = coopId;
        this.cropIds = cropIds != null ? cropIds : new ArrayList<>();
        this.districtId = districtId;
        this.marketId = marketId;
        this.balance = balance;
        this.responsibilityScore = responsibilityScore;
    }

    public String getCoopId() { return coopId; }
    public void setCoopId(String coopId) { this.coopId = coopId; }
    public List<String> getCropIds() { return cropIds; }
    public void setCropIds(List<String> cropIds) { this.cropIds = cropIds; }
    public String getDistrictId() { return districtId; }
    public void setDistrictId(String districtId) { this.districtId = districtId; }
    public String getMarketId() { return marketId; }
    public void setMarketId(String marketId) { this.marketId = marketId; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public double getResponsibilityScore() { return responsibilityScore; }
    public void setResponsibilityScore(double score) {
        this.responsibilityScore = Math.max(0, Math.min(100, score));
    }

    @Override
    public String toCsv() {
        String crops = String.join(";", cropIds);
        // Columns: id,name,email,number,password,type,coopId,cropIds,districtId,marketId,balance,responsibilityScore
        return String.join(",", id, name, email, number, password, type,
                coopId != null ? coopId : "",
                crops,
                districtId != null ? districtId : "",
                marketId != null ? marketId : "",
                String.valueOf(balance),
                String.valueOf(responsibilityScore));
    }

    public static Farmer fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 11) return null;
        List<String> crops = p[7].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[7].split(";")));
        double bal = 5000.0, score = 50.0;
        try { bal = Double.parseDouble(p[10]); } catch (NumberFormatException ignored) {}
        if (p.length >= 12) { try { score = Double.parseDouble(p[11]); } catch (NumberFormatException ignored) {} }
        return new Farmer(p[0], p[1], p[2], p[3], p[4], p[6], crops, p[8], p[9], bal, score);
    }
}
