package com.agrisystem.model;

public class Admin extends User {

    public Admin() {
        this.type = "ADMIN";
    }

    public Admin(String id, String name, String email, String password) {
        super(id, name, email, "", password, "ADMIN");
    }

    @Override
    public String toCsv() {
        return String.join(",", id, name, email, number, password, type);
    }

    public static Admin fromCsv(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 6) return null;
        Admin a = new Admin(p[0], p[1], p[2], p[4]);
        a.setNumber(p[3]);
        return a;
    }
}
