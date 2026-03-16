package com.agrisystem.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class District {
    private String id;
    private String name;
    private List<String> coopIds;
    private List<String> marketIds;

    public District() {
        coopIds = new ArrayList<>();
        marketIds = new ArrayList<>();
    }

    public District(String id, String name, List<String> coopIds, List<String> marketIds) {
        this.id = id;
        this.name = name;
        this.coopIds = coopIds != null ? coopIds : new ArrayList<>();
        this.marketIds = marketIds != null ? marketIds : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getCoopIds() { return coopIds; }
    public void setCoopIds(List<String> coopIds) { this.coopIds = coopIds; }
    public List<String> getMarketIds() { return marketIds; }
    public void setMarketIds(List<String> marketIds) { this.marketIds = marketIds; }

    public String toCsv() {
        return String.join(",", id, name,
                String.join(";", coopIds),
                String.join(";", marketIds));
    }

    public static District fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 4) return null;
        List<String> coops = p[2].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[2].split(";")));
        List<String> markets = p[3].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[3].split(";")));
        return new District(p[0], p[1], coops, markets);
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }
}
