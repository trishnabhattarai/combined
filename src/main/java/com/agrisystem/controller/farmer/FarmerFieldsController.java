package com.agrisystem.controller.farmer;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.util.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;

public class FarmerFieldsController {

    @FXML private GridPane fieldGrid;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label pageLabel;
    @FXML private Button addFieldBtn;
    @FXML private Label fieldCountLabel;

    private static final int PAGE_SIZE = 9; // 3x3
    private int currentPage = 0;
    private List<Field> farmerFields;

    @FXML
    public void initialize() {
        refreshFields();
    }

    private void refreshFields() {
        User user = SessionManager.getInstance().getCurrentUser();
        farmerFields = DataService.getInstance().getFieldsByFarmer(user.getId());
        renderPage();
    }

    private void renderPage() {
        fieldGrid.getChildren().clear();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, farmerFields.size());
        int totalPages = (int) Math.ceil((double) farmerFields.size() / PAGE_SIZE);

        for (int i = start; i < end; i++) {
            Field field = farmerFields.get(i);
            VBox card = createFieldCard(field);
            int col = (i - start) % 3;
            int row = (i - start) / 3;
            fieldGrid.add(card, col, row);
        }

        pageLabel.setText("Page " + (currentPage + 1) + " of " + Math.max(1, totalPages));
        prevBtn.setDisable(currentPage == 0);
        nextBtn.setDisable(currentPage >= totalPages - 1);

        // Update count label and disable add button at limit
        int count = farmerFields.size();
        if (fieldCountLabel != null)
            fieldCountLabel.setText(count + " / 9 fields");
        if (addFieldBtn != null)
            addFieldBtn.setDisable(count >= 9);
    }

    private VBox createFieldCard(Field field) {
        VBox card = new VBox();
        card.setPrefSize(200, 180);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("field-card");

        // Background color based on crop presence
        boolean hasCrops = !field.getCropIds().isEmpty();

        // Crop info
        DataService ds = DataService.getInstance();
        String cropNames = field.getCropIds().stream()
                .map(cid -> ds.findCropById(cid).map(Crop::getName).orElse(cid))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);

        VBox info = new VBox(6);
        info.setPadding(new Insets(16));
        info.setStyle(hasCrops
                ? "-fx-background-color: linear-gradient(to bottom, #4CAF50, #2E7D32);"
                : "-fx-background-color: linear-gradient(to bottom, #78909C, #455A64);");
        info.setPrefHeight(90);

        Label idLabel = new Label(field.getId());
        idLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label cropLabel = new Label(cropNames.isEmpty() ? "No crops assigned" : cropNames);
        cropLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 11px;");
        cropLabel.setWrapText(true);
        info.getChildren().addAll(idLabel, cropLabel);

        // Hover overlay
        VBox details = new VBox(4);
        details.setPadding(new Insets(12));
        details.setStyle("-fx-background-color: rgba(0,0,0,0.05);");

        Label areaLabel = new Label(String.format("Area: %.1f ha", field.getArea()));
        areaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        HBox actions = new HBox(8);
        Button editBtn = new Button("Edit Crop");
        editBtn.getStyleClass().add("btn-sm");
        editBtn.setOnAction(e -> editFieldCrop(field));
        Button deleteBtn = new Button("Remove");
        deleteBtn.getStyleClass().addAll("btn-sm", "btn-danger-sm");
        deleteBtn.setOnAction(e -> deleteField(field));
        actions.getChildren().addAll(editBtn, deleteBtn);

        details.getChildren().addAll(areaLabel, actions);
        card.getChildren().addAll(info, details);

        return card;
    }

    @FXML
    private void addField() {
        User user = SessionManager.getInstance().getCurrentUser();
        List<Field> existing = DataService.getInstance().getFieldsByFarmer(user.getId());
        if (existing.size() >= 9) {
            AlertUtil.showWarning("Field Limit Reached",
                    "You can have a maximum of 9 fields. Remove an existing field to add a new one.");
            return;
        }
        String newId = IdGenerator.nextFieldId();
        Field field = new Field(newId, user.getId(), new java.util.ArrayList<>(), 1.0, "");
        DataService.getInstance().addField(field);
        refreshFields();
    }

    private void editFieldCrop(Field field) {
        DataService ds = DataService.getInstance();
        List<Crop> crops = ds.getAllCrops();

        ChoiceDialog<Crop> dialog = new ChoiceDialog<>(null, crops);
        dialog.setTitle("Assign Crop");
        dialog.setHeaderText("Assign a crop to " + field.getId());
        dialog.setContentText("Select crop:");
        dialog.showAndWait().ifPresent(crop -> {
            if (!field.getCropIds().contains(crop.getId()))
                field.getCropIds().add(crop.getId());
            ds.updateField(field);
            refreshFields();
        });
    }

    private void deleteField(Field field) {
        if (AlertUtil.showConfirm("Remove Field", "Are you sure you want to remove " + field.getId() + "?")) {
            DataService.getInstance().deleteField(field.getId());
            refreshFields();
        }
    }

    @FXML
    private void prevPage() { if (currentPage > 0) { currentPage--; renderPage(); } }

    @FXML
    private void nextPage() {
        int totalPages = (int) Math.ceil((double) farmerFields.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) { currentPage++; renderPage(); }
    }
}
