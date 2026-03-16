package com.agrisystem.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static SceneManager instance;
    private Stage primaryStage;

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 750;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void init(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("AgriSystem");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
    }

    public void showLogin() { loadScene("/com/agrisystem/fxml/Login.fxml", 500, 600); }
    public void showSignup() { loadScene("/com/agrisystem/fxml/Signup.fxml", 500, 700); }
    public void showFarmerDashboard() { loadScene("/com/agrisystem/fxml/farmer/FarmerMain.fxml", WIDTH, HEIGHT); }
    public void showAdminDashboard() { loadScene("/com/agrisystem/fxml/admin/AdminMain.fxml", WIDTH, HEIGHT); }
    public void showOfficerDashboard() { loadScene("/com/agrisystem/fxml/officer/OfficerMain.fxml", WIDTH, HEIGHT); }

    private void loadScene(String fxml, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(getClass().getResource("/com/agrisystem/css/main.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() { return primaryStage; }
}
