package com.agrisystem.controller.admin;

import com.agrisystem.exception.InvalidPasswordException;
import com.agrisystem.model.User;
import com.agrisystem.service.AuditLogService;
import com.agrisystem.service.DataService;
import com.agrisystem.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminProfileController {

    @FXML private Label profileInitial;
    @FXML private Label idLabel;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passField;
    @FXML private PasswordField confirmField;
    @FXML private Label errorLabel;

    private AdminMainController mainController;

    public void setMainController(AdminMainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        profileInitial.setText(user.getName().substring(0, 1).toUpperCase());
        idLabel.setText(user.getId());
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
    }

    @FXML
    private void saveProfile() {
        User user = SessionManager.getInstance().getCurrentUser();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String pass = passField.getText();
        String confirm = confirmField.getText();

        if (name.isEmpty() || email.isEmpty()) {
            showError("Name and email cannot be empty."); return;
        }

        if (!pass.isEmpty()) {
            if (!pass.equals(confirm)) { showError("Passwords do not match."); return; }
            try {
                Validator.validatePassword(pass);
                user.setPassword(pass);
            } catch (InvalidPasswordException e) {
                showError(e.getMessage()); return;
            }
        }

        user.setName(name);
        user.setEmail(email);
        DataService.getInstance().updateUser(user);
        SessionManager.getInstance().setCurrentUser(user);
        if (mainController != null) mainController.refreshSidebarName();
        AuditLogService.getInstance().log(user.getId(), user.getName(), "PROFILE_UPDATE", "Admin updated their profile");
        errorLabel.setVisible(false);
        AlertUtil.showInfo("Saved", "Profile updated successfully.");
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
