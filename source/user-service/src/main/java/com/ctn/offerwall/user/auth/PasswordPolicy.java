package com.ctn.offerwall.user.auth;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private static final String MESSAGE = "Password must be at least 16 characters, or at least 8 characters with at least 3 of 4: lowercase, uppercase, numbers, or special characters.";

    public Result validate(String password) {
        if (password == null) {
            return new Result(false, MESSAGE);
        }

        if (password.length() >= 16) {
            return new Result(true, null);
        }

        if (password.length() < 8) {
            return new Result(false, MESSAGE);
        }

        int categories = 0;
        categories += password.chars().anyMatch(Character::isLowerCase) ? 1 : 0;
        categories += password.chars().anyMatch(Character::isUpperCase) ? 1 : 0;
        categories += password.chars().anyMatch(Character::isDigit) ? 1 : 0;
        categories += password.chars().anyMatch(character -> !Character.isLetterOrDigit(character)) ? 1 : 0;

        return categories >= 3 ? new Result(true, null) : new Result(false, MESSAGE);
    }

    public record Result(boolean valid, String message) {
    }
}
