package com.agrisystem.controller.officer;

import com.agrisystem.model.*;
import com.agrisystem.service.AuditLogService;
import com.agrisystem.service.DataService;
import com.agrisystem.service.MarketPriceService;
import com.agrisystem.service.MarketPriceService.PriceEntry;
import com.agrisystem.util.AlertUtil;
import com.agrisystem.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class OfficerReportController {

    @FXML private Label coopLabel;
    @FXML private Label membersLabel;
    @FXML private Label totalLoansLabel;
    @FXML private Label approvedLoansLabel;
    @FXML private Label repaidLoansLabel;
    @FXML private PieChart loanPieChart;
    @FXML private BarChart<String, Number> marketBar;
    @FXML private Label detailsLabel;

    // Market prices table
    @FXML private TableView<PriceEntry>              pricesTable;
    @FXML private TableColumn<PriceEntry, String>    colPriceMarket;
    @FXML private TableColumn<PriceEntry, String>    colPriceCrop;
    @FXML private TableColumn<PriceEntry, String>    colPriceValue;
    @FXML private TableColumn<PriceEntry, String>    colPriceUpdated;
    @FXML private TableColumn<PriceEntry, String>    colPriceExpiry;

    private final DataService ds = DataService.getInstance();
    private final MarketPriceService priceService = MarketPriceService.getInstance();
    private Cooperative coop;
    private List<Loan> loans;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (!(user instanceof Officer officer)) return;

        coop = ds.findCoopById(officer.getCoopId()).orElse(null);
        if (coop == null) { coopLabel.setText("No cooperative assigned."); return; }

        coopLabel.setText("Cooperative: " + coop.getName() + " (" + coop.getId() + ")");
        membersLabel.setText(String.valueOf(coop.getMemberIds().size()));

        loans = ds.getLoansByCoop(coop.getId());
        long approved = loans.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        long pending  = loans.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long rejected = loans.stream().filter(l -> "REJECTED".equals(l.getStatus())).count();
        long repaid   = loans.stream().filter(l -> "REPAID".equals(l.getStatus())).count();

        totalLoansLabel.setText(String.valueOf(loans.size()));
        approvedLoansLabel.setText(String.valueOf(approved));
        repaidLoansLabel.setText(String.valueOf(repaid));

        // Pie chart
        loanPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Pending (" + pending + ")", pending),
                new PieChart.Data("Approved (" + approved + ")", approved),
                new PieChart.Data("Rejected (" + rejected + ")", rejected),
                new PieChart.Data("Repaid (" + repaid + ")", repaid)
        ));
        loanPieChart.setAnimated(false);

        // Market bar chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Volume (NRs)");
        marketBar.getData().clear();
        for (String mid : coop.getMarketIds()) {
            double vol = ds.getTransactionsByMarket(mid).stream()
                    .mapToDouble(MarketTransaction::getTotalAmount).sum();
            String name = ds.findMarketById(mid).map(Market::getName).orElse(mid);
            series.getData().add(new XYChart.Data<>(name, vol));
        }
        marketBar.getData().add(series);
        marketBar.setAnimated(false);

        // Text details
        District district = ds.findDistrictById(coop.getDistrictId()).orElse(null);
        double totalLoanAmt = loans.stream().mapToDouble(Loan::getAmount).sum();
        double repaidAmt    = loans.stream().filter(l -> "REPAID".equals(l.getStatus())).mapToDouble(Loan::getAmount).sum();

        String details = String.format(
                "District: %s\nOfficer: %s\nTotal Loan Amount: NRs %.2f\nRepaid Amount: NRs %.2f\nRepayment Rate: %.1f%%\nMarkets: %s",
                district != null ? district.getName() : "—",
                officer.getName(),
                totalLoanAmt,
                repaidAmt,
                loans.isEmpty() ? 0 : (repaidAmt / totalLoanAmt * 100),
                String.join(", ", coop.getMarketIds().stream()
                        .map(m -> ds.findMarketById(m).map(Market::getName).orElse(m)).toList())
        );
        detailsLabel.setText(details);
        setupPricesTable();
    }

    private void setupPricesTable() {
        if (pricesTable == null) return; // guard if FXML not yet updated
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        colPriceMarket.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findMarketById(c.getValue().marketId).map(Market::getName).orElse(c.getValue().marketId)));
        colPriceCrop.setCellValueFactory(c -> new SimpleStringProperty(
                ds.findCropById(c.getValue().cropId).map(Crop::getName).orElse(c.getValue().cropId)));
        colPriceValue.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("NRs %.2f/kg", c.getValue().price)));
        colPriceUpdated.setCellValueFactory(c -> new SimpleStringProperty(
                sdf.format(new Date(c.getValue().timestamp))));
        colPriceExpiry.setCellValueFactory(c -> {
            long remaining = c.getValue().millisUntilRefresh();
            String text = remaining > 0
                    ? String.format("%dh %dm", remaining / 3600000, (remaining % 3600000) / 60000)
                    : "Refreshing soon";
            return new SimpleStringProperty(text);
        });

        List<PriceEntry> prices = priceService.getAllPrices();
        pricesTable.setItems(FXCollections.observableArrayList(prices));
    }

    @FXML
    private void exportReportCsv() {
        if (coop == null) {
            AlertUtil.showError("No Data", "No cooperative data available to export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report CSV");
        chooser.setInitialFileName("report_" + coop.getId() + "_" +
                new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(coopLabel.getScene().getWindow());
        if (file == null) return;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            // Header section
            bw.write("Cooperative Report");
            bw.newLine();
            bw.write("Generated," + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            bw.newLine();
            bw.write("Cooperative," + coop.getName() + "," + coop.getId());
            bw.newLine();
            bw.write("Total Members," + coop.getMemberIds().size());
            bw.newLine();
            bw.newLine();

            // Loan summary
            bw.write("Loan Summary");
            bw.newLine();
            bw.write("Status,Count,Total Amount (NRs)");
            bw.newLine();
            Map<String, List<Loan>> byStatus = new LinkedHashMap<>();
            for (String status : List.of("PENDING", "APPROVED", "REJECTED", "REPAID")) {
                byStatus.put(status, loans.stream().filter(l -> status.equals(l.getStatus())).toList());
            }
            for (var entry : byStatus.entrySet()) {
                double amt = entry.getValue().stream().mapToDouble(Loan::getAmount).sum();
                bw.write(entry.getKey() + "," + entry.getValue().size() + "," + String.format("%.2f", amt));
                bw.newLine();
            }
            bw.newLine();

            // Individual loans
            bw.write("Loan Details");
            bw.newLine();
            bw.write("Loan ID,User ID,Amount (NRs),Status,Request Date");
            bw.newLine();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            for (Loan loan : loans) {
                bw.write(String.join(",",
                        loan.getId(),
                        loan.getUserId(),
                        String.format("%.2f", loan.getAmount()),
                        loan.getStatus(),
                        sdf.format(new Date(loan.getRequestDate()))));
                bw.newLine();
            }
            bw.newLine();

            // Market volumes
            bw.write("Market Volume");
            bw.newLine();
            bw.write("Market ID,Market Name,Total Volume (NRs)");
            bw.newLine();
            for (String mid : coop.getMarketIds()) {
                double vol = ds.getTransactionsByMarket(mid).stream()
                        .mapToDouble(MarketTransaction::getTotalAmount).sum();
                String mname = ds.findMarketById(mid).map(Market::getName).orElse(mid);
                bw.write(mid + "," + mname + "," + String.format("%.2f", vol));
                bw.newLine();
            }

            User user = SessionManager.getInstance().getCurrentUser();
            AuditLogService.getInstance().log(user.getId(), user.getName(),
                    "EXPORT_REPORT", "Exported CSV report for cooperative " + coop.getId());

            AlertUtil.showInfo("Export Successful", "Report saved to:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            AlertUtil.showError("Export Failed", "Could not write file: " + e.getMessage());
        }
    }
}
