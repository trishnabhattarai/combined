package com.agrisystem.controller.farmer;

import com.agrisystem.model.*;
import com.agrisystem.service.AuthService;
import com.agrisystem.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

public class FarmerMainController {

    @FXML private StackPane contentArea;
    @FXML private Label     sidebarName;
    @FXML private ImageView profileImage;
    @FXML private Label     profileInitial;

    @FXML private Button navDashboard;
    @FXML private Button navFields;
    @FXML private Button navCoops;
    @FXML private Button navLoans;
    @FXML private Button navMarket;
    @FXML private Button profileMenuBtn;

    private List<Button> navButtons;

    /** Called by child controllers when the user renames themselves. */
    public void refreshSidebarName() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            sidebarName.setText(user.getName());
            profileInitial.setText(user.getName().substring(0, 1).toUpperCase());
        }
    }

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            sidebarName.setText(user.getName());
            profileInitial.setText(user.getName().substring(0, 1).toUpperCase());
        }
        navButtons = List.of(navDashboard, navFields, navCoops, navLoans, navMarket);
        showDashboard();
    }

    @FXML private void showDashboard()    { loadContent("/com/agrisystem/fxml/farmer/FarmerDashboard.fxml",     navDashboard); }
    @FXML private void showFields()       { loadContent("/com/agrisystem/fxml/farmer/FarmerFields.fxml",        navFields); }
    @FXML private void showCooperatives() { loadContent("/com/agrisystem/fxml/farmer/FarmerCooperatives.fxml",  navCoops); }
    @FXML private void showLoans()        { loadContent("/com/agrisystem/fxml/farmer/FarmerLoans.fxml",         navLoans); }
    @FXML private void showMarket()       { loadContent("/com/agrisystem/fxml/farmer/FarmerMarket.fxml",        navMarket); }

    @FXML
    private void showProfileMenu() {
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem editProfile = new javafx.scene.control.MenuItem("Edit Profile");
        javafx.scene.control.MenuItem logout      = new javafx.scene.control.MenuItem("Logout");
        editProfile.setOnAction(e -> loadContent("/com/agrisystem/fxml/farmer/FarmerProfile.fxml", null));
        logout.setOnAction(e -> handleLogout());
        menu.getItems().addAll(editProfile, logout);
        menu.show(profileMenuBtn, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void handleLogout() {
        AuthService.getInstance().logout();
        SceneManager.getInstance().showLogin();
    }

    private void loadContent(String fxml, Button activeButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node content = loader.load();

            Object controller = loader.getController();
            // Pass back-reference so child controllers can navigate / update sidebar
            if (controller instanceof FarmerProfileController pc) {
                pc.setMainController(this);
            }
            if (controller instanceof FarmerFieldsController fc) {
                fc.setMainController(this);
            }

            contentArea.getChildren().setAll(content);

            navButtons.forEach(b -> b.getStyleClass().remove("nav-active"));
            if (activeButton != null && !activeButton.getStyleClass().contains("nav-active"))
                activeButton.getStyleClass().add("nav-active");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
