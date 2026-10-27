package com.clinic.model;

import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import com.clinic.rules.ClinicRules;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Shared name rule (BR-02) for patients and doctors.
 */
final class NameValidator {

    /** Letters (including Turkish characters), spaces, hyphens and apostrophes; must start with a letter. */
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}][\\p{L} '\\-]*");

    private NameValidator() {
    }

    static String validate(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(BusinessRule.NAME_FORMAT, field + " cannot be empty");
        }
        // NFC turns "S" + combining cedilla into the single letter "Ş" (DEF-03).
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);
        if (normalized.length() < ClinicRules.NAME_MIN_LENGTH || normalized.length() > ClinicRules.NAME_MAX_LENGTH) {
            throw new ValidationException(BusinessRule.NAME_FORMAT, field + " must be between "
                    + ClinicRules.NAME_MIN_LENGTH + " and " + ClinicRules.NAME_MAX_LENGTH + " characters");
        }
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException(BusinessRule.NAME_FORMAT,
                    field + " may only contain letters, spaces, hyphens and apostrophes");
        }
        return normalized;
    }
}
