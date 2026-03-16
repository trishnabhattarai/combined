package com.agrisystem.controller.farmer;

import com.agrisystem.model.*;
import com.agrisystem.service.AuditLogService;
import com.agrisystem.service.DataService;
import com.agrisystem.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FarmerProfileController {

    // ── Profile card ──────────────────────────────────────────────────────────
    @FXML private ImageView profileImage;
    @FXML private Label     profileInitial;
    @FXML private TextField nameField;
    @FXML private Label     idLabel;
    @FXML private Label     emailLabel;
    @FXML private Label     districtLabel;

    // ── Crops table ───────────────────────────────────────────────────────────
    @FXML private TableView<CropRow>              cropsTable;
    @FXML private TableColumn<CropRow, String>    colFieldId;
    @FXML private TableColumn<CropRow, String>    colCropId;
    @FXML private TableColumn<CropRow, String>    colCropName;

    // ── Cooperatives list ─────────────────────────────────────────────────────
    @FXML private VBox coopsContainer;

    // ── Membership requests table ─────────────────────────────────────────────
    @FXML private TableView<CoopMembershipRequest>              membershipTable;
    @FXML private TableColumn<CoopMembershipRequest, String>    colMbrReqId;
    @FXML private TableColumn<CoopMembershipRequest, String>    colMbrReqCoop;
    @FXML private TableColumn<CoopMembershipRequest, String>    colMbrReqDate;
    @FXML private TableColumn<CoopMembershipRequest, String>    colMbrReqStat;

    private final DataService ds = DataService.getInstance();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private FarmerMainController mainController;

    /** Called by FarmerMainController after loading this view. */
    public void setMainController(FarmerMainController mainController) {
        this.mainController = mainController;
    }

    public record CropRow(String fieldId, String cropId, String cropName) {}

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        nameField.setText(user.getName());
        idLabel.setText(user.getId());
        emailLabel.setText(user.getEmail().isEmpty() ? user.getNumber() : user.getEmail());
        profileInitial.setText(user.getName().substring(0, 1).toUpperCase());

        loadProfileImage(user);

        if (user instanceof Farmer farmer) {
            String distName = ds.findDistrictById(farmer.getDistrictId())
                    .map(District::getName).orElse("—");
            districtLabel.setText(distName);
            setupCropsTable(farmer);
        }

        loadCoops();
        setupMembershipTable();
        refreshMembershipRequests();
    }

    // ── Profile image ─────────────────────────────────────────────────────────

    private void loadProfileImage(User user) {
        File file = new File("assets/images/" + user.getId() + ".png");
        if (file.exists()) {
            profileImage.setImage(new Image(file.toURI().toString()));
            profileInitial.setVisible(false);
        } else {
            profileInitial.setVisible(true);
        }
    }

    // ── Crops ─────────────────────────────────────────────────────────────────

    private void setupCropsTable(Farmer farmer) {
        colFieldId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fieldId()));
        colCropId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cropId()));
        colCropName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().cropName()));

        // Build one row per (field, crop) pair from the farmer's fields
        List<Field> farmerFields = ds.getFieldsByFarmer(farmer.getId());
        List<CropRow> rows = new ArrayList<>();
        for (Field field : farmerFields) {
            for (String cropId : field.getCropIds()) {
                String cropName = ds.findCropById(cropId).map(Crop::getName).orElse(cropId);
                rows.add(new CropRow(field.getId(), cropId, cropName));
            }
        }

        cropsTable.setItems(FXCollections.observableArrayList(rows));
    }

    // ── Cooperatives list ─────────────────────────────────────────────────────

    private void loadCoops() {
        User user = SessionManager.getInstance().getCurrentUser();
        coopsContainer.getChildren().clear();
        List<Cooperative> myCoops = ds.getAllCooperatives().stream()
                .filter(c -> c.getMemberIds().contains(user.getId())).toList();

        if (myCoops.isEmpty()) {
            Label none = new Label("Not a member of any cooperative yet.");
            none.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            coopsContainer.getChildren().add(none);
        } else {
            for (Cooperative coop : myCoops) {
                Label l = new Label("✔  " + coop.getName() + " (" + coop.getId() + ")");
                l.setStyle("-fx-font-size: 13px; -fx-text-fill: #2E7D32;");
                coopsContainer.getChildren().add(l);
            }
        }
    }

    // ── Membership requests ───────────────────────────────────────────────────

    private void setupMembershipTable() {
        colMbrReqId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colMbrReqCoop.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findCoopById(c.getValue().getCoopId())
                  .map(Cooperative::getName).orElse(c.getValue().getCoopId())));
        colMbrReqDate.setCellValueFactory(c -> new SimpleStringProperty(
                sdf.format(new Date(c.getValue().getRequestDate()))));
        colMbrReqStat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        colMbrReqStat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = "APPROVED".equals(item) ? "#4CAF50"
                             : "REJECTED".equals(item) ? "#F44336" : "#FF9800";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });
    }

    private void refreshMembershipRequests() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<CoopMembershipRequest> requests = ds.getMembershipRequestsByFarmer(user.getId());
        membershipTable.setItems(FXCollections.observableArrayList(requests));
        loadCoops(); // refresh coop list in case a request was just approved
    }

    @FXML
    private void requestMembership() {
        User user = SessionManager.getInstance().getCurrentUser();

        // Show all cooperatives the farmer is NOT already a member of
        List<Cooperative> available = ds.getAllCooperatives().stream()
                .filter(c -> !c.getMemberIds().contains(user.getId()))
                .filter(c -> !ds.hasPendingMembershipRequest(user.getId(), c.getId()))
                .toList();

        if (available.isEmpty()) {
            AlertUtil.showInfo("No Cooperatives Available",
                    "You are already a member of (or have a pending request for) all cooperatives.");
            return;
        }

        ChoiceDialog<Cooperative> dialog = new ChoiceDialog<>(available.get(0), available);
        dialog.setTitle("Join Cooperative");
        dialog.setHeaderText("Request membership to a cooperative");
        dialog.setContentText("Select cooperative:");

        dialog.showAndWait().ifPresent(coop -> {
            // Double-check no duplicate
            if (ds.hasPendingMembershipRequest(user.getId(), coop.getId())) {
                AlertUtil.showWarning("Already Requested",
                        "You already have a pending request for " + coop.getName() + ".");
                return;
            }
            if (coop.getMemberIds().contains(user.getId())) {
                AlertUtil.showInfo("Already a Member",
                        "You are already a member of " + coop.getName() + ".");
                return;
            }

            CoopMembershipRequest req = new CoopMembershipRequest(
                    ds.nextMembershipRequestId(),
                    user.getId(),
                    coop.getId(),
                    "PENDING",
                    System.currentTimeMillis(),
                    "");
            ds.addMembershipRequest(req);
            refreshMembershipRequests();
            AlertUtil.showInfo("Request Submitted",
                    "Your membership request for " + coop.getName() +
                    " has been sent to the officer for review.");
        });
    }

    // ── Photo & Save ──────────────────────────────────────────────────────────

    @FXML
    private void changePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(nameField.getScene().getWindow());
        if (file == null) return;

        User user = SessionManager.getInstance().getCurrentUser();
        String destPath = "assets/images/" + user.getId() + ".png";
        new File("assets/images").mkdirs();
        try {
            Files.copy(file.toPath(), Path.of(destPath), StandardCopyOption.REPLACE_EXISTING);
            profileImage.setImage(new Image(file.toURI().toString()));
            profileInitial.setVisible(false);
            AlertUtil.showInfo("Photo Updated", "Profile photo saved successfully.");
        } catch (IOException e) {
            AlertUtil.showError("Error", "Could not save photo: " + e.getMessage());
        }
    }

    @FXML
    private void saveProfile() {
        User user = SessionManager.getInstance().getCurrentUser();
        String name = nameField.getText().trim();
        if (name.isEmpty()) { AlertUtil.showError("Error", "Name cannot be empty."); return; }
        user.setName(name);
        ds.updateUser(user);
        SessionManager.getInstance().setCurrentUser(user);
        profileInitial.setText(name.substring(0, 1).toUpperCase());
        // Refresh parent sidebar so the new name appears immediately
        if (mainController != null) mainController.refreshSidebarName();
        AuditLogService.getInstance().log(user.getId(), user.getName(), "PROFILE_UPDATE", "User updated their name");
        AlertUtil.showInfo("Saved", "Profile updated successfully.");
    }
}
