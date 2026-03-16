package com.agrisystem.controller.admin;

import com.agrisystem.service.AuthService;
import com.agrisystem.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

public class AdminMainController {

    @FXML private StackPane contentArea;
    @FXML private Label sidebarName;
    @FXML private Label profileInitial;
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navBackup;

    private List<Button> navButtons;

    /** Called by child controllers after a name change to keep the sidebar in sync. */
    public void refreshSidebarName() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            sidebarName.setText(user.getName());
            profileInitial.setText(user.getName().substring(0, 1).toUpperCase());
        }
    }

    @FXML
    public void initialize() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            sidebarName.setText(user.getName());
            profileInitial.setText(user.getName().substring(0, 1).toUpperCase());
        }
        navButtons = List.of(navDashboard, navUsers, navBackup);
        showDashboard();
    }

    @FXML private void showDashboard()    { load("/com/agrisystem/fxml/admin/AdminDashboard.fxml", navDashboard); }
    @FXML private void showManageUsers()  { load("/com/agrisystem/fxml/admin/AdminManageUsers.fxml", navUsers); }
    @FXML private void showBackup()       { load("/com/agrisystem/fxml/admin/AdminBackup.fxml", navBackup); }

    @FXML
    private void showProfileMenu() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem ep = new javafx.scene.control.MenuItem("Edit Profile");
        javafx.scene.control.MenuItem lo = new javafx.scene.control.MenuItem("Logout");
        ep.setOnAction(e -> load("/com/agrisystem/fxml/admin/AdminProfile.fxml", null));
        lo.setOnAction(e -> handleLogout());
        menu.getItems().addAll(ep, lo);
        menu.show(sidebarName, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleLogout() {
        AuthService.getInstance().logout();
        SceneManager.getInstance().showLogin();
    }

    private void load(String fxml, Button active) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AdminProfileController apc) {
                apc.setMainController(this);
            }
            contentArea.getChildren().setAll(content);
            navButtons.forEach(b -> b.getStyleClass().remove("nav-active"));
            if (active != null && !active.getStyleClass().contains("nav-active"))
                active.getStyleClass().add("nav-active");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
