package com.agrisystem.model;

/**
 * Tracks how much of each crop a farmer holds.
 * Production: 10 kg/hour per crop held, starting with 50 kg.
 * Format: farmerId,cropId,quantity,lastProductionTime
 */
public class CropInventory {
    private String farmerId;
    private String cropId;
    private double quantity;
    private long lastProductionTime; // epoch millis when last production was calculated

    public CropInventory() {}

    public CropInventory(String farmerId, String cropId, double quantity, long lastProductionTime) {
        this.farmerId = farmerId;
        this.cropId = cropId;
        this.quantity = quantity;
        this.lastProductionTime = lastProductionTime;
    }

    // ─── Production logic ──────────────────────────────────────────────────────

    /**
     * Calculates and applies accrued production since lastProductionTime.
     * Rate: 10 kg per hour. Updates quantity and lastProductionTime in place.
     */
    public void applyProduction() {
        long now = System.currentTimeMillis();
        double hoursElapsed = (now - lastProductionTime) / 3_600_000.0;
        double produced = hoursElapsed * 10.0;
        this.quantity += produced;
        this.lastProductionTime = now;
    }

    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public long getLastProductionTime() { return lastProductionTime; }
    public void setLastProductionTime(long lastProductionTime) { this.lastProductionTime = lastProductionTime; }

    public String toCsv() {
        return String.join(",", farmerId, cropId,
                String.valueOf(quantity),
                String.valueOf(lastProductionTime));
    }

    public static CropInventory fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 4) return null;
        double qty = 0; long lpt = 0;
        try { qty = Double.parseDouble(p[2]); } catch (NumberFormatException ignored) {}
        try { lpt = Long.parseLong(p[3]); } catch (NumberFormatException ignored) {}
        return new CropInventory(p[0], p[1], qty, lpt);
    }
}
