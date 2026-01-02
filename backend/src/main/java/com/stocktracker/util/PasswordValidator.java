package com.stocktracker.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordValidator {
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SYMBOL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?]");

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters");
        }
        if (password != null) {
            if (!UPPERCASE.matcher(password).find()) {
                errors.add("Password must contain at least one uppercase letter");
            }
            if (!LOWERCASE.matcher(password).find()) {
                errors.add("Password must contain at least one lowercase letter");
            }
            if (!DIGIT.matcher(password).find()) {
                errors.add("Password must contain at least one number");
            }
            if (!SYMBOL.matcher(password).find()) {
                errors.add("Password must contain at least one symbol (!@#$%^&*()_+-=[]{}|;':\",./<>?)");
            }
        }
        return errors;
    }

    public static boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
