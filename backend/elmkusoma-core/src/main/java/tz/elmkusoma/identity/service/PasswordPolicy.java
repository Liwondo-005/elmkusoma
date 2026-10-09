package tz.elmkusoma.identity.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * The password screen shared by every entry point that sets a password.
 *
 * <p>It existed as a private helper inside {@code AuthServiceImpl}, which meant only the four
 * auth flows got it. Invitation redemption creates an account too, from a token that anyone
 * holding it can redeem, and it was accepting any 8-character string while registration
 * rejected "password123". One rule, one list, applied wherever a secret is chosen.</p>
 *
 * <p>Deliberately a minimal breached-obvious screen: an exact (case-insensitive) dictionary
 * match plus the account's own email local-part. It is not a strength estimator, and it is not
 * presented as one.</p>
 */
@Component
public class PasswordPolicy {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password12", "password123",
            "12345678", "123456789", "qwerty", "qwerty123", "abc12345",
            "letmein", "letmein123", "welcome", "welcome123", "admin123",
            "user12345", "test12345", "changeme", "changeme123", "iloveyou123",
            "football123", "monkey123", "dragon123", "elmkusoma", "elmkusoma123",
            "tanzania", "tanzania123");

    /**
     * @throws IllegalArgumentException if the password is blank, in the dictionary, or
     *                                  derived from the account's own email address
     */
    public void rejectWeak(String email, String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("A password is required");
        }
        if (isWeak(email, password)) {
            throw new IllegalArgumentException(
                    "Password is too common or too easily guessed. Choose a less predictable password.");
        }
    }

    /** Non-throwing form, for callers that want a boolean rather than an exception. */
    public boolean isWeak(String email, String password) {
        if (password == null || password.isBlank()) {
            return true;
        }
        String lowered = password.toLowerCase(Locale.ROOT);
        if (COMMON_PASSWORDS.contains(lowered)) {
            return true;
        }
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
            return !localPart.isBlank()
                    && (lowered.equals(localPart) || lowered.contains(localPart));
        }
        return false;
    }
}