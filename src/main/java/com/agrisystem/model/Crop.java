package com.agrisystem.model;

public class Crop {
    private String id;
    private String name;

    public Crop() {}
    public Crop(String id, String name) { this.id = id; this.name = name; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String toCsv() { return id + "," + name; }

    public static Crop fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 2) return null;
        return new Crop(p[0], p[1]);
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }
}
