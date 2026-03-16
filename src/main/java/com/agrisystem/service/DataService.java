package com.agrisystem.service;

import com.agrisystem.model.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Singleton service that manages all file I/O using BufferedReader/FileReader.
 * Data is persisted as CSV files in the /data directory.
 */
public class DataService {
    private static DataService instance;

    private static final String DATA_DIR = "data/";
    private static final String USERS_FILE = DATA_DIR + "users.csv";
    private static final String LOANS_FILE = DATA_DIR + "loans.csv";
    private static final String FIELDS_FILE = DATA_DIR + "fields.csv";
    private static final String CROPS_FILE = DATA_DIR + "crops.csv";
    private static final String COOPS_FILE = DATA_DIR + "cooperatives.csv";
    private static final String DISTRICTS_FILE = DATA_DIR + "districts.csv";
    private static final String MARKETS_FILE = DATA_DIR + "markets.csv";
    private static final String WEATHER_FILE = DATA_DIR + "weather.csv";
    private static final String TRANSACTIONS_FILE = DATA_DIR + "transactions.csv";
    private static final String INVENTORY_FILE = DATA_DIR + "inventory.csv";
    private static final String MEMBERSHIP_FILE = DATA_DIR + "membership_requests.csv";

    private List<User> users = new ArrayList<>();
    private List<Loan> loans = new ArrayList<>();
    private List<Field> fields = new ArrayList<>();
    private List<Crop> crops = new ArrayList<>();
    private List<Cooperative> cooperatives = new ArrayList<>();
    private List<District> districts = new ArrayList<>();
    private List<Market> markets = new ArrayList<>();
    private List<Weather> weatherData = new ArrayList<>();
    private List<MarketTransaction> transactions = new ArrayList<>();
    private List<CropInventory> inventories = new ArrayList<>();
    private List<CoopMembershipRequest> membershipRequests = new ArrayList<>();

    private DataService() {}

    public static DataService getInstance() {
        if (instance == null) instance = new DataService();
        return instance;
    }

    // ─── INIT ────────────────────────────────────────────────────────────────

    public void initializeData() {
        createDataDir();
        loadAll();
        if (users.isEmpty()) seedData();
    }

    /** Public reload — used after restoring a backup. */
    public void reloadAll() {
        loadAll();
    }

    private void createDataDir() {
        new File(DATA_DIR).mkdirs();
    }

    private void loadAll() {
        users = loadUsers();
        loans = loadLoans();
        fields = loadFields();
        crops = loadCrops();
        cooperatives = loadCooperatives();
        districts = loadDistricts();
        markets = loadMarkets();
        weatherData = loadWeather();
        transactions = loadTransactions();
        inventories = loadInventories();
        membershipRequests = loadMembershipRequests();
    }

    // ─── GENERIC CSV READ/WRITE ──────────────────────────────────────────────

    private List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) return lines;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) lines.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private <T> void writeLines(String path, List<T> items, java.util.function.Function<T, String> toCsv) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            for (T item : items) {
                bw.write(toCsv.apply(item));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── USERS ───────────────────────────────────────────────────────────────

    private List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        for (String line : readLines(USERS_FILE)) {
            String[] p = line.split(",", -1);
            if (p.length < 6) continue;
            String type = p[5];
            User u = switch (type.toUpperCase()) {
                case "ADMIN" -> Admin.fromCsv(line);
                case "FARMER" -> Farmer.fromCsv(line);
                case "OFFICER" -> Officer.fromCsv(line);
                default -> User.fromCsv(line);
            };
            if (u != null) list.add(u);
        }
        return list;
    }

    public void saveUsers() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User u : users) {
                bw.write(u.toCsv());
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public List<User> getAllUsers() { return users; }

    public Optional<User> findUserByEmail(String email) {
        return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    public Optional<User> findUserById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    public void addUser(User user) {
        users.add(user);
        saveUsers();
    }

    public void updateUser(User updated) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updated.getId())) {
                users.set(i, updated);
                break;
            }
        }
        saveUsers();
    }

    public void deleteUser(String userId) {
        users.removeIf(u -> u.getId().equals(userId));
        loans.removeIf(l -> l.getUserId().equals(userId));
        fields.removeIf(f -> f.getFarmerId().equals(userId));
        transactions.removeIf(t -> t.getUserId().equals(userId));
        inventories.removeIf(i -> i.getFarmerId().equals(userId));
        membershipRequests.removeIf(r -> r.getFarmerId().equals(userId));
        for (Cooperative c : cooperatives) c.getMemberIds().remove(userId);
        saveUsers();
        saveLoans();
        saveFields();
        saveTransactions();
        saveInventories();
        saveMembershipRequests();
        saveCooperatives();
    }

    // ─── LOANS ───────────────────────────────────────────────────────────────

    private List<Loan> loadLoans() {
        List<Loan> list = new ArrayList<>();
        for (String line : readLines(LOANS_FILE)) {
            Loan l = Loan.fromCsv(line);
            if (l != null) list.add(l);
        }
        return list;
    }

    public void saveLoans() { writeLines(LOANS_FILE, loans, Loan::toCsv); }
    public List<Loan> getAllLoans() { return loans; }
    public List<Loan> getLoansByUser(String userId) {
        return loans.stream().filter(l -> l.getUserId().equals(userId)).toList();
    }
    public List<Loan> getLoansByCoop(String coopId) {
        return loans.stream().filter(l -> l.getCoopId().equals(coopId)).toList();
    }
    public void addLoan(Loan loan) { loans.add(loan); saveLoans(); }
    public void updateLoan(Loan updated) {
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).getId().equals(updated.getId())) { loans.set(i, updated); break; }
        }
        saveLoans();
    }

    // ─── FIELDS ──────────────────────────────────────────────────────────────

    private List<Field> loadFields() {
        List<Field> list = new ArrayList<>();
        for (String line : readLines(FIELDS_FILE)) {
            Field f = Field.fromCsv(line);
            if (f != null) list.add(f);
        }
        return list;
    }

    public void saveFields() { writeLines(FIELDS_FILE, fields, Field::toCsv); }
    public List<Field> getAllFields() { return fields; }
    public List<Field> getFieldsByFarmer(String farmerId) {
        return fields.stream().filter(f -> f.getFarmerId().equals(farmerId)).toList();
    }
    public void addField(Field field) { fields.add(field); saveFields(); }
    public void updateField(Field updated) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getId().equals(updated.getId())) { fields.set(i, updated); break; }
        }
        saveFields();
    }
    public void deleteField(String fieldId) { fields.removeIf(f -> f.getId().equals(fieldId)); saveFields(); }

    // ─── CROPS ───────────────────────────────────────────────────────────────

    private List<Crop> loadCrops() {
        List<Crop> list = new ArrayList<>();
        for (String line : readLines(CROPS_FILE)) {
            Crop c = Crop.fromCsv(line);
            if (c != null) list.add(c);
        }
        return list;
    }

    public void saveCrops() { writeLines(CROPS_FILE, crops, Crop::toCsv); }
    public List<Crop> getAllCrops() { return crops; }
    public Optional<Crop> findCropById(String id) { return crops.stream().filter(c -> c.getId().equals(id)).findFirst(); }

    // ─── COOPERATIVES ────────────────────────────────────────────────────────

    private List<Cooperative> loadCooperatives() {
        List<Cooperative> list = new ArrayList<>();
        for (String line : readLines(COOPS_FILE)) {
            Cooperative c = Cooperative.fromCsv(line);
            if (c != null) list.add(c);
        }
        return list;
    }

    public void saveCooperatives() { writeLines(COOPS_FILE, cooperatives, Cooperative::toCsv); }
    public List<Cooperative> getAllCooperatives() { return cooperatives; }
    public Optional<Cooperative> findCoopById(String id) {
        return cooperatives.stream().filter(c -> c.getId().equals(id)).findFirst();
    }
    public void addCooperative(Cooperative coop) { cooperatives.add(coop); saveCooperatives(); }
    public void updateCooperative(Cooperative updated) {
        for (int i = 0; i < cooperatives.size(); i++) {
            if (cooperatives.get(i).getId().equals(updated.getId())) { cooperatives.set(i, updated); break; }
        }
        saveCooperatives();
    }

    // ─── DISTRICTS ───────────────────────────────────────────────────────────

    private List<District> loadDistricts() {
        List<District> list = new ArrayList<>();
        for (String line : readLines(DISTRICTS_FILE)) {
            District d = District.fromCsv(line);
            if (d != null) list.add(d);
        }
        return list;
    }

    public void saveDistricts() { writeLines(DISTRICTS_FILE, districts, District::toCsv); }
    public List<District> getAllDistricts() { return districts; }
    public Optional<District> findDistrictById(String id) {
        return districts.stream().filter(d -> d.getId().equals(id)).findFirst();
    }

    // ─── MARKETS ─────────────────────────────────────────────────────────────

    private List<Market> loadMarkets() {
        List<Market> list = new ArrayList<>();
        for (String line : readLines(MARKETS_FILE)) {
            Market m = Market.fromCsv(line);
            if (m != null) list.add(m);
        }
        return list;
    }

    public void saveMarkets() { writeLines(MARKETS_FILE, markets, Market::toCsv); }
    public List<Market> getAllMarkets() { return markets; }
    public Optional<Market> findMarketById(String id) {
        return markets.stream().filter(m -> m.getId().equals(id)).findFirst();
    }

    // ─── WEATHER ─────────────────────────────────────────────────────────────

    private List<Weather> loadWeather() {
        List<Weather> list = new ArrayList<>();
        for (String line : readLines(WEATHER_FILE)) {
            Weather w = Weather.fromCsv(line);
            if (w != null) list.add(w);
        }
        return list;
    }

    public List<Weather> getWeatherData() { return weatherData; }

    // ─── TRANSACTIONS ─────────────────────────────────────────────────────────

    private List<MarketTransaction> loadTransactions() {
        List<MarketTransaction> list = new ArrayList<>();
        for (String line : readLines(TRANSACTIONS_FILE)) {
            MarketTransaction t = MarketTransaction.fromCsv(line);
            if (t != null) list.add(t);
        }
        return list;
    }

    public void saveTransactions() { writeLines(TRANSACTIONS_FILE, transactions, MarketTransaction::toCsv); }
    public List<MarketTransaction> getAllTransactions() { return transactions; }
    public List<MarketTransaction> getTransactionsByUser(String userId) {
        return transactions.stream().filter(t -> t.getUserId().equals(userId)).toList();
    }
    public List<MarketTransaction> getTransactionsByMarket(String marketId) {
        return transactions.stream().filter(t -> t.getMarketId().equals(marketId)).toList();
    }
    public void addTransaction(MarketTransaction t) { transactions.add(t); saveTransactions(); }

    // ─── CROP INVENTORY ───────────────────────────────────────────────────────

    private List<CropInventory> loadInventories() {
        List<CropInventory> list = new ArrayList<>();
        for (String line : readLines(INVENTORY_FILE)) {
            CropInventory inv = CropInventory.fromCsv(line);
            if (inv != null) list.add(inv);
        }
        return list;
    }

    public void saveInventories() { writeLines(INVENTORY_FILE, inventories, CropInventory::toCsv); }

    public List<CropInventory> getInventoriesByFarmer(String farmerId) {
        return inventories.stream().filter(i -> i.getFarmerId().equals(farmerId)).toList();
    }

    public Optional<CropInventory> findInventory(String farmerId, String cropId) {
        return inventories.stream()
                .filter(i -> i.getFarmerId().equals(farmerId) && i.getCropId().equals(cropId))
                .findFirst();
    }

    /**
     * Returns the inventory for (farmerId, cropId), applying production accrual first.
     * Creates it with 50 kg starting stock if it doesn't exist yet.
     */
    public CropInventory getOrCreateInventory(String farmerId, String cropId) {
        Optional<CropInventory> existing = findInventory(farmerId, cropId);
        if (existing.isPresent()) {
            CropInventory inv = existing.get();
            inv.applyProduction();
            updateInventory(inv);
            return inv;
        }
        // First time — seed with 50 kg
        CropInventory inv = new CropInventory(farmerId, cropId, 50.0, System.currentTimeMillis());
        inventories.add(inv);
        saveInventories();
        return inv;
    }

    public void updateInventory(CropInventory updated) {
        for (int i = 0; i < inventories.size(); i++) {
            CropInventory inv = inventories.get(i);
            if (inv.getFarmerId().equals(updated.getFarmerId())
                    && inv.getCropId().equals(updated.getCropId())) {
                inventories.set(i, updated);
                saveInventories();
                return;
            }
        }
        inventories.add(updated);
        saveInventories();
    }

    // ─── COOPERATIVE MEMBERSHIP REQUESTS ─────────────────────────────────────

    private List<CoopMembershipRequest> loadMembershipRequests() {
        List<CoopMembershipRequest> list = new ArrayList<>();
        for (String line : readLines(MEMBERSHIP_FILE)) {
            CoopMembershipRequest r = CoopMembershipRequest.fromCsv(line);
            if (r != null) list.add(r);
        }
        return list;
    }

    public void saveMembershipRequests() {
        writeLines(MEMBERSHIP_FILE, membershipRequests, CoopMembershipRequest::toCsv);
    }

    public List<CoopMembershipRequest> getAllMembershipRequests() { return membershipRequests; }

    public List<CoopMembershipRequest> getMembershipRequestsByFarmer(String farmerId) {
        return membershipRequests.stream().filter(r -> r.getFarmerId().equals(farmerId)).toList();
    }

    public List<CoopMembershipRequest> getMembershipRequestsByCoop(String coopId) {
        return membershipRequests.stream().filter(r -> r.getCoopId().equals(coopId)).toList();
    }

    public void addMembershipRequest(CoopMembershipRequest req) {
        membershipRequests.add(req);
        saveMembershipRequests();
    }

    public void updateMembershipRequest(CoopMembershipRequest updated) {
        for (int i = 0; i < membershipRequests.size(); i++) {
            if (membershipRequests.get(i).getId().equals(updated.getId())) {
                membershipRequests.set(i, updated);
                break;
            }
        }
        saveMembershipRequests();
    }

    public boolean hasPendingMembershipRequest(String farmerId, String coopId) {
        return membershipRequests.stream().anyMatch(r ->
                r.getFarmerId().equals(farmerId) &&
                r.getCoopId().equals(coopId) &&
                "PENDING".equals(r.getStatus()));
    }

    public String nextMembershipRequestId() {
        String prefix = "MBR-";
        int max = 0;
        for (CoopMembershipRequest r : membershipRequests) {
            if (r.getId().startsWith(prefix)) {
                try { int n = Integer.parseInt(r.getId().substring(prefix.length())); if (n > max) max = n; }
                catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    private void seedData() {
        // Districts
        District d1 = new District("DIS-001", "Dang",       new ArrayList<>(List.of("CO-001")), new ArrayList<>(List.of("MAR-001")));
        District d2 = new District("DIS-002", "Chitwan",    new ArrayList<>(List.of("CO-002")), new ArrayList<>(List.of("MAR-002")));
        District d3 = new District("DIS-003", "Rupandehi",  new ArrayList<>(),                  new ArrayList<>(List.of("MAR-003")));
        districts.add(d1); districts.add(d2); districts.add(d3);
        saveDistricts();

        // Markets
        Market m1 = new Market("MAR-001", "Dang Agricultural Market",    "DIS-001");
        Market m2 = new Market("MAR-002", "Chitwan Grain Market",        "DIS-002");
        Market m3 = new Market("MAR-003", "Rupandehi Farmers Market",    "DIS-003");
        markets.add(m1); markets.add(m2); markets.add(m3);
        saveMarkets();

        // Crops
        Crop c1 = new Crop("CRP-001", "Maize");
        Crop c2 = new Crop("CRP-002", "Wheat");
        Crop c3 = new Crop("CRP-003", "Rice");
        Crop c4 = new Crop("CRP-004", "Sorghum");
        Crop c5 = new Crop("CRP-005", "Cassava");
        crops.add(c1); crops.add(c2); crops.add(c3); crops.add(c4); crops.add(c5);
        saveCrops();

        // Admin
        Admin admin = new Admin("USR-A-001", "System Admin", "admin@agrisystem.com", "Admin@1234");
        users.add(admin);

        // Officer
        Officer officer = new Officer("USR-O-001", "Jane Officer", "officer@agrisystem.com", "0712345678", "Officer@1234", "CO-001");
        users.add(officer);

        // Farmers
        Farmer f1 = new Farmer("USR-F-001", "Ram Bahadur", "farmer@agrisystem.com", "9841234567", "Farmer@1234",
                "CO-001", new ArrayList<>(List.of("CRP-001", "CRP-002")), "DIS-001", "MAR-001", 15000.0, 75.0);
        Farmer f2 = new Farmer("USR-F-002", "Sita Devi", "alice@farm.com", "9857654321", "Alice@1234",
                "CO-001", new ArrayList<>(List.of("CRP-003")), "DIS-001", "MAR-001", 8000.0, 60.0);
        users.add(f1); users.add(f2);
        saveUsers();

        // Cooperatives
        Cooperative coop = new Cooperative("CO-001", "Dang Farmers Cooperative", "USR-O-001",
                new ArrayList<>(List.of("USR-F-001", "USR-F-002")),
                "DIS-001", new ArrayList<>(List.of("MAR-001")), new ArrayList<>());
        Cooperative coop2 = new Cooperative("CO-002", "Chitwan Growers Cooperative", "USR-O-001",
                new ArrayList<>(), "DIS-002", new ArrayList<>(List.of("MAR-002")), new ArrayList<>());
        cooperatives.add(coop); cooperatives.add(coop2);
        saveCooperatives();

        // Fields
        Field field1 = new Field("FIE-001", "USR-F-001", new ArrayList<>(List.of("CRP-001")), 2.5, "");
        Field field2 = new Field("FIE-002", "USR-F-001", new ArrayList<>(List.of("CRP-002")), 1.8, "");
        Field field3 = new Field("FIE-003", "USR-F-002", new ArrayList<>(List.of("CRP-003")), 3.0, "");
        fields.add(field1); fields.add(field2); fields.add(field3);
        saveFields();

        // Weather (30 days of dummy data)
        String[] conditions = {"Sunny", "Cloudy", "Rainy", "Partly Cloudy", "Windy"};
        Random rng = new Random(42);
        for (int i = 0; i < 30; i++) {
            int day = i + 1;
            String date = String.format("2025-%02d-%02d", (day / 30) + 3, (day % 28) + 1);
            double temp = 18 + rng.nextDouble() * 15;
            double hum = 40 + rng.nextDouble() * 40;
            weatherData.add(new Weather(date, Math.round(temp * 10.0) / 10.0,
                    conditions[rng.nextInt(conditions.length)], Math.round(hum * 10.0) / 10.0));
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(WEATHER_FILE))) {
            for (Weather w : weatherData) { bw.write(w.toCsv()); bw.newLine(); }
        } catch (IOException e) { e.printStackTrace(); }

        // Seed initial crop inventories (50 kg each)
        long now = System.currentTimeMillis();
        for (String cropId : List.of("CRP-001", "CRP-002")) {
            inventories.add(new CropInventory("USR-F-001", cropId, 50.0, now));
        }
        inventories.add(new CropInventory("USR-F-002", "CRP-003", 50.0, now));
        saveInventories();
    }
}
