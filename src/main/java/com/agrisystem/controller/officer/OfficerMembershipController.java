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

public class OfficerMembershipController {

    @FXML private TableView<CoopMembershipRequest> requestsTable;
    @FXML private TableColumn<CoopMembershipRequest, String> colId;
    @FXML private TableColumn<CoopMembershipRequest, String> colFarmer;
    @FXML private TableColumn<CoopMembershipRequest, String> colDate;
    @FXML private TableColumn<CoopMembershipRequest, String> colStatus;
    @FXML private TableColumn<CoopMembershipRequest, String> colActions;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label pendingCountLabel;

    private final DataService ds = DataService.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private String coopId;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user instanceof Officer officer) coopId = officer.getCoopId();

        statusFilter.setItems(FXCollections.observableArrayList("All", "PENDING", "APPROVED", "REJECTED"));
        statusFilter.setValue("PENDING");
        setupColumns();
        filterRequests();
    }

    @FXML
    public void filterRequests() {
        String statusVal = statusFilter.getValue();
        List<CoopMembershipRequest> all = coopId != null
                ? ds.getMembershipRequestsByCoop(coopId)
                : ds.getAllMembershipRequests();

        List<CoopMembershipRequest> filtered = all.stream()
                .filter(r -> "All".equals(statusVal) || r.getStatus().equals(statusVal))
                .collect(Collectors.toList());

        requestsTable.setItems(FXCollections.observableArrayList(filtered));

        long pending = all.stream().filter(r -> "PENDING".equals(r.getStatus())).count();
        pendingCountLabel.setText(pending + " pending");
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colFarmer.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findUserById(c.getValue().getFarmerId())
                        .map(u -> u.getName() + " (" + u.getId() + ")")
                        .orElse(c.getValue().getFarmerId())));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                sdf.format(new Date(c.getValue().getRequestDate()))));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "APPROVED" -> "#4CAF50";
                    case "REJECTED" -> "#F44336";
                    default -> "#FF9800";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn  = new Button("Reject");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, approveBtn, rejectBtn);
            {
                approveBtn.getStyleClass().addAll("btn-sm", "btn-success-sm");
                rejectBtn.getStyleClass().addAll("btn-sm", "btn-danger-sm");
                box.setPadding(new Insets(4));
                approveBtn.setOnAction(e -> processRequest(getTableView().getItems().get(getIndex()), "APPROVED"));
                rejectBtn.setOnAction(e ->  processRequest(getTableView().getItems().get(getIndex()), "REJECTED"));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                CoopMembershipRequest req = getTableView().getItems().get(getIndex());
                boolean isPending = "PENDING".equals(req.getStatus());
                approveBtn.setDisable(!isPending);
                rejectBtn.setDisable(!isPending);
                setGraphic(box);
            }
        });
    }

    private void processRequest(CoopMembershipRequest req, String newStatus) {
        // Guard: re-check status is still PENDING
        ds.getAllMembershipRequests().stream()
                .filter(r -> r.getId().equals(req.getId())).findFirst()
                .ifPresent(current -> {
                    if (!"PENDING".equals(current.getStatus())) {
                        AlertUtil.showWarning("Already Processed",
                                "This request has already been " + current.getStatus().toLowerCase() + ".");
                        filterRequests();
                        return;
                    }
                    String verb = "APPROVED".equals(newStatus) ? "approve" : "reject";
                    String farmerName = ds.findUserById(current.getFarmerId())
                            .map(User::getName).orElse(current.getFarmerId());

                    if (AlertUtil.showConfirm("Membership Decision",
                            "Are you sure you want to " + verb + " " + farmerName + "'s request?")) {

                        current.setStatus(newStatus);
                        ds.updateMembershipRequest(current);

                        // If approved, add farmer to cooperative
                        if ("APPROVED".equals(newStatus)) {
                            ds.findCoopById(current.getCoopId()).ifPresent(coop -> {
                                if (!coop.getMemberIds().contains(current.getFarmerId())) {
                                    coop.getMemberIds().add(current.getFarmerId());
                                    ds.updateCooperative(coop);
                                }
                                // Also update farmer's coopId field
                                ds.findUserById(current.getFarmerId()).ifPresent(u -> {
                                    if (u instanceof Farmer f) {
                                        f.setCoopId(coop.getId());
                                        f.setDistrictId(coop.getDistrictId());
                                        if (!coop.getMarketIds().isEmpty())
                                            f.setMarketId(coop.getMarketIds().get(0));
                                        ds.updateUser(f);
                                    }
                                });
                            });
                            AlertUtil.showInfo("Approved",
                                    farmerName + " has been added to your cooperative.");
                        } else {
                            AlertUtil.showInfo("Rejected",
                                    farmerName + "'s membership request has been rejected.");
                        }
                        filterRequests();
                    }
                });
    }
}
