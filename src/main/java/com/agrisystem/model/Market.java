package com.agrisystem.model;

import java.util.HashMap;
import java.util.Map;

public class Market {
    private String id;
    private String name;
    private String districtId;

    public Market() {}

    public Market(String id, String name, String districtId) {
        this.id = id;
        this.name = name;
        this.districtId = districtId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDistrictId() { return districtId; }
    public void setDistrictId(String districtId) { this.districtId = districtId; }

    public String toCsv() {
        return String.join(",", id, name, districtId != null ? districtId : "");
    }

    public static Market fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 3) return null;
        return new Market(p[0], p[1], p[2]);
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }
}
