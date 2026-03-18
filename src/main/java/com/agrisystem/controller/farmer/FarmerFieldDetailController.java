package com.agrisystem.controller.farmer;

import com.agrisystem.dao.PlotTaskDAO;
import com.agrisystem.model.*;
import com.agrisystem.model.PlotTask.Status;
import com.agrisystem.service.DataService;
import com.agrisystem.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for the Field Detail screen.
 *
 * Shows all plots of a field (max 9) and a Kanban board
 * (To Do / Doing / Done) for the selected plot.
 * Only visible when the field has at least one crop assigned.
 */
public class FarmerFieldDetailController {

    // ── FXML references ───────────────────────────────────────────────────────
    @FXML private Label  fieldTitleLabel;     // e.g. "Field FIE-001"
    @FXML private Label  fieldCropLabel;      // e.g. "Crops: Maize, Wheat"
    @FXML private Label  fieldAreaLabel;      // e.g. "Area: 2.5 ha"
    @FXML private Button backBtn;

    @FXML private HBox   plotBar;             // horizontal strip of plot buttons
    @FXML private Button addPlotBtn;
    @FXML private Label  plotCountLabel;

    @FXML private Label  selectedPlotLabel;   // "Plot 2 tasks"
    @FXML private Button addTaskBtn;

    // Kanban columns
    @FXML private VBox   todoColumn;
    @FXML private VBox   doingColumn;
    @FXML private VBox   doneColumn;

    // ── State ─────────────────────────────────────────────────────────────────
    private Field        field;
    private int          selectedPlot = 1;    // currently viewed plot
    private int          totalPlots   = 0;    // how many plots this field has
    private FarmerMainController mainController;

    private final PlotTaskDAO taskDAO = PlotTaskDAO.getInstance();
    private final DataService ds      = DataService.getInstance();

    private static final int MAX_PLOTS = 9;

    // ── Wiring ────────────────────────────────────────────────────────────────

    /** Called by FarmerFieldsController before loading this view. */
    public void setField(Field field, FarmerMainController mainController) {
        this.field          = field;
        this.mainController = mainController;
        initView();
    }

    @FXML
    public void initialize() {
        // Populated by setField(); nothing to do here before data is injected.
    }

    // ── Initialise the view once field data is available ──────────────────────

    private void initView() {
        // Header labels
        fieldTitleLabel.setText("Field: " + field.getId());

        String cropNames = field.getCropIds().stream()
                .map(cid -> ds.findCropById(cid).map(Crop::getName).orElse(cid))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
        fieldCropLabel.setText("Crops: " + (cropNames.isEmpty() ? "—" : cropNames));
        fieldAreaLabel.setText("Area: " + field.getArea() + " ha");

        // Determine how many distinct plots already have tasks
        List<PlotTask> allTasks = taskDAO.findByField(field.getId());
        totalPlots = allTasks.stream()
                .mapToInt(PlotTask::getPlotNumber)
                .max().orElse(0);

        renderPlotBar();
        selectPlot(totalPlots > 0 ? 1 : 0);
    }

    // ── Plot bar ──────────────────────────────────────────────────────────────

    private void renderPlotBar() {
        plotBar.getChildren().clear();
        for (int i = 1; i <= totalPlots; i++) {
            final int plotNum = i;
            Button btn = new Button("Plot " + i);
            btn.getStyleClass().add(plotNum == selectedPlot ? "btn-primary" : "btn-secondary");
            btn.setOnAction(e -> selectPlot(plotNum));
            plotBar.getChildren().add(btn);
        }
        plotCountLabel.setText(totalPlots + " / " + MAX_PLOTS + " plots");
        addPlotBtn.setDisable(totalPlots >= MAX_PLOTS);
    }

    private void selectPlot(int plotNum) {
        if (plotNum < 1) {
            // No plots yet — show empty state
            selectedPlotLabel.setText("No plots yet. Click '+ Add Plot' to create one.");
            clearKanban();
            addTaskBtn.setDisable(true);
            return;
        }
        selectedPlot = plotNum;
        selectedPlotLabel.setText("Plot " + plotNum + " — Tasks");
        addTaskBtn.setDisable(false);
        renderPlotBar(); // re-render to update active button style
        renderKanban();
    }

    // ── Kanban board ──────────────────────────────────────────────────────────

    private void renderKanban() {
        clearKanban();
        List<PlotTask> tasks = taskDAO.findByFieldAndPlot(field.getId(), selectedPlot);

        for (PlotTask task : tasks) {
            VBox card = buildTaskCard(task);
            switch (task.getStatus()) {
                case TODO  -> todoColumn.getChildren().add(card);
                case DOING -> doingColumn.getChildren().add(card);
                case DONE  -> doneColumn.getChildren().add(card);
            }
        }
    }

    private void clearKanban() {
        todoColumn.getChildren().clear();
        doingColumn.getChildren().clear();
        doneColumn.getChildren().clear();
    }

    /**
     * Builds a task card.
     * Clicking a TODO card moves it to DOING.
     * Clicking a DOING card moves it to DONE.
     * DONE cards show a delete button only.
     */
    private VBox buildTaskCard(PlotTask task) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMaxWidth(Double.MAX_VALUE);

        // Card colour by status
        String bg = switch (task.getStatus()) {
            case TODO  -> "#FFF8E1";
            case DOING -> "#E3F2FD";
            case DONE  -> "#E8F5E9";
        };
        String border = switch (task.getStatus()) {
            case TODO  -> "#FFD54F";
            case DOING -> "#64B5F6";
            case DONE  -> "#81C784";
        };
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8;" +
                      "-fx-border-color: " + border + "; -fx-border-radius: 8;" +
                      "-fx-cursor: hand;");

        Label titleLbl = new Label(task.getTitle());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        titleLbl.setWrapText(true);

        Label descLbl = new Label(task.getDescription());
        descLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        descLbl.setWrapText(true);

        Label statusLbl = new Label(statusEmoji(task.getStatus()) + " " + task.getStatus().name());
        statusLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_RIGHT);

        // Advance button (only for TODO and DOING)
        if (task.getStatus() != Status.DONE) {
            String btnText = task.getStatus() == Status.TODO ? "▶ Start" : "✓ Done";
            Button advBtn = new Button(btnText);
            advBtn.getStyleClass().addAll("btn-sm",
                    task.getStatus() == Status.TODO ? "btn-secondary" : "btn-primary");
            advBtn.setOnAction(e -> {
                task.advance();
                taskDAO.update(task);
                renderKanban();
            });
            footer.getChildren().add(advBtn);
        }

        // Delete button always visible
        Button delBtn = new Button("✕");
        delBtn.getStyleClass().addAll("btn-sm", "btn-danger-sm");
        delBtn.setOnAction(e -> {
            if (AlertUtil.showConfirm("Delete Task", "Delete task \"" + task.getTitle() + "\"?")) {
                taskDAO.delete(task.getId());
                renderKanban();
            }
        });
        footer.getChildren().add(delBtn);

        card.getChildren().addAll(titleLbl, descLbl, statusLbl, footer);
        return card;
    }

    private String statusEmoji(Status s) {
        return switch (s) {
            case TODO  -> "📋";
            case DOING -> "⚙️";
            case DONE  -> "✅";
        };
    }

    // ── FXML actions ─────────────────────────────────────────────────────────

    @FXML
    private void addPlot() {
        if (totalPlots >= MAX_PLOTS) {
            AlertUtil.showWarning("Plot Limit", "Maximum " + MAX_PLOTS + " plots per field.");
            return;
        }
        totalPlots++;
        renderPlotBar();
        selectPlot(totalPlots);
    }

    @FXML
    private void addTask() {
        // Dialog to enter task title and description
        Dialog<PlotTask> dialog = new Dialog<>();
        dialog.setTitle("Add Task to Plot " + selectedPlot);
        dialog.setHeaderText("New task for " + field.getId() + " — Plot " + selectedPlot);

        ButtonType addBtnType = new ButtonType("Add Task", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Apply fertiliser");
        titleField.setPrefWidth(260);

        TextArea descField = new TextArea();
        descField.setPromptText("Optional description...");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);

        grid.add(new Label("Title *"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Disable Add button until title is not empty
        dialog.getDialogPane().lookupButton(addBtnType).setDisable(true);
        titleField.textProperty().addListener((obs, o, n) ->
                dialog.getDialogPane().lookupButton(addBtnType).setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn == addBtnType) {
                PlotTask t = new PlotTask(null, field.getId(), selectedPlot,
                        titleField.getText().trim(),
                        descField.getText().trim(),
                        Status.TODO);
                return t;
            }
            return null;
        });

        Optional<PlotTask> result = dialog.showAndWait();
        result.ifPresent(t -> {
            taskDAO.save(t);
            renderKanban();
        });
    }

    @FXML
    private void goBack() {
        // Navigate back to FarmerFields screen
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/agrisystem/fxml/farmer/FarmerFields.fxml"));
            javafx.scene.Node content = loader.load();
            // Get the StackPane contentArea from the parent (FarmerMainController)
            javafx.scene.Parent parent = backBtn.getParent();
            while (parent != null && !(parent instanceof StackPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof StackPane sp) {
                sp.getChildren().setAll(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
