package com.agrisystem.controller.admin;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.*;
import java.util.stream.Collectors;

public class AdminManageUsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colType;
    @FXML private TableColumn<User, String> colActions;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label pageLabel;

    private static final int PAGE_SIZE = 25;
    private int currentPage = 0;
    private List<User> filteredUsers = new ArrayList<>();
    private final DataService ds = DataService.getInstance();

    @FXML
    public void initialize() {
        typeFilter.setItems(FXCollections.observableArrayList("All", "FARMER", "OFFICER", "ADMIN"));
        typeFilter.setValue("All");
        setupColumns();
        filterUsers();
    }

    @FXML
    public void filterUsers() {
        String query = searchField.getText().trim().toLowerCase();
        String type = typeFilter.getValue();

        filteredUsers = ds.getAllUsers().stream()
                .filter(u -> {
                    boolean matchType = "All".equals(type) || type == null || u.getType().equalsIgnoreCase(type);
                    boolean matchQuery = query.isEmpty()
                            || u.getName().toLowerCase().contains(query)
                            || u.getEmail().toLowerCase().contains(query)
                            || u.getId().toLowerCase().contains(query);
                    return matchType && matchQuery;
                })
                .collect(Collectors.toList());
        currentPage = 0;
        renderPage();
    }

    private void renderPage() {
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredUsers.size());
        int total = (int) Math.ceil((double) filteredUsers.size() / PAGE_SIZE);
        usersTable.setItems(FXCollections.observableArrayList(filteredUsers.subList(start, end)));
        pageLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(1, total));
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= total - 1);
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmail().isEmpty() ? c.getValue().getNumber() : c.getValue().getEmail()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));

        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color = switch (item) {
                    case "ADMIN" -> "#9C27B0";
                    case "OFFICER" -> "#2196F3";
                    default -> "#4CAF50";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("btn-sm");
                delBtn.getStyleClass().addAll("btn-sm", "btn-danger-sm");
                box.setPadding(new Insets(4));

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    showEditUser(user);
                });
                delBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        usersTable.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showEditUser(row.getItem());
            });
            return row;
        });
    }

    private void showEditUser(User user) {
        // ── Build fields ─────────────────────────────────────────────────────
        TextField nameF  = new TextField(user.getName());
        TextField emailF = new TextField(user.getEmail());
        PasswordField passF = new PasswordField();
        passF.setPromptText("New password (leave blank to keep)");

        ComboBox<String> typeBox = new ComboBox<>(
                FXCollections.observableArrayList("FARMER", "OFFICER", "ADMIN"));
        typeBox.setValue(user.getType());
        typeBox.setMaxWidth(Double.MAX_VALUE);
        nameF.setMaxWidth(Double.MAX_VALUE);
        emailF.setMaxWidth(Double.MAX_VALUE);
        passF.setMaxWidth(Double.MAX_VALUE);

        // Cooperative row — only shown/used when type is OFFICER
        Label coopLabel = new Label("Assign Cooperative:");
        ComboBox<Cooperative> coopBox = new ComboBox<>(
                FXCollections.observableArrayList(ds.getAllCooperatives()));
        coopBox.setPromptText("Select cooperative");
        coopBox.setMaxWidth(Double.MAX_VALUE);

        // Pre-select the officer's current coop (if editing an existing officer)
        if (user instanceof Officer o && o.getCoopId() != null) {
            ds.findCoopById(o.getCoopId()).ifPresent(coopBox::setValue);
        }

        // Show/hide coop row based on current type selection
        coopLabel.managedProperty().bind(coopLabel.visibleProperty());
        coopBox.managedProperty().bind(coopBox.visibleProperty());
        boolean isOfficerNow = "OFFICER".equals(user.getType());
        coopLabel.setVisible(isOfficerNow);
        coopBox.setVisible(isOfficerNow);
        typeBox.setOnAction(e -> {
            boolean show = "OFFICER".equals(typeBox.getValue());
            coopLabel.setVisible(show);
            coopBox.setVisible(show);
        });

        // ── Inline error label ────────────────────────────────────────────────
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #F44336; -fx-font-size: 12px;");
        errorLbl.setWrapText(true);
        errorLbl.setVisible(false);
        errorLbl.managedProperty().bind(errorLbl.visibleProperty());

        // ── Buttons ───────────────────────────────────────────────────────────
        Button updateBtn = new Button("Update User");
        Button cancelBtn = new Button("Cancel");
        updateBtn.getStyleClass().add("btn-primary");
        cancelBtn.getStyleClass().add("btn-secondary");
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox btnRow =
                new javafx.scene.layout.HBox(10, cancelBtn, updateBtn);
        btnRow.setPadding(new Insets(8, 0, 0, 0));
        javafx.scene.layout.HBox.setHgrow(updateBtn, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(cancelBtn, javafx.scene.layout.Priority.ALWAYS);

        // ── Title label ───────────────────────────────────────────────────────
        Label titleLbl = new Label("Edit User — " + user.getId());
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1B5E20;");

        // ── Layout ────────────────────────────────────────────────────────────
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10,
                titleLbl,
                new Label("Name:"),          nameF,
                new Label("Email:"),         emailF,
                new Label("New Password:"),  passF,
                new Label("User Type:"),     typeBox,
                coopLabel, coopBox,
                errorLbl,
                btnRow);
        content.setPadding(new Insets(20));
        content.setPrefWidth(370);

        // ── Custom Stage (instead of Dialog, so our CSS buttons render correctly) ──
        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        stage.initOwner(usersTable.getScene().getWindow());
        stage.setTitle("Edit User");
        stage.setResizable(false);
        javafx.scene.Scene scene = new javafx.scene.Scene(content);
        scene.getStylesheets().add(
                getClass().getResource("/com/agrisystem/css/main.css").toExternalForm());
        stage.setScene(scene);

        cancelBtn.setOnAction(e -> stage.close());

        updateBtn.setOnAction(e -> {
            String newName     = nameF.getText().trim();
            String newEmail    = emailF.getText().trim();
            String newPassword = passF.getText();
            String newType     = typeBox.getValue();

            if (newName.isEmpty()) {
                errorLbl.setText("Name cannot be empty.");
                errorLbl.setVisible(true);
                return;
            }

            // ── Build the correct User subclass ───────────────────────────────
            User updated = convertUserType(user, newType);
            updated.setName(newName);
            updated.setEmail(newEmail);
            if (!newPassword.isEmpty()) updated.setPassword(newPassword);

            // Assign cooperative when type is OFFICER
            if ("OFFICER".equals(newType)) {
                Cooperative selectedCoop = coopBox.getValue();
                if (selectedCoop == null) {
                    errorLbl.setText("An Officer must be assigned to a cooperative.");
                    errorLbl.setVisible(true);
                    return;
                }
                Officer officer = (Officer) updated;
                String oldCoopId = officer.getCoopId();

                // Remove from old coop's officer slot if changed
                if (oldCoopId != null && !oldCoopId.equals(selectedCoop.getId())) {
                    ds.findCoopById(oldCoopId).ifPresent(oldCoop -> {
                        if (officer.getId().equals(oldCoop.getOfficerId())) {
                            oldCoop.setOfficerId(null);
                            ds.updateCooperative(oldCoop);
                        }
                    });
                }
                officer.setCoopId(selectedCoop.getId());
                selectedCoop.setOfficerId(officer.getId());
                ds.updateCooperative(selectedCoop);
            }

            ds.updateUser(updated);
            filterUsers();
            stage.close();
            AlertUtil.showInfo("Saved", "User updated successfully.");
        });

        stage.showAndWait();
    }

    /**
     * Converts a User to the target type subclass, carrying over
     * all common fields. If the type is unchanged and already correct,
     * the same object is returned.
     */
    private User convertUserType(User source, String targetType) {
        // Already the right type — no conversion needed
        if (source.getType().equalsIgnoreCase(targetType)) return source;

        String id       = source.getId();
        String name     = source.getName();
        String email    = source.getEmail();
        String number   = source.getNumber();
        String password = source.getPassword();

        return switch (targetType) {
            case "ADMIN" -> new Admin(id, name, email, password);

            case "OFFICER" -> {
                // Carry over coopId if source was already an officer
                String existingCoopId = (source instanceof Officer o) ? o.getCoopId() : null;
                yield new Officer(id, name, email, number, password, existingCoopId);
            }

            case "FARMER" -> {
                // Carry over balance if source was already a farmer
                double balance = (source instanceof Farmer f) ? f.getBalance() : 5000.0;
                double score   = (source instanceof Farmer f2) ? f2.getResponsibilityScore() : 50.0;
                yield new Farmer(id, name, email, number, password,
                        null, new ArrayList<>(), null, null, balance, score);
            }

            default -> source;
        };
    }

    private void deleteUser(User user) {
        if (AlertUtil.showConfirm("Delete User",
                "Delete " + user.getName() + "? This will remove all their loans, fields, and market records.")) {
            ds.deleteUser(user.getId());
            filterUsers();
            AlertUtil.showInfo("Deleted", "User removed successfully.");
        }
    }

    @FXML private void prevPage() { if (currentPage > 0) { currentPage--; renderPage(); } }
    @FXML private void nextPage() {
        int total = (int) Math.ceil((double) filteredUsers.size() / PAGE_SIZE);
        if (currentPage < total - 1) { currentPage++; renderPage(); }
    }
}
