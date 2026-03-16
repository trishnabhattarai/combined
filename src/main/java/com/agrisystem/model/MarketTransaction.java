package com.agrisystem.model;

public class MarketTransaction {
    private String id;
    private String userId;
    private String cropId;
    private String marketId;
    private String type; // BUY or SELL
    private double quantity;
    private double pricePerKg;
    private double totalAmount;
    private long timestamp;

    public MarketTransaction() {}

    public MarketTransaction(String id, String userId, String cropId, String marketId,
                              String type, double quantity, double pricePerKg, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.cropId = cropId;
        this.marketId = marketId;
        this.type = type;
        this.quantity = quantity;
        this.pricePerKg = pricePerKg;
        this.totalAmount = quantity * pricePerKg;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }
    public String getMarketId() { return marketId; }
    public void setMarketId(String marketId) { this.marketId = marketId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String toCsv() {
        return String.join(",", id, userId, cropId, marketId, type,
                String.valueOf(quantity), String.valueOf(pricePerKg),
                String.valueOf(totalAmount), String.valueOf(timestamp));
    }

    public static MarketTransaction fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 9) return null;
        double qty = 0, price = 0, total = 0;
        long ts = 0;
        try { qty = Double.parseDouble(p[5]); } catch (NumberFormatException ignored) {}
        try { price = Double.parseDouble(p[6]); } catch (NumberFormatException ignored) {}
        try { total = Double.parseDouble(p[7]); } catch (NumberFormatException ignored) {}
        try { ts = Long.parseLong(p[8]); } catch (NumberFormatException ignored) {}
        MarketTransaction mt = new MarketTransaction(p[0], p[1], p[2], p[3], p[4], qty, price, ts);
        mt.setTotalAmount(total);
        return mt;
    }
}
