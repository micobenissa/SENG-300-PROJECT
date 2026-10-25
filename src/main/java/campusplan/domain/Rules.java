package campusplan.domain;

import java.util.Collection;

final class Rules {
    private Rules() {}

    static String text(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have 1-" + maxLength + " characters.");
        }
        return trimmed;
    }

    static void present(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
    }

    static void items(Collection<?> values, String field) {
        present(values, field);
        if (values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(field + " cannot contain null values.");
        }
    }
}
