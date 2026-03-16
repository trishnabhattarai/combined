package com.agrisystem.model;

public class Weather {
    private String date;
    private double temperature;
    private String condition;
    private double humidity;

    public Weather() {}

    public Weather(String date, double temperature, String condition, double humidity) {
        this.date = date;
        this.temperature = temperature;
        this.condition = condition;
        this.humidity = humidity;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public String toCsv() {
        return String.join(",", date, String.valueOf(temperature), condition, String.valueOf(humidity));
    }

    public static Weather fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 4) return null;
        double temp = 0, hum = 0;
        try { temp = Double.parseDouble(p[1]); } catch (NumberFormatException ignored) {}
        try { hum = Double.parseDouble(p[3]); } catch (NumberFormatException ignored) {}
        return new Weather(p[0], temp, p[2], hum);
    }
}
