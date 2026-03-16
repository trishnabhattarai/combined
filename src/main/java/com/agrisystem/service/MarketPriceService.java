package com.agrisystem.service;

import java.io.*;
import java.util.*;

/**
 * Singleton service that manages market crop prices.
 *
 * Prices are generated once per 24-hour window per (marketId, cropId) pair
 * and persisted to {@code data/market_prices.csv} so officers and farmers
 * always see the same price regardless of how many times they open the page.
 *
 * CSV format: marketId,cropId,price,lastUpdatedTimestamp
 */
public class MarketPriceService {

    private static MarketPriceService instance;

    private static final String PRICES_FILE    = "data/market_prices.csv";
    private static final long   REFRESH_MILLIS = 24L * 60 * 60 * 1000; // 24 hours
    private static final double PRICE_MIN      = 50.0;
    private static final double PRICE_RANGE    = 950.0;

    /** key = "marketId_cropId", value = PriceEntry */
    private final Map<String, PriceEntry> cache = new HashMap<>();
    private final Random random = new Random();

    private MarketPriceService() {
        new File("data").mkdirs();
        load();
    }

    public static MarketPriceService getInstance() {
        if (instance == null) instance = new MarketPriceService();
        return instance;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the current price for the given market/crop pair.
     * If no price exists, or the stored price is older than 24 hours,
     * a new price is generated and saved immediately.
     */
    public double getPrice(String marketId, String cropId) {
        String key = marketId + "_" + cropId;
        PriceEntry entry = cache.get(key);
        long now = System.currentTimeMillis();

        if (entry == null || (now - entry.timestamp) >= REFRESH_MILLIS) {
            double newPrice = PRICE_MIN + random.nextDouble() * PRICE_RANGE;
            // Round to 2 dp
            newPrice = Math.round(newPrice * 100.0) / 100.0;
            entry = new PriceEntry(marketId, cropId, newPrice, now);
            cache.put(key, entry);
            save();
        }
        return entry.price;
    }

    /**
     * Returns all persisted price entries — used by officers to view
     * historical/current market prices.
     */
    public List<PriceEntry> getAllPrices() {
        return new ArrayList<>(cache.values());
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void load() {
        File file = new File(PRICES_FILE);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",", -1);
                if (p.length < 4) continue;
                try {
                    String marketId = p[0];
                    String cropId   = p[1];
                    double price    = Double.parseDouble(p[2]);
                    long   ts       = Long.parseLong(p[3]);
                    String key = marketId + "_" + cropId;
                    cache.put(key, new PriceEntry(marketId, cropId, price, ts));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(PRICES_FILE))) {
            for (PriceEntry e : cache.values()) {
                bw.write(e.marketId + "," + e.cropId + "," + e.price + "," + e.timestamp);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Price Entry ───────────────────────────────────────────────────────────

    public static class PriceEntry {
        public final String marketId;
        public final String cropId;
        public final double price;
        public final long   timestamp;

        public PriceEntry(String marketId, String cropId, double price, long timestamp) {
            this.marketId  = marketId;
            this.cropId    = cropId;
            this.price     = price;
            this.timestamp = timestamp;
        }

        /** Milliseconds until this price expires (negative if already expired). */
        public long millisUntilRefresh() {
            return (timestamp + 24L * 60 * 60 * 1000) - System.currentTimeMillis();
        }
    }
}
