package com.agrisystem.controller.farmer;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class FarmerLoansController {

    // Table and columns
    @FXML private TableView<Loan> loansTable;
    @FXML private TableColumn<Loan, String> colId;
    @FXML private TableColumn<Loan, String> colAmount;
    @FXML private TableColumn<Loan, String> colCoop;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, String> colDate;
    @FXML private TableColumn<Loan, String> colAction;

    // Stat-card labels
    @FXML private Label balanceLabel;
    @FXML private Label responsibilityLabel;
    @FXML private ProgressBar responsibilityBar;
    @FXML private Label activeLoansLabel;

    private final DataService ds = DataService.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @FXML
    public void initialize() {
        setupColumns();
        refreshLoans();
    }

    private void refreshLoans() {
        // Update current user data
        User raw = SessionManager.getInstance().getCurrentUser();
        ds.findUserById(raw.getId()).ifPresent(fresh -> {
            SessionManager.getInstance().setCurrentUser(fresh);
            if (fresh instanceof Farmer f) {
                balanceLabel.setText(String.format("Nrs %.2f", f.getBalance()));
                double score = f.getResponsibilityScore();
                responsibilityLabel.setText(String.format("%.1f / 100", score));
                responsibilityBar.setProgress(score / 100.0);
                String barStyle = score >= 70
                        ? "-fx-accent: #4CAF50;"
                        : score >= 40 ? "-fx-accent: #FF9800;" : "-fx-accent: #F44336;";
                responsibilityBar.setStyle(barStyle);
            }
        });

        User user = SessionManager.getInstance().getCurrentUser();
        List<Loan> loans = ds.getLoansByUser(user.getId());
        long active = loans.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        activeLoansLabel.setText(String.valueOf(active));
        loansTable.setItems(FXCollections.observableArrayList(loans));
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getAmount())));
        colCoop.setCellValueFactory(c -> {
            String coopId = c.getValue().getCoopId();
            return new SimpleStringProperty(ds.findCoopById(coopId).map(Cooperative::getName).orElse(coopId));
        });
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(sdf.format(new Date(c.getValue().getRequestDate()))));

        // Color-coded status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "APPROVED" -> "#4CAF50";
                    case "REJECTED" -> "#F44336";
                    case "REPAID" -> "#2196F3";
                    default -> "#FF9800";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        // Action column with dynamic "Repay" button
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("btn-sm");
                btn.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx < 0 || idx >= getTableView().getItems().size()) return;
                    repayLoan(getTableView().getItems().get(idx));
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Loan loan = getTableView().getItems().get(getIndex());
                boolean canRepay = "APPROVED".equals(loan.getStatus());
                btn.setText(canRepay ? "Repay" : "—");
                btn.setDisable(!canRepay);
                btn.getStyleClass().removeAll("btn-success-sm");
                if (canRepay) btn.getStyleClass().add("btn-success-sm");
                setGraphic(btn);
            }
        });
    }

    private void repayLoan(Loan loan) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Farmer farmer)) return;

        ds.getAllLoans().stream().filter(l -> l.getId().equals(loan.getId())).findFirst()
                .ifPresentOrElse(current -> {
                    if (!"APPROVED".equals(current.getStatus())) {
                        AlertUtil.showWarning("Cannot Repay", "Loan is not in APPROVED state.");
                        refreshLoans();
                        return;
                    }
                    if (farmer.getBalance() < current.getAmount()) {
                        AlertUtil.showError("Insufficient Funds",
                                String.format("Need Nrs %.2f — you have Nrs %.2f.", current.getAmount(), farmer.getBalance()));
                        return;
                    }
                    if (!AlertUtil.showConfirm("Repay Loan",
                            String.format("Repay Nrs %.2f for loan %s?", current.getAmount(), current.getId()))) return;

                    // Deduct balance and mark as repaid
                    farmer.setBalance(farmer.getBalance() - current.getAmount());
                    current.setStatus("REPAID");
                    ds.updateLoan(current);

                    // Responsibility score
                    long now = System.currentTimeMillis();
                    boolean onTime = now <= current.getDueDate();
                    farmer.setResponsibilityScore(farmer.getResponsibilityScore() + (onTime ? 5 : -10));

                    ds.updateUser(farmer);
                    SessionManager.getInstance().setCurrentUser(farmer);
                    refreshLoans();

                    String scoreMsg = onTime
                            ? "\n✅ On-time repayment! Score +5 → " + String.format("%.1f", farmer.getResponsibilityScore())
                            : "\n⚠️ Late repayment. Score −10 → " + String.format("%.1f", farmer.getResponsibilityScore());
                    AlertUtil.showInfo("Loan Repaid", "Successfully repaid." + scoreMsg);
                }, () -> AlertUtil.showError("Error", "Loan not found."));
    }

    @FXML
    private void requestLoan() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Farmer farmer)) return;

        List<Cooperative> coops = ds.getAllCooperatives().stream()
                .filter(c -> c.getMemberIds().contains(user.getId()))
                .toList();

        if (coops.isEmpty()) {
            AlertUtil.showWarning("No Cooperative",
                    "You must be an approved cooperative member to request a loan.");
            return;
        }

        Dialog<Loan> dialog = new Dialog<>();
        dialog.setTitle("Request Loan");
        dialog.setHeaderText("Submit a new loan request");
        ButtonType submitType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitType, ButtonType.CANCEL);

        ComboBox<Cooperative> coopBox = new ComboBox<>(FXCollections.observableArrayList(coops));
        coopBox.setPromptText("Select cooperative");
        TextField amountField = new TextField();
        amountField.setPromptText("Amount (Nrs)");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
                new Label("Cooperative:"), coopBox,
                new Label("Amount (Nrs):"), amountField);
        content.setPadding(new javafx.geometry.Insets(16));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn != submitType) return null;
            Cooperative coop = coopBox.getValue();
            if (coop == null) { AlertUtil.showError("Error", "Select a cooperative."); return null; }
            double amount;
            try { amount = Double.parseDouble(amountField.getText().trim()); }
            catch (NumberFormatException e) { AlertUtil.showError("Error", "Enter a valid number."); return null; }
            if (amount <= 0) { AlertUtil.showError("Error", "Amount must be positive."); return null; }

            long now = System.currentTimeMillis();
            return new Loan(IdGenerator.nextLoanId(), amount, user.getId(), "PENDING",
                    coop.getId(), coop.getOfficerId(), now, now + (30L * 24 * 60 * 60 * 1000));
        });

        dialog.showAndWait().ifPresent(loan -> {
            ds.addLoan(loan);
            ds.findCoopById(loan.getCoopId()).ifPresent(c -> {
                c.getLoanIds().add(loan.getId());
                ds.updateCooperative(c);
            });
            refreshLoans();
            AlertUtil.showInfo("Loan Requested", "Your request has been submitted for officer review.");
        });
    }
}