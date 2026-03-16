package com.agrisystem.controller;

import com.agrisystem.exception.UserNotFoundException;
import com.agrisystem.model.User;
import com.agrisystem.service.AuditLogService;
import com.agrisystem.service.AuthService;
import com.agrisystem.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            User user = AuthService.getInstance().login(email, password);
            AuditLogService.getInstance().log(user.getId(), user.getName(), "LOGIN",
                    "User logged in as " + user.getType());
            navigateToDashboard(user);
        } catch (UserNotFoundException e) {
            showError(e.getMessage());
        }
    }

    private void navigateToDashboard(User user) {
        switch (user.getType().toUpperCase()) {
            case "ADMIN" -> SceneManager.getInstance().showAdminDashboard();
            case "OFFICER" -> SceneManager.getInstance().showOfficerDashboard();
            default -> SceneManager.getInstance().showFarmerDashboard();
        }
    }

    @FXML
    private void goToSignup() {
        SceneManager.getInstance().showSignup();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
