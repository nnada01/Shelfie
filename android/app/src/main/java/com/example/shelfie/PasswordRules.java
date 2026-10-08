package com.example.shelfie;

/**
 * Client-side password policy: minimum length, mixed case, and a non-alphanumeric symbol.
 */
public final class PasswordRules {

    public static final int MIN_LENGTH = 8;

    public static final class Result {
        public final boolean minLength;
        public final boolean hasUpperCase;
        public final boolean hasLowerCase;
        public final boolean hasSpecialChar;

        Result(boolean minLength, boolean hasUpperCase, boolean hasLowerCase, boolean hasSpecialChar) {
            this.minLength = minLength;
            this.hasUpperCase = hasUpperCase;
            this.hasLowerCase = hasLowerCase;
            this.hasSpecialChar = hasSpecialChar;
        }

        public boolean isValid() {
            return minLength && hasUpperCase && hasLowerCase && hasSpecialChar;
        }
    }

    private PasswordRules() {
    }

    public static Result evaluate(CharSequence raw) {
        String password = raw == null ? "" : raw.toString();
        boolean minLength = password.length() >= MIN_LENGTH;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasSpecial = false;
        for (int i = 0, n = password.length(); i < n; i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isUpperCase(c)) {
                    hasUpper = true;
                }
                if (Character.isLowerCase(c)) {
                    hasLower = true;
                }
            } else if (!Character.isDigit(c) && !Character.isWhitespace(c)) {
                hasSpecial = true;
            }
        }
        return new Result(minLength, hasUpper, hasLower, hasSpecial);
    }
}
