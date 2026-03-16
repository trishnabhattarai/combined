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

public class FarmerCooperativesController {

    // My cooperatives table
    @FXML private TableView<Cooperative> myCoopsTable;
    @FXML private TableColumn<Cooperative, String> colMyCoopId;
    @FXML private TableColumn<Cooperative, String> colMyCoopName;
    @FXML private TableColumn<Cooperative, String> colMyDistrict;
    @FXML private TableColumn<Cooperative, String> colMyMembers;

    // Available cooperatives table
    @FXML private TableView<Cooperative> availableCoopsTable;
    @FXML private TableColumn<Cooperative, String> colAvailId;
    @FXML private TableColumn<Cooperative, String> colAvailName;
    @FXML private TableColumn<Cooperative, String> colAvailDistrict;
    @FXML private TableColumn<Cooperative, String> colAvailMembers;
    @FXML private TableColumn<Cooperative, String> colAvailAction;

    // Requests table
    @FXML private TableView<CoopMembershipRequest> requestsTable;
    @FXML private TableColumn<CoopMembershipRequest, String> colReqId;
    @FXML private TableColumn<CoopMembershipRequest, String> colReqCoop;
    @FXML private TableColumn<CoopMembershipRequest, String> colReqStatus;
    @FXML private TableColumn<CoopMembershipRequest, String> colReqDate;

    private final DataService ds = DataService.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @FXML
    public void initialize() {
        setupMyCoopsColumns();
        setupAvailableCoopsColumns();
        setupRequestsColumns();
        refresh();
    }

    private void refresh() {
        User user = SessionManager.getInstance().getCurrentUser();
        String farmerId = user.getId();

        // My cooperatives
        List<Cooperative> myCoops = ds.getAllCooperatives().stream()
                .filter(c -> c.getMemberIds().contains(farmerId)).toList();
        myCoopsTable.setItems(FXCollections.observableArrayList(myCoops));

        // Available (not already a member, no pending request)
        List<Cooperative> available = ds.getAllCooperatives().stream()
                .filter(c -> !c.getMemberIds().contains(farmerId))
                .filter(c -> !ds.hasPendingMembershipRequest(farmerId, c.getId()))
                .toList();
        availableCoopsTable.setItems(FXCollections.observableArrayList(available));

        // My requests
        List<CoopMembershipRequest> requests = ds.getMembershipRequestsByFarmer(farmerId);
        requestsTable.setItems(FXCollections.observableArrayList(requests));
    }

    private void setupMyCoopsColumns() {
        colMyCoopId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colMyCoopName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colMyDistrict.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findDistrictById(c.getValue().getDistrictId()).map(District::getName).orElse("—")));
        colMyMembers.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getMemberIds().size())));
    }

    private void setupAvailableCoopsColumns() {
        colAvailId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colAvailName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colAvailDistrict.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findDistrictById(c.getValue().getDistrictId()).map(District::getName).orElse("—")));
        colAvailMembers.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getMemberIds().size())));

        colAvailAction.setCellFactory(col -> new TableCell<>() {
            private final Button joinBtn = new Button("Request to Join");
            {
                joinBtn.getStyleClass().add("btn-sm");
                joinBtn.setOnAction(e -> {
                    Cooperative coop = getTableView().getItems().get(getIndex());
                    requestJoin(coop);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : joinBtn);
            }
        });
    }

    private void setupRequestsColumns() {
        colReqId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colReqCoop.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findCoopById(c.getValue().getCoopId()).map(Cooperative::getName).orElse(c.getValue().getCoopId())));
        colReqStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        colReqDate.setCellValueFactory(c -> new SimpleStringProperty(
                sdf.format(new Date(c.getValue().getRequestDate()))));

        colReqStatus.setCellFactory(col -> new TableCell<>() {
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
    }

    private void requestJoin(Cooperative coop) {
        User user = SessionManager.getInstance().getCurrentUser();
        // Double-check no duplicate pending request
        if (ds.hasPendingMembershipRequest(user.getId(), coop.getId())) {
            AlertUtil.showWarning("Already Requested",
                    "You already have a pending request for " + coop.getName() + ".");
            return;
        }
        if (coop.getMemberIds().contains(user.getId())) {
            AlertUtil.showWarning("Already a Member", "You are already a member of " + coop.getName() + ".");
            return;
        }
        if (AlertUtil.showConfirm("Request Membership",
                "Send a membership request to " + coop.getName() + "?")) {
            CoopMembershipRequest req = new CoopMembershipRequest(
                    ds.nextMembershipRequestId(),
                    user.getId(), coop.getId(),
                    "PENDING", System.currentTimeMillis(), "");
            ds.addMembershipRequest(req);
            refresh();
            AlertUtil.showInfo("Request Sent",
                    "Your membership request has been sent to " + coop.getName() + ". The officer will review it.");
        }
    }
}
