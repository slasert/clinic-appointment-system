package com.clinic.model;

import com.clinic.exception.ValidationException;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Shared name rule for patients and doctors.
 */
final class NameValidator {

    static final int MIN_LENGTH = 2;
    static final int MAX_LENGTH = 50;

    /** Letters (including Turkish characters), spaces, hyphens and apostrophes. */
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}][\\p{L} '\\-]*");

    private NameValidator() {
    }

    static String validate(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(field + " cannot be empty");
        }
        // NFC turns "S" + combining cedilla into the single letter "Ş" (DEF-03).
        String trimmed = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new ValidationException(field + " must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException(field + " may only contain letters, spaces, hyphens and apostrophes");
        }
        return trimmed;
    }
}
