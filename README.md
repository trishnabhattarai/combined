# 🌾 AgriSystem — Farm Management Platform

A JavaFX application with FXML + Controllers for managing agricultural cooperatives, loans, markets, and farm operations.

---

## 📁 Project Structure

```
agrisystem/
├── pom.xml
├── data/                         ← CSV files (auto-created on first run)
│   ├── users.csv
│   ├── loans.csv
│   ├── fields.csv
│   ├── crops.csv
│   ├── cooperatives.csv
│   ├── districts.csv
│   ├── markets.csv
│   ├── weather.csv
│   └── transactions.csv
├── assets/
│   └── images/                   ← Profile photos (<userId>.png)
└── src/main/
    ├── java/com/agrisystem/
    │   ├── MainApp.java
    │   ├── model/
    │   │   ├── User.java           (base class)
    │   │   ├── Admin.java
    │   │   ├── Farmer.java
    │   │   ├── Officer.java
    │   │   ├── Loan.java
    │   │   ├── Cooperative.java
    │   │   ├── Crop.java
    │   │   ├── Field.java
    │   │   ├── District.java
    │   │   ├── Market.java
    │   │   ├── Weather.java
    │   │   └── MarketTransaction.java
    │   ├── exception/
    │   │   ├── UserAlreadyExistsException.java
    │   │   ├── UserNotFoundException.java
    │   │   ├── InvalidPasswordException.java
    │   │   ├── InvalidEmailException.java
    │   │   ├── InsufficientFundsException.java
    │   │   └── LoanException.java
    │   ├── service/
    │   │   ├── DataService.java    (all file I/O via BufferedReader/FileReader)
    │   │   └── AuthService.java
    │   ├── util/
    │   │   ├── Validator.java      (password + email validation)
    │   │   ├── IdGenerator.java
    │   │   ├── SessionManager.java
    │   │   ├── SceneManager.java
    │   │   └── AlertUtil.java
    │   └── controller/
    │       ├── LoginController.java
    │       ├── SignupController.java
    │       ├── farmer/
    │       │   ├── FarmerMainController.java
    │       │   ├── FarmerDashboardController.java
    │       │   ├── FarmerFieldsController.java
    │       │   ├── FarmerLoansController.java
    │       │   ├── FarmerMarketController.java
    │       │   └── FarmerProfileController.java
    │       ├── admin/
    │       │   ├── AdminMainController.java
    │       │   ├── AdminDashboardController.java
    │       │   ├── AdminManageUsersController.java
    │       │   └── AdminProfileController.java
    │       └── officer/
    │           ├── OfficerMainController.java
    │           ├── OfficerDashboardController.java
    │           ├── OfficerLoansController.java
    │           └── OfficerReportController.java
    └── resources/com/agrisystem/
        ├── fxml/
        │   ├── Login.fxml
        │   ├── Signup.fxml
        │   ├── farmer/  (FarmerMain, Dashboard, Fields, Loans, Market, Profile)
        │   ├── admin/   (AdminMain, Dashboard, ManageUsers, Profile)
        │   └── officer/ (OfficerMain, Dashboard, Loans, Report)
        └── css/
            └── main.css
```

---

## 🚀 Running the Application

### Prerequisites
- Java 17+
- Maven 3.8+
- JavaFX 21 (handled via Maven)

### Run

```bash
cd agrisystem
mvn javafx:run
```

### Default Seed Accounts

| Type    | Email                       | Password     |
|---------|-----------------------------|--------------|
| Admin   | admin@agrisystem.com        | Admin@1234   |
| Officer | officer@agrisystem.com      | Officer@1234 |
| Farmer  | farmer@agrisystem.com       | Farmer@1234  |
| Farmer  | alice@farm.com              | Alice@1234   |

---

## 💾 Data Storage

All data is stored in CSV files in the `data/` directory using `FileReader`, `BufferedReader`, and `FileWriter`/`BufferedWriter`. Each model class has `toCsv()` and `fromCsv()` static methods for serialization.

**CSV formats:**
- `users.csv` — id, name, email, number, password, type, [type-specific fields...]
- `loans.csv` — id, amount, userId, status, coopId, officerId, requestDate, dueDate
- `fields.csv` — id, farmerId, cropIds(;-separated), area, imagePath
- `cooperatives.csv` — id, name, officerId, memberIds(;), districtId, marketIds(;), loanIds(;)
- `transactions.csv` — id, userId, cropId, marketId, type, quantity, pricePerKg, total, timestamp

---

## 🔐 Password Validation Rules

Passwords must contain:
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one digit (0-9)
- At least one special character (!@#$%...)

Enforced via `Validator.validatePassword()` → throws `InvalidPasswordException`.

---

## 📐 Design Decisions / Class Structure Changes

### Added `MarketTransaction`
The spec implied buy/sell functionality but had no transaction model. `MarketTransaction` was added to track all buy/sell events, enabling income/expense graphs and market activity reports.

### Subclassing `User`
`Admin`, `Farmer`, `Officer` each extend `User` and override `toCsv()`/`fromCsv()` to include their extra fields (coopId, cropIds, balance, etc.). `DataService.loadUsers()` dispatches to the correct subclass parser based on the `type` column.

### `DataService` as central I/O singleton
All CSV reads/writes go through `DataService`. This keeps file access in one place and avoids scattered `new FileReader(...)` calls across controllers. Relationships (e.g. cooperative→members, loan→coop) are stored as ID lists in the CSV.

### `SessionManager` for auth state
Stores the currently logged-in `User` object, accessible throughout the app without passing it around.

### `SceneManager` for navigation
Centralized FXML loading + scene switching, so controllers only call `SceneManager.getInstance().showFarmerDashboard()` etc.

---

## 🎨 Features Summary

| Feature | User |
|---------|------|
| Login / Signup with email or phone | All |
| Password strength validation + hints | Signup |
| Sidebar navigation with active highlight | All |
| Profile pic upload (saved as /assets/images/<id>.png) | All |
| Temperature trend line chart (30 days dummy data) | Farmer |
| 3×3 field grid with Next/Previous pagination | Farmer |
| Loan request, approval, and repayment flow | Farmer + Officer |
| Buy/Sell crops with random market prices (50–1000/kg) | Farmer |
| Income/Expense bar chart and totals | Farmer |
| User distribution pie chart | Admin |
| Search + filter users with 25/page pagination | Admin |
| Edit/Delete users with cascade deletion | Admin |
| Market activity line chart per market | Officer |
| Loan responsibility score meter | Officer |
| Cooperative report with pie + bar charts | Officer |
