package com.agrisystem.service;

import com.agrisystem.exception.*;
import com.agrisystem.model.*;
import com.agrisystem.util.*;

import java.util.Optional;

public class AuthService {
    private static AuthService instance;
    private final DataService data = DataService.getInstance();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public User login(String emailOrPhone, String password) throws UserNotFoundException {
        Optional<User> found = data.getAllUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(emailOrPhone)
                          || emailOrPhone.equals(u.getNumber()))
                .findFirst();
        if (found.isEmpty() || !found.get().getPassword().equals(password)) {
            throw new UserNotFoundException("Invalid credentials. Please check your email/phone and password.");
        }
        SessionManager.getInstance().setCurrentUser(found.get());
        return found.get();
    }

    public User signup(String name, String emailOrPhone, String password, String districtId)
            throws UserAlreadyExistsException, InvalidPasswordException, InvalidEmailException {

        // Validate password strength
        Validator.validatePassword(password);

        boolean isEmail = emailOrPhone.contains("@");
        if (isEmail) {
            Validator.validateEmail(emailOrPhone);
        } else if (!Validator.isValidPhone(emailOrPhone)) {
            throw new InvalidEmailException("Invalid phone number format.");
        }

        // Check duplicates
        boolean exists = data.getAllUsers().stream().anyMatch(u ->
                u.getEmail().equalsIgnoreCase(emailOrPhone)
                        || u.getNumber().equals(emailOrPhone));
        if (exists) {
            throw new UserAlreadyExistsException("An account with this email/phone already exists.");
        }

        // Resolve a default market from the chosen district
        String marketId = data.getAllMarkets().stream()
                .filter(m -> districtId != null && districtId.equals(m.getDistrictId()))
                .findFirst()
                .map(m -> m.getId())
                .orElse(null);

        String id = IdGenerator.nextUserId("FARMER");
        Farmer farmer = new Farmer();
        farmer.setId(id);
        farmer.setName(name);
        farmer.setEmail(isEmail ? emailOrPhone : "");
        farmer.setNumber(isEmail ? "" : emailOrPhone);
        farmer.setPassword(password);
        farmer.setType("FARMER");
        farmer.setBalance(5000.0);
        farmer.setResponsibilityScore(50.0);
        farmer.setDistrictId(districtId);
        farmer.setMarketId(marketId);

        data.addUser(farmer);
        SessionManager.getInstance().setCurrentUser(farmer);
        return farmer;
    }

    public void logout() {
        SessionManager.getInstance().logout();
    }
}
