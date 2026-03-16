package com.agrisystem.controller.officer;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class OfficerDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label coopLabel;
    @FXML private Label scoreLabel;
    @FXML private Label pendingLabel;
    @FXML private Label membersLabel;
    @FXML private Label pendingMembersLabel;
    @FXML private ComboBox<Market> marketCombo;
    @FXML private LineChart<String, Number> activityChart;

    private final DataService ds = DataService.getInstance();
    private Cooperative coop;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Welcome, " + user.getName() + " 🏦");

        if (user instanceof Officer officer) {
            coop = ds.findCoopById(officer.getCoopId()).orElse(null);
            if (coop != null) {
                coopLabel.setText("Cooperative: " + coop.getName());
                long pending = ds.getLoansByCoop(coop.getId()).stream()
                        .filter(l -> "PENDING".equals(l.getStatus())).count();
                long processed = ds.getLoansByCoop(coop.getId()).stream()
                        .filter(l -> !"PENDING".equals(l.getStatus())).count();
                scoreLabel.setText(String.valueOf(processed));
                pendingLabel.setText(String.valueOf(pending));
                membersLabel.setText(String.valueOf(coop.getMemberIds().size()));
                if (pendingMembersLabel != null) {
                    pendingMembersLabel.setText(String.valueOf(
                            ds.getMembershipRequestsByCoop(coop.getId()).stream()
                                    .filter(r -> "PENDING".equals(r.getStatus())).count()));
                }

                // Markets
                List<Market> markets = coop.getMarketIds().stream()
                        .map(mid -> ds.findMarketById(mid).orElse(null))
                        .filter(Objects::nonNull).toList();
                marketCombo.setItems(FXCollections.observableArrayList(markets));
                if (!markets.isEmpty()) {
                    marketCombo.setValue(markets.get(0));
                    updateChart();
                }
            }
        }
    }

    @FXML
    public void updateChart() {
        Market market = marketCombo.getValue();
        if (market == null) return;

        activityChart.getData().clear();

        // Re-query live transaction data every time
        List<MarketTransaction> txs = ds.getTransactionsByMarket(market.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");

        // Two series: sales volume and purchase volume
        XYChart.Series<String, Number> sellSeries = new XYChart.Series<>();
        sellSeries.setName("Sales (NRs)");
        XYChart.Series<String, Number> buySeries = new XYChart.Series<>();
        buySeries.setName("Purchases (NRs)");

        Map<String, Double> sellMap = new TreeMap<>();
        Map<String, Double> buyMap  = new TreeMap<>();

        for (MarketTransaction tx : txs) {
            String day = sdf.format(new Date(tx.getTimestamp()));
            if ("SELL".equals(tx.getType())) sellMap.merge(day, tx.getTotalAmount(), Double::sum);
            else                              buyMap.merge(day,  tx.getTotalAmount(), Double::sum);
        }

        sellMap.forEach((day, vol) -> sellSeries.getData().add(new XYChart.Data<>(day, vol)));
        buyMap.forEach((day,  vol) -> buySeries.getData().add(new XYChart.Data<>(day, vol)));

        activityChart.getData().addAll(sellSeries, buySeries);
        activityChart.setAnimated(false);
        activityChart.setLegendVisible(true);

        // Update pending loans and processed count live
        if (coop != null) {
            pendingLabel.setText(String.valueOf(
                    ds.getLoansByCoop(coop.getId()).stream()
                            .filter(l -> "PENDING".equals(l.getStatus())).count()));
            long processed = ds.getLoansByCoop(coop.getId()).stream()
                    .filter(l -> !"PENDING".equals(l.getStatus())).count();
            scoreLabel.setText(String.valueOf(processed));
            membersLabel.setText(String.valueOf(coop.getMemberIds().size()));
            if (pendingMembersLabel != null) {
                pendingMembersLabel.setText(String.valueOf(
                        ds.getMembershipRequestsByCoop(coop.getId()).stream()
                                .filter(r -> "PENDING".equals(r.getStatus())).count()));
            }
        }
    }
}
