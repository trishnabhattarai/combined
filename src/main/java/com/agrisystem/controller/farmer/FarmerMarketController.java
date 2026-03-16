package com.agrisystem.controller.farmer;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.service.MarketPriceService;
import com.agrisystem.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class FarmerMarketController {

    @FXML private ComboBox<Market> marketCombo;
    @FXML private ComboBox<Crop> cropCombo;
    @FXML private TextField quantityField;
    @FXML private Label priceLabel;
    @FXML private Label totalLabel;
    @FXML private Label balanceLabel;
    @FXML private Label inventoryLabel; // shows available stock for selected crop

    @FXML private TableView<MarketTransaction> txTable;
    @FXML private TableColumn<MarketTransaction, String> colType;
    @FXML private TableColumn<MarketTransaction, String> colCrop;
    @FXML private TableColumn<MarketTransaction, String> colMarket;
    @FXML private TableColumn<MarketTransaction, String> colQty;
    @FXML private TableColumn<MarketTransaction, String> colPrice;
    @FXML private TableColumn<MarketTransaction, String> colTotal;
    @FXML private TableColumn<MarketTransaction, String> colDate;

    @FXML private BarChart<String, Number> financeChart;
    @FXML private Label totalIncome;
    @FXML private Label totalExpense;

    private final DataService ds = DataService.getInstance();
    private final MarketPriceService priceService = MarketPriceService.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
    private final SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd");

    @FXML
    public void initialize() {
        marketCombo.setItems(FXCollections.observableArrayList(ds.getAllMarkets()));
        cropCombo.setItems(FXCollections.observableArrayList(ds.getAllCrops()));

        cropCombo.setOnAction(e -> { updatePrice(); updateInventoryLabel(); });
        marketCombo.setOnAction(e -> updatePrice());
        quantityField.textProperty().addListener((o, old, val) -> updateTotal());

        setupColumns();
        refreshBalance();
        refreshTransactions();
    }

    @FXML
    private void onMarketSelected() { updatePrice(); }

    // ─── Price helpers ────────────────────────────────────────────────────────

    private double getPrice(String marketId, String cropId) {
        return priceService.getPrice(marketId, cropId);
    }

    private void updatePrice() {
        Market market = marketCombo.getValue();
        Crop crop = cropCombo.getValue();
        if (market == null || crop == null) return;
        priceLabel.setText(String.format("NRs %.2f", getPrice(market.getId(), crop.getId())));
        updateTotal();
    }

    private void updateTotal() {
        Market market = marketCombo.getValue();
        Crop crop = cropCombo.getValue();
        if (market == null || crop == null) return;
        try {
            double qty = Double.parseDouble(quantityField.getText().trim());
            totalLabel.setText(String.format("NRs %.2f", qty * getPrice(market.getId(), crop.getId())));
        } catch (NumberFormatException e) { totalLabel.setText("—"); }
    }

    private void updateInventoryLabel() {
        User user = SessionManager.getInstance().getCurrentUser();
        Crop crop = cropCombo.getValue();
        if (crop == null || !(user instanceof Farmer)) {
            if (inventoryLabel != null) inventoryLabel.setText("Stock: —");
            return;
        }
        CropInventory inv = ds.getOrCreateInventory(user.getId(), crop.getId());
        if (inventoryLabel != null)
            inventoryLabel.setText(String.format("In stock: %.2f kg", inv.getQuantity()));
    }

    // ─── Buy ─────────────────────────────────────────────────────────────────

    @FXML
    private void buyCrop() {
        if (!validateInputs()) return;
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Farmer farmer)) return;

        Market market = marketCombo.getValue();
        Crop crop = cropCombo.getValue();
        double qty = Double.parseDouble(quantityField.getText().trim());
        if (qty < 0.5) { AlertUtil.showError("Invalid Quantity", "Minimum purchase is 0.5 kg."); return; }

        double price = getPrice(market.getId(), crop.getId());
        double total = qty * price;

        if (farmer.getBalance() < total) {
            AlertUtil.showError("Insufficient Funds", "You need NRs " + String.format("%.2f", total) +
                    " but only have NRs " + String.format("%.2f", farmer.getBalance()) + ".");
            return;
        }

        // Deduct balance
        farmer.setBalance(farmer.getBalance() - total);
        ds.updateUser(farmer);
        SessionManager.getInstance().setCurrentUser(farmer);

        // Add to inventory
        CropInventory inv = ds.getOrCreateInventory(farmer.getId(), crop.getId());
        inv.setQuantity(inv.getQuantity() + qty);
        ds.updateInventory(inv);

        recordTransaction(user.getId(), crop.getId(), market.getId(), "BUY", qty, price);
        refreshBalance();
        refreshTransactions();
        updateInventoryLabel();
        AlertUtil.showInfo("Purchase Complete",
                String.format("Bought %.2f kg of %s for NRs %.2f", qty, crop.getName(), total));
    }

    // ─── Sell ────────────────────────────────────────────────────────────────

    @FXML
    private void sellCrop() {
        if (!validateInputs()) return;
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Farmer farmer)) return;

        Market market = marketCombo.getValue();
        Crop crop = cropCombo.getValue();
        double qty = Double.parseDouble(quantityField.getText().trim());

        // Enforce inventory: apply accrued production, then check stock
        CropInventory inv = ds.getOrCreateInventory(farmer.getId(), crop.getId());
        if (qty < 0.5) { AlertUtil.showError("Invalid Quantity", "Minimum sale is 0.5 kg."); return; }
        if (qty > inv.getQuantity()) {
            AlertUtil.showError("Insufficient Stock",
                    String.format("You only have %.2f kg of %s available.\n" +
                            "Fields produce 10 kg/hour — come back later or reduce quantity.",
                            inv.getQuantity(), crop.getName()));
            return;
        }

        double price = getPrice(market.getId(), crop.getId());
        double total = qty * price;

        // Deduct stock, add balance
        inv.setQuantity(inv.getQuantity() - qty);
        ds.updateInventory(inv);

        farmer.setBalance(farmer.getBalance() + total);
        ds.updateUser(farmer);
        SessionManager.getInstance().setCurrentUser(farmer);

        recordTransaction(user.getId(), crop.getId(), market.getId(), "SELL", qty, price);
        refreshBalance();
        refreshTransactions();
        updateInventoryLabel();
        AlertUtil.showInfo("Sale Complete",
                String.format("Sold %.2f kg of %s for NRs %.2f\nRemaining stock: %.2f kg",
                        qty, crop.getName(), total, inv.getQuantity()));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean validateInputs() {
        if (marketCombo.getValue() == null) { AlertUtil.showError("Error", "Please select a market."); return false; }
        if (cropCombo.getValue() == null) { AlertUtil.showError("Error", "Please select a crop."); return false; }
        try {
            double qty = Double.parseDouble(quantityField.getText().trim());
            if (qty <= 0) { AlertUtil.showError("Error", "Quantity must be positive."); return false; }
        } catch (NumberFormatException e) { AlertUtil.showError("Error", "Enter a valid number for quantity."); return false; }
        return true;
    }

    private void recordTransaction(String userId, String cropId, String marketId,
                                   String type, double qty, double price) {
        String id = IdGenerator.nextTransactionId();
        ds.addTransaction(new MarketTransaction(id, userId, cropId, marketId, type, qty, price,
                System.currentTimeMillis()));
    }

    private void setupColumns() {
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colCrop.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findCropById(c.getValue().getCropId()).map(Crop::getName).orElse(c.getValue().getCropId())));
        colMarket.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findMarketById(c.getValue().getMarketId()).map(Market::getName).orElse(c.getValue().getMarketId())));
        colQty.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getQuantity())));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getPricePerKg())));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getTotalAmount())));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                fullSdf.format(new Date(c.getValue().getTimestamp()))));

        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("BUY".equals(item)
                        ? "-fx-text-fill: #F44336; -fx-font-weight:bold;"
                        : "-fx-text-fill: #4CAF50; -fx-font-weight:bold;");
            }
        });
    }

    private void refreshBalance() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user instanceof Farmer f)
            balanceLabel.setText("Balance: NRs " + String.format("%.2f", f.getBalance()));
    }

    private void refreshTransactions() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<MarketTransaction> txs = ds.getTransactionsByUser(user.getId());
        txTable.setItems(FXCollections.observableArrayList(txs));

        double income  = txs.stream().filter(t -> "SELL".equals(t.getType())).mapToDouble(MarketTransaction::getTotalAmount).sum();
        double expense = txs.stream().filter(t -> "BUY".equals(t.getType())).mapToDouble(MarketTransaction::getTotalAmount).sum();
        totalIncome.setText(String.format("NRs %.2f", income));
        totalExpense.setText(String.format("NRs %.2f", expense));

        financeChart.getData().clear();
        XYChart.Series<String, Number> incomeSeries  = new XYChart.Series<>(); incomeSeries.setName("Income");
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>(); expenseSeries.setName("Expense");
        Map<String, Double> incomeMap  = new TreeMap<>();
        Map<String, Double> expenseMap = new TreeMap<>();
        for (MarketTransaction tx : txs) {
            String day = sdf.format(new Date(tx.getTimestamp()));
            if ("SELL".equals(tx.getType())) incomeMap.merge(day,  tx.getTotalAmount(), Double::sum);
            else                              expenseMap.merge(day, tx.getTotalAmount(), Double::sum);
        }
        incomeMap.forEach((k, v)  -> incomeSeries.getData().add(new XYChart.Data<>(k, v)));
        expenseMap.forEach((k, v) -> expenseSeries.getData().add(new XYChart.Data<>(k, v)));
        financeChart.getData().addAll(incomeSeries, expenseSeries);
    }
}
