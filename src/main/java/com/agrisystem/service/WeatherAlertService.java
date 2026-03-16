package com.agrisystem.service;

import com.agrisystem.model.Weather;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton service that generates silent weather alerts based on temperature changes
 * detected in the weather data. Alerts can be marked as read per-session.
 */
public class WeatherAlertService {
    private static WeatherAlertService instance;

    /** Threshold in degrees C to trigger a temperature-change alert. */
    private static final double CHANGE_THRESHOLD = 3.0;
    private static final double HOT_THRESHOLD = 35.0;
    private static final double COLD_THRESHOLD = 10.0;

    private final List<WeatherAlert> alerts = new ArrayList<>();

    private WeatherAlertService() {}

    public static WeatherAlertService getInstance() {
        if (instance == null) instance = new WeatherAlertService();
        return instance;
    }

    /** Generate alerts from a list of Weather records. Replaces existing alerts. */
    public void generateAlerts(List<Weather> weatherList) {
        alerts.clear();
        if (weatherList == null || weatherList.isEmpty()) return;

        // Alert for extreme temperatures
        Weather latest = weatherList.get(weatherList.size() - 1);
        if (latest.getTemperature() >= HOT_THRESHOLD) {
            alerts.add(new WeatherAlert(
                    "🌡️ High Temperature Warning",
                    String.format("Temperature reached %.1f°C on %s. Consider irrigating crops and providing shade.",
                            latest.getTemperature(), latest.getDate())));
        } else if (latest.getTemperature() <= COLD_THRESHOLD) {
            alerts.add(new WeatherAlert(
                    "❄️ Low Temperature Warning",
                    String.format("Temperature dropped to %.1f°C on %s. Protect sensitive crops from frost.",
                            latest.getTemperature(), latest.getDate())));
        }

        // Alert for significant temperature changes between days
        for (int i = 1; i < weatherList.size(); i++) {
            double prev = weatherList.get(i - 1).getTemperature();
            double curr = weatherList.get(i).getTemperature();
            double delta = curr - prev;
            if (Math.abs(delta) >= CHANGE_THRESHOLD) {
                String dir = delta > 0 ? "risen" : "dropped";
                alerts.add(new WeatherAlert(
                        String.format("🌤 Temperature %s sharply", dir),
                        String.format("Temp %s by %.1f°C between %s and %s (%.1f°C → %.1f°C).",
                                dir, Math.abs(delta),
                                weatherList.get(i - 1).getDate(),
                                weatherList.get(i).getDate(),
                                prev, curr)));
            }
        }

        // Keep only latest 10 alerts
        if (alerts.size() > 10) {
            List<WeatherAlert> trimmed = new ArrayList<>(alerts.subList(alerts.size() - 10, alerts.size()));
            alerts.clear();
            alerts.addAll(trimmed);
        }
    }

    public List<WeatherAlert> getAlerts() {
        return alerts;
    }

    public long getUnreadCount() {
        return alerts.stream().filter(a -> !a.isRead()).count();
    }

    /** Mutable alert object with a read flag. */
    public static class WeatherAlert {
        private final String title;
        private final String message;
        private boolean read = false;

        public WeatherAlert(String title, String message) {
            this.title = title;
            this.message = message;
        }

        public String getTitle()   { return title; }
        public String getMessage() { return message; }
        public boolean isRead()    { return read; }
        public void markRead()     { this.read = true; }
    }
}
