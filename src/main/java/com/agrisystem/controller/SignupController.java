package com.agrisystem.controller;

import com.agrisystem.exception.*;
import com.agrisystem.model.District;
import com.agrisystem.service.AuthService;
import com.agrisystem.service.DataService;
import com.agrisystem.util.SceneManager;
import com.agrisystem.util.Validator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SignupController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<District> districtCombo;
    @FXML private Label errorLabel;
    @FXML private Label strengthLabel;
    @FXML private Label hintLength;
    @FXML private Label hintUpper;
    @FXML private Label hintLower;
    @FXML private Label hintDigit;
    @FXML private Label hintSpecial;

    @FXML
    public void initialize() {
        passwordField.textProperty().addListener((obs, old, val) -> updatePasswordHints(val));

        // Populate districts from DataService
        districtCombo.setItems(FXCollections.observableArrayList(
                DataService.getInstance().getAllDistricts()));
        // Show district name in the combo
        districtCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(District d) { return d == null ? "" : d.getName(); }
            @Override public District fromString(String s) { return null; }
        });
    }

    private void updatePasswordHints(String pw) {
        setHint(hintLength,  pw.length() >= 8,             "Minimum 8 characters");
        setHint(hintUpper,   pw.matches(".*[A-Z].*"),      "Uppercase letter");
        setHint(hintLower,   pw.matches(".*[a-z].*"),      "Lowercase letter");
        setHint(hintDigit,   pw.matches(".*\\d.*"),         "Number");
        setHint(hintSpecial, pw.matches(".*[^a-zA-Z0-9].*"), "Special character");
        strengthLabel.setText("Strength: " + Validator.getPasswordStrength(pw));
    }

    private void setHint(Label label, boolean ok, String text) {
        label.setText((ok ? "✓ " : "✗ ") + text);
        label.setStyle(ok ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #F44336;");
    }

    @FXML
    private void handleSignup() {
        String name    = nameField.getText().trim();
        String email   = emailField.getText().trim();
        String pass    = passwordField.getText();
        String confirm = confirmField.getText();
        District selectedDistrict = districtCombo.getValue();

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (!pass.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        if (selectedDistrict == null) {
            showError("Please select your district.");
            return;
        }

        try {
            AuthService.getInstance().signup(name, email, pass, selectedDistrict.getId());
            SceneManager.getInstance().showFarmerDashboard();
        } catch (UserAlreadyExistsException | InvalidPasswordException | InvalidEmailException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        SceneManager.getInstance().showLogin();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
