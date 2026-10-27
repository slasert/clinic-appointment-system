package com.clinic.model;

import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import com.clinic.rules.ClinicRules;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A registered patient. Instances are immutable and always valid.
 */
public final class Patient {

    /** 11 digits, first digit cannot be 0 (Turkish national ID format, BR-01). */
    private static final Pattern ID_PATTERN = Pattern.compile("[1-9][0-9]{10}");

    private final String id;
    private final String name;
    private final int age;

    public Patient(String id, String name, int age) {
        this.id = validateId(id);
        this.name = NameValidator.validate(name, "Patient name");
        this.age = validateAge(age);
    }

    private static String validateId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new ValidationException(BusinessRule.PATIENT_ID_FORMAT,
                    "Patient ID must be 11 digits and cannot start with 0");
        }
        return id;
    }

    private static int validateAge(int age) {
        if (age < ClinicRules.MIN_AGE || age > ClinicRules.MAX_AGE) {
            throw new ValidationException(BusinessRule.PATIENT_AGE_RANGE,
                    "Patient age must be between " + ClinicRules.MIN_AGE + " and " + ClinicRules.MAX_AGE);
        }
        return age;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Patient other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}
