package com.agrisystem.controller.farmer;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.service.WeatherAlertService;
import com.agrisystem.service.WeatherAlertService.WeatherAlert;
import com.agrisystem.util.SessionManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class FarmerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label tempLabel;
    @FXML private Label conditionLabel;
    @FXML private Label fieldsLabel;
    @FXML private Label loansLabel;
    @FXML private Label balanceLabel;
    @FXML private LineChart<String, Number> temperatureChart;

    // Alert bell and notification panel
    @FXML private Label alertBadge;
    @FXML private VBox  alertsPanel;

    // Crop recommendations
    @FXML private Label cropRecoLabel;
    @FXML private VBox  cropRecoContainer;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Welcome back, " + user.getName() + "! 👋");

        DataService ds = DataService.getInstance();
        List<Field> fields = ds.getFieldsByFarmer(user.getId());
        List<Loan> loans = ds.getLoansByUser(user.getId()).stream()
                .filter(l -> l.getStatus().equals("PENDING")).toList();

        fieldsLabel.setText(String.valueOf(fields.size()));
        loansLabel.setText(String.valueOf(loans.size()));

        if (user instanceof Farmer f) {
            balanceLabel.setText(String.format("NRs %.2f", f.getBalance()));
        }

        // Weather
        List<Weather> weather = ds.getWeatherData();
        if (!weather.isEmpty()) {
            Weather today = weather.get(weather.size() - 1);
            tempLabel.setText(today.getTemperature() + "°C");
            conditionLabel.setText(today.getCondition());
        }

        // Generate alerts and update badge
        WeatherAlertService alertSvc = WeatherAlertService.getInstance();
        alertSvc.generateAlerts(weather);
        refreshAlertBadge(alertSvc);

        // Crop recommendations based on current temperature
        if (!weather.isEmpty()) {
            loadCropRecommendations(weather.get(weather.size() - 1).getTemperature());
        }

        // Chart
        loadTemperatureChart(weather);
    }

    private void loadTemperatureChart(List<Weather> weatherList) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Temperature");
        int start = Math.max(0, weatherList.size() - 14);
        for (int i = start; i < weatherList.size(); i++) {
            Weather w = weatherList.get(i);
            series.getData().add(new XYChart.Data<>(w.getDate().substring(5), w.getTemperature()));
        }
        temperatureChart.getData().add(series);
        temperatureChart.setCreateSymbols(true);
        temperatureChart.setAnimated(false);
    }

    @FXML
    private void toggleAlertsPanel() {
        boolean visible = alertsPanel.isVisible();
        alertsPanel.setVisible(!visible);
        alertsPanel.setManaged(!visible);
        if (!visible) {
            renderAlerts();
        }
    }

    private void renderAlerts() {
        alertsPanel.getChildren().clear();
        List<WeatherAlert> alerts = WeatherAlertService.getInstance().getAlerts();

        if (alerts.isEmpty()) {
            Label none = new Label("No weather alerts at this time.");
            none.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            alertsPanel.getChildren().add(none);
            return;
        }

        for (WeatherAlert alert : alerts) {
            VBox card = new VBox(4);
            card.setPadding(new Insets(10, 14, 10, 14));
            card.setStyle(alert.isRead()
                    ? "-fx-background-color: #F5F5F5; -fx-background-radius: 8; -fx-border-color: #E0E0E0; -fx-border-radius: 8;"
                    : "-fx-background-color: #FFF8E1; -fx-background-radius: 8; -fx-border-color: #FFD54F; -fx-border-radius: 8;");

            Label title = new Label(alert.getTitle());
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                    + (alert.isRead() ? "#888" : "#E65100") + ";");

            Label msg = new Label(alert.getMessage());
            msg.setWrapText(true);
            msg.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            HBox footer = new HBox(8);
            footer.setPadding(new Insets(4, 0, 0, 0));
            if (!alert.isRead()) {
                Button markRead = new Button("Mark as Read");
                markRead.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                        "-fx-font-size: 11px; -fx-background-radius: 4; -fx-padding: 3 8 3 8;");
                markRead.setOnAction(e -> {
                    alert.markRead();
                    refreshAlertBadge(WeatherAlertService.getInstance());
                    renderAlerts();
                });
                footer.getChildren().add(markRead);
            } else {
                Label readLabel = new Label("✓ Read");
                readLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
                footer.getChildren().add(readLabel);
            }

            card.getChildren().addAll(title, msg, footer);
            alertsPanel.getChildren().add(card);
        }
    }

    private void refreshAlertBadge(WeatherAlertService alertSvc) {
        long unread = alertSvc.getUnreadCount();
        alertBadge.setVisible(unread > 0);
        alertBadge.setManaged(unread > 0);
        alertBadge.setText(String.valueOf(unread));
    }

    private void loadCropRecommendations(double temp) {
        record CropRec(String name, String range, String tip) {}

        // Temperature-range based crop suggestion map
        List<CropRec> recommendations = new java.util.ArrayList<>();

        if (temp < 10) {
            cropRecoLabel.setText(String.format("Current temperature: %.1f°C — Very cold conditions.", temp));
            recommendations.add(new CropRec("Barley",  "0–12°C",  "Hardy grain; tolerates frost well."));
            recommendations.add(new CropRec("Garlic",  "0–10°C",  "Plant bulbs in well-drained soil."));
            recommendations.add(new CropRec("Spinach", "2–15°C",  "Frost-tolerant leafy green."));
        } else if (temp < 18) {
            cropRecoLabel.setText(String.format("Current temperature: %.1f°C — Cool conditions.", temp));
            recommendations.add(new CropRec("Wheat",     "10–24°C", "Ideal cool-season grain crop."));
            recommendations.add(new CropRec("Potato",    "10–20°C", "Great for cool highland zones."));
            recommendations.add(new CropRec("Cabbage",   "7–20°C",  "Best grown in cool, moist weather."));
            recommendations.add(new CropRec("Cauliflower","10–20°C","Cool-season brassica; avoid frost."));
        } else if (temp < 28) {
            cropRecoLabel.setText(String.format("Current temperature: %.1f°C — Mild/warm conditions.", temp));
            recommendations.add(new CropRec("Maize",     "18–32°C", "Warm-season staple, high yield."));
            recommendations.add(new CropRec("Rice",      "20–35°C", "Ideal for flooded paddy fields."));
            recommendations.add(new CropRec("Soybean",   "20–30°C", "Nitrogen-fixing legume."));
            recommendations.add(new CropRec("Tomato",    "18–28°C", "Popular warm-season vegetable."));
        } else if (temp < 36) {
            cropRecoLabel.setText(String.format("Current temperature: %.1f°C — Hot conditions.", temp));
            recommendations.add(new CropRec("Sorghum",  "25–35°C", "Drought-tolerant; thrives in heat."));
            recommendations.add(new CropRec("Cassava",  "25–40°C", "Heat-tolerant root crop."));
            recommendations.add(new CropRec("Cowpea",   "20–35°C", "Heat-tolerant legume."));
            recommendations.add(new CropRec("Okra",     "22–35°C", "Grows well in tropical heat."));
        } else {
            cropRecoLabel.setText(String.format("Current temperature: %.1f°C — Extreme heat. Most crops require protection.", temp));
            recommendations.add(new CropRec("Cassava",  "25–40°C", "One of the most heat-resilient crops."));
            recommendations.add(new CropRec("Sorghum",  "25–38°C", "Best option for very hot, dry climates."));
        }

        cropRecoContainer.getChildren().clear();
        for (CropRec rec : recommendations) {
            HBox row = new HBox(12);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: #F1F8E9; -fx-background-radius: 6;");

            Label nameLabel = new Label("🌿 " + rec.name());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2E7D32;");
            nameLabel.setMinWidth(110);

            Label rangeLabel = new Label("Temp: " + rec.range());
            rangeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-background-color: #DCEDC8;" +
                    "-fx-background-radius: 4; -fx-padding: 2 6 2 6;");
            rangeLabel.setMinWidth(110);

            Label tipLabel = new Label(rec.tip());
            tipLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            tipLabel.setWrapText(true);
            HBox.setHgrow(tipLabel, javafx.scene.layout.Priority.ALWAYS);

            row.getChildren().addAll(nameLabel, rangeLabel, tipLabel);
            cropRecoContainer.getChildren().add(row);
        }
    }
}
