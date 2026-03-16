package com.agrisystem.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Cooperative {
    private String id;
    private String name;
    private String officerId;
    private List<String> memberIds;
    private String districtId;
    private List<String> marketIds;
    private List<String> loanIds;

    public Cooperative() {
        memberIds = new ArrayList<>();
        marketIds = new ArrayList<>();
        loanIds = new ArrayList<>();
    }

    public Cooperative(String id, String name, String officerId, List<String> memberIds,
                       String districtId, List<String> marketIds, List<String> loanIds) {
        this.id = id;
        this.name = name;
        this.officerId = officerId;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
        this.districtId = districtId;
        this.marketIds = marketIds != null ? marketIds : new ArrayList<>();
        this.loanIds = loanIds != null ? loanIds : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOfficerId() { return officerId; }
    public void setOfficerId(String officerId) { this.officerId = officerId; }
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    public String getDistrictId() { return districtId; }
    public void setDistrictId(String districtId) { this.districtId = districtId; }
    public List<String> getMarketIds() { return marketIds; }
    public void setMarketIds(List<String> marketIds) { this.marketIds = marketIds; }
    public List<String> getLoanIds() { return loanIds; }
    public void setLoanIds(List<String> loanIds) { this.loanIds = loanIds; }

    public String toCsv() {
        return String.join(",", id, name,
                officerId != null ? officerId : "",
                String.join(";", memberIds),
                districtId != null ? districtId : "",
                String.join(";", marketIds),
                String.join(";", loanIds));
    }

    public static Cooperative fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 7) return null;
        List<String> members = p[3].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[3].split(";")));
        List<String> markets = p[5].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[5].split(";")));
        List<String> loans = p[6].isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(p[6].split(";")));
        return new Cooperative(p[0], p[1], p[2], members, p[4], markets, loans);
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }
}
