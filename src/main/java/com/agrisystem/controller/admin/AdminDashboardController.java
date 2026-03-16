package com.agrisystem.controller.admin;

import com.agrisystem.model.*;
import com.agrisystem.service.DataService;
import com.agrisystem.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label farmersLabel;
    @FXML private Label officersLabel;
    @FXML private Label loansLabel;
    @FXML private PieChart userPieChart;
    @FXML private ListView<String> recentLoansList;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        welcomeLabel.setText("Welcome, " + user.getName() + " (Admin) 🔑");

        DataService ds = DataService.getInstance();
        List<User> users = ds.getAllUsers();

        long farmers = users.stream().filter(u -> "FARMER".equals(u.getType())).count();
        long officers = users.stream().filter(u -> "OFFICER".equals(u.getType())).count();
        long admins = users.stream().filter(u -> "ADMIN".equals(u.getType())).count();

        totalUsersLabel.setText(String.valueOf(users.size()));
        farmersLabel.setText(String.valueOf(farmers));
        officersLabel.setText(String.valueOf(officers));
        loansLabel.setText(String.valueOf(ds.getAllLoans().size()));

        // Pie chart
        userPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Farmers (" + farmers + ")", farmers),
                new PieChart.Data("Officers (" + officers + ")", officers),
                new PieChart.Data("Admins (" + admins + ")", admins)
        ));
        userPieChart.setAnimated(false);

        // Recent loans
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<String> recentLoans = ds.getAllLoans().stream()
                .sorted(Comparator.comparingLong(Loan::getRequestDate).reversed())
                .limit(10)
                .map(l -> l.getId() + " | " + l.getUserId() + " | NRs " +
                        String.format("%.0f", l.getAmount()) + " | " + l.getStatus())
                .toList();
        recentLoansList.setItems(FXCollections.observableArrayList(recentLoans));
    }
}
