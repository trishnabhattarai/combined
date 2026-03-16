package com.agrisystem.controller.officer;

import com.agrisystem.service.AuthService;
import com.agrisystem.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

public class OfficerMainController {

    @FXML private StackPane contentArea;
    @FXML private Label sidebarName;
    @FXML private Label profileInitial;
    @FXML private Button navDashboard;
    @FXML private Button navMembership;
    @FXML private Button navLoans;
    @FXML private Button navReport;

    private List<Button> navButtons;

    @FXML
    public void initialize() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            sidebarName.setText(user.getName());
            profileInitial.setText(user.getName().substring(0, 1).toUpperCase());
        }
        navButtons = List.of(navDashboard, navMembership, navLoans, navReport);
        showDashboard();
    }

    @FXML private void showDashboard()  { load("/com/agrisystem/fxml/officer/OfficerDashboard.fxml", navDashboard); }
    @FXML private void showMembership() { load("/com/agrisystem/fxml/officer/OfficerMembership.fxml", navMembership); }
    @FXML private void showLoans()      { load("/com/agrisystem/fxml/officer/OfficerLoans.fxml", navLoans); }
    @FXML private void showReport()     { load("/com/agrisystem/fxml/officer/OfficerReport.fxml", navReport); }

    @FXML
    private void showProfileMenu() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem lo = new javafx.scene.control.MenuItem("Logout");
        lo.setOnAction(e -> handleLogout());
        menu.getItems().add(lo);
        menu.show(sidebarName, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleLogout() {
        AuthService.getInstance().logout();
        SceneManager.getInstance().showLogin();
    }

    private void load(String fxml, Button active) {
        try {
            Node content = FXMLLoader.load(getClass().getResource(fxml));
            contentArea.getChildren().setAll(content);
            navButtons.forEach(b -> b.getStyleClass().remove("nav-active"));
            if (active != null && !active.getStyleClass().contains("nav-active"))
                active.getStyleClass().add("nav-active");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
