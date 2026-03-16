package com.agrisystem.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Field {
    private String id;
    private String farmerId;
    private List<String> cropIds;
    private double area;
    private String imagePath;

    public Field() {
        cropIds = new ArrayList<>();
    }

    public Field(String id, String farmerId, List<String> cropIds, double area, String imagePath) {
        this.id = id;
        this.farmerId = farmerId;
        this.cropIds = cropIds != null ? cropIds : new ArrayList<>();
        this.area = area;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public List<String> getCropIds() { return cropIds; }
    public void setCropIds(List<String> cropIds) { this.cropIds = cropIds; }
    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String toCsv() {
        return String.join(",", id, farmerId,
                String.join(";", cropIds),
                String.valueOf(area),
                imagePath != null ? imagePath : "");
    }

    public static Field fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 5) return null;
        List<String> crops = p[2].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[2].split(";")));
        double area = 0;
        try { area = Double.parseDouble(p[3]); } catch (NumberFormatException ignored) {}
        return new Field(p[0], p[1], crops, area, p[4]);
    }
}
