package com.agrisystem.model;

public class User {
    protected String id;
    protected String name;
    protected String email;
    protected String number;
    protected String password;
    protected String type; // ADMIN, FARMER, OFFICER

    public User() {}

    public User(String id, String name, String email, String number, String password, String type) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.number = number;
        this.password = password;
        this.type = type;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String toCsv() {
        return String.join(",", id, name, email, number, password, type);
    }

    public static User fromCsv(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) return null;
        return new User(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', name='" + name + "', type='" + type + "'}";
    }
}
