package campusplan.domain;

import java.util.Locale;

public record Student(int id, String name, String email) {
    public static final int MAX_NAME_LENGTH = 100;

    public Student {
        if (id < 1) {
            throw new IllegalArgumentException("Student ID must be positive.");
        }
        name = Rules.text(name, "Name", MAX_NAME_LENGTH);
        email = validEmail(email);
    }

    public Student withName(String newName) {
        return new Student(id, newName, email);
    }

    public Student withEmail(String newEmail) {
        return new Student(id, name, newEmail);
    }

    public String emailKey() {
        return emailKey(email);
    }

    // Emails are compared after trimming and lowercasing.
    public static String emailKey(String email) {
        Rules.present(email, "Email");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search text is required.");
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(needle) || emailKey().contains(needle);
    }

    private static String validEmail(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String trimmed = value.trim();
        int at = trimmed.indexOf('@');
        if (trimmed.chars().anyMatch(Character::isWhitespace)
                || at <= 0
                || at != trimmed.lastIndexOf('@')
                || at == trimmed.length() - 1) {
            throw new IllegalArgumentException("Email must contain one @ with text on both sides and no spaces.");
        }
        return trimmed;
    }
}
