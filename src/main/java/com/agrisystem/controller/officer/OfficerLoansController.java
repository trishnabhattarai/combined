package com.agrisystem.controller.officer;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class OfficerLoansController {

    // ── Loan tab ──────────────────────────────────────────────────────────────
    @FXML private TableView<Loan> loansTable;
    @FXML private TableColumn<Loan, String> colId;
    @FXML private TableColumn<Loan, String> colFarmer;
    @FXML private TableColumn<Loan, String> colAmount;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, String> colDate;
    @FXML private TableColumn<Loan, String> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    // ── Membership tab ────────────────────────────────────────────────────────
    @FXML private TableView<CoopMembershipRequest> memberTable;
    @FXML private TableColumn<CoopMembershipRequest, String> colMbrId;
    @FXML private TableColumn<CoopMembershipRequest, String> colMbrFarmer;
    @FXML private TableColumn<CoopMembershipRequest, String> colMbrCoop;
    @FXML private TableColumn<CoopMembershipRequest, String> colMbrStatus;
    @FXML private TableColumn<CoopMembershipRequest, String> colMbrDate;
    @FXML private TableColumn<CoopMembershipRequest, String> colMbrAction;

    // ── Farmer score column ───────────────────────────────────────────────────
    @FXML private TableColumn<Loan, String> colFarmerScore;

    private final DataService ds = DataService.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private String coopId;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user instanceof Officer officer) {
            coopId = officer.getCoopId();
        }

        statusFilter.setItems(FXCollections.observableArrayList(
                "All", "PENDING", "APPROVED", "REJECTED", "REPAID"));
        statusFilter.setValue("All");

        setupLoanColumns();
        setupMemberColumns();
        filterLoans();
        refreshMemberships();
    }

    // ── LOANS ─────────────────────────────────────────────────────────────────

    @FXML
    public void filterLoans() {
        String query = searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        List<Loan> coopLoans = coopId != null ? ds.getLoansByCoop(coopId) : ds.getAllLoans();
        List<Loan> filtered = coopLoans.stream().filter(l -> {
            boolean matchStatus = "All".equals(status) || l.getStatus().equals(status);
            boolean matchQuery  = query.isEmpty()
                    || l.getUserId().toLowerCase().contains(query)
                    || ds.findUserById(l.getUserId())
                          .map(u -> u.getName().toLowerCase().contains(query))
                          .orElse(false);
            return matchStatus && matchQuery;
        }).collect(Collectors.toList());

        loansTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void setupLoanColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colFarmer.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findUserById(c.getValue().getUserId())
                  .map(u -> u.getName() + " (" + u.getId() + ")")
                  .orElse(c.getValue().getUserId())));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.2f", c.getValue().getAmount())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                sdf.format(new Date(c.getValue().getRequestDate()))));

        // Farmer responsibility score column
        colFarmerScore.setCellValueFactory(c -> {
            String score = ds.findUserById(c.getValue().getUserId())
                    .filter(u -> u instanceof Farmer)
                    .map(u -> String.format("%.1f / 100", ((Farmer) u).getResponsibilityScore()))
                    .orElse("N/A");
            return new SimpleStringProperty(score);
        });
        colFarmerScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                try {
                    double val = Double.parseDouble(item.split(" ")[0]);
                    String color = val >= 70 ? "#4CAF50" : val >= 40 ? "#FF9800" : "#F44336";
                    setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                } catch (NumberFormatException ignored) { setStyle(""); }
            }
        });

        // Colour-coded status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "APPROVED" -> "#4CAF50";
                    case "REJECTED" -> "#F44336";
                    case "REPAID"   -> "#2196F3";
                    default         -> "#FF9800";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        // Approve / Reject buttons — only active when PENDING
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn  = new Button("Reject");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(6, approveBtn, rejectBtn);
            {
                approveBtn.getStyleClass().addAll("btn-sm", "btn-success-sm");
                rejectBtn.getStyleClass().addAll("btn-sm",  "btn-danger-sm");
                box.setPadding(new Insets(4));
                approveBtn.setOnAction(e -> processLoan(
                        getTableView().getItems().get(getIndex()), "APPROVED"));
                rejectBtn.setOnAction(e -> processLoan(
                        getTableView().getItems().get(getIndex()), "REJECTED"));
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) { setGraphic(null); return; }
                Loan loan = getTableView().getItems().get(idx);
                boolean isPending = "PENDING".equals(loan.getStatus());
                approveBtn.setDisable(!isPending);
                rejectBtn.setDisable(!isPending);
                setGraphic(box);
            }
        });
    }

    private void processLoan(Loan loan, String newStatus) {
        // Re-fetch loan to prevent stale-reference double processing
        Optional<Loan> fresh = ds.getAllLoans().stream()
                .filter(l -> l.getId().equals(loan.getId())).findFirst();
        if (fresh.isEmpty()) return;
        Loan current = fresh.get();

        // Hard guard — only PENDING may be processed
        if (!"PENDING".equals(current.getStatus())) {
            AlertUtil.showWarning("Already Processed",
                    "Loan " + current.getId() + " is already " + current.getStatus().toLowerCase() + ".");
            filterLoans();
            return;
        }

        String action = "APPROVED".equals(newStatus) ? "approve" : "reject";
        if (!AlertUtil.showConfirm("Loan Decision",
                "Are you sure you want to " + action + " loan " + current.getId() + "?")) return;

        User officer = SessionManager.getInstance().getCurrentUser();
        current.setStatus(newStatus);
        current.setOfficerId(officer.getId());
        ds.updateLoan(current);

        // Credit farmer balance on approval
        if ("APPROVED".equals(newStatus)) {
            ds.findUserById(current.getUserId()).ifPresent(u -> {
                if (u instanceof Farmer f) {
                    f.setBalance(f.getBalance() + current.getAmount());
                    ds.updateUser(f);
                }
            });
        }

        filterLoans();
        AlertUtil.showInfo("Done", "Loan " + newStatus.toLowerCase() + " successfully.");
    }

    // ── MEMBERSHIP REQUESTS ───────────────────────────────────────────────────

    private void refreshMemberships() {
        List<CoopMembershipRequest> requests = coopId != null
                ? ds.getMembershipRequestsByCoop(coopId)
                : ds.getAllMembershipRequests();
        memberTable.setItems(FXCollections.observableArrayList(requests));
    }

    private void setupMemberColumns() {
        colMbrId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colMbrFarmer.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findUserById(c.getValue().getFarmerId())
                  .map(u -> u.getName() + " (" + u.getId() + ")")
                  .orElse(c.getValue().getFarmerId())));
        colMbrCoop.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findCoopById(c.getValue().getCoopId())
                  .map(Cooperative::getName).orElse(c.getValue().getCoopId())));
        colMbrStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colMbrDate.setCellValueFactory(c -> new SimpleStringProperty(
                sdf.format(new Date(c.getValue().getRequestDate()))));

        // Colour-coded status
        colMbrStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = "APPROVED".equals(item) ? "#4CAF50"
                             : "REJECTED".equals(item) ? "#F44336" : "#FF9800";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        // Approve / Reject membership buttons
        colMbrAction.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn  = new Button("Reject");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(6, approveBtn, rejectBtn);
            {
                approveBtn.getStyleClass().addAll("btn-sm", "btn-success-sm");
                rejectBtn.getStyleClass().addAll("btn-sm",  "btn-danger-sm");
                box.setPadding(new Insets(4));
                approveBtn.setOnAction(e -> processMembership(
                        getTableView().getItems().get(getIndex()), "APPROVED"));
                rejectBtn.setOnAction(e -> processMembership(
                        getTableView().getItems().get(getIndex()), "REJECTED"));
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) { setGraphic(null); return; }
                boolean isPending = "PENDING".equals(getTableView().getItems().get(idx).getStatus());
                approveBtn.setDisable(!isPending);
                rejectBtn.setDisable(!isPending);
                setGraphic(box);
            }
        });
    }

    private void processMembership(CoopMembershipRequest req, String newStatus) {
        // Re-fetch to prevent stale reference
        Optional<CoopMembershipRequest> fresh = ds.getAllMembershipRequests().stream()
                .filter(r -> r.getId().equals(req.getId())).findFirst();
        if (fresh.isEmpty()) return;
        CoopMembershipRequest current = fresh.get();

        if (!"PENDING".equals(current.getStatus())) {
            AlertUtil.showWarning("Already Processed",
                    "This request has already been " + current.getStatus().toLowerCase() + ".");
            refreshMemberships();
            return;
        }

        String farmerName = ds.findUserById(current.getFarmerId())
                .map(User::getName).orElse(current.getFarmerId());
        String action = "APPROVED".equals(newStatus) ? "approve" : "reject";
        if (!AlertUtil.showConfirm("Membership Decision",
                "Are you sure you want to " + action + " " + farmerName + "'s membership request?")) return;

        current.setStatus(newStatus);
        ds.updateMembershipRequest(current);

        if ("APPROVED".equals(newStatus)) {
            // Add farmer to cooperative member list
            ds.findCoopById(current.getCoopId()).ifPresent(coop -> {
                if (!coop.getMemberIds().contains(current.getFarmerId())) {
                    coop.getMemberIds().add(current.getFarmerId());
                    ds.updateCooperative(coop);
                }
            });
            // Update farmer's primary coopId
            ds.findUserById(current.getFarmerId()).ifPresent(u -> {
                if (u instanceof Farmer f && (f.getCoopId() == null || f.getCoopId().isEmpty())) {
                    f.setCoopId(current.getCoopId());
                    ds.updateUser(f);
                }
            });
            AlertUtil.showInfo("Approved",
                    farmerName + " has been approved and added to the cooperative.");
        } else {
            AlertUtil.showInfo("Rejected",
                    farmerName + "'s membership request has been rejected.");
        }

        refreshMemberships();
    }
}
