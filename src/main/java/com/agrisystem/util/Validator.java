package com.agrisystem.util;

import com.agrisystem.exception.InvalidEmailException;
import com.agrisystem.exception.InvalidPasswordException;

public class Validator {

    /**
     * Validates password strength:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    public static void validatePassword(String password) throws InvalidPasswordException {
        if (password == null || password.length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long.");
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!hasUpper) throw new InvalidPasswordException("Password must contain at least one uppercase letter.");
        if (!hasLower) throw new InvalidPasswordException("Password must contain at least one lowercase letter.");
        if (!hasDigit) throw new InvalidPasswordException("Password must contain at least one number.");
        if (!hasSpecial) throw new InvalidPasswordException("Password must contain at least one special character.");
    }

    /**
     * Validates email format
     */
    public static void validateEmail(String email) throws InvalidEmailException {
        if (email == null || !email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            throw new InvalidEmailException("Invalid email format: " + email);
        }
    }

    /**
     * Validates phone number (digits only, 10-15 chars)
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[0-9]{10,15}$");
    }

    /**
     * Returns a password strength description for UI hints
     */
    public static String getPasswordStrength(String password) {
        if (password == null || password.length() < 6) return "Too short";
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        return switch (score) {
            case 1, 2 -> "Weak";
            case 3 -> "Fair";
            case 4 -> "Strong";
            case 5 -> "Very Strong";
            default -> "Weak";
        };
    }
}
