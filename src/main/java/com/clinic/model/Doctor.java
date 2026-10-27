package com.clinic.model;

import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import com.clinic.rules.ClinicRules;

import java.util.Objects;

/**
 * A doctor working at the clinic. Instances are immutable and always valid.
 */
public final class Doctor {

    private final String id;
    private final String name;
    private final String specialty;
    private final int dailyLimit;

    public Doctor(String id, String name, String specialty, int dailyLimit) {
        if (id == null || id.isBlank()) {
            throw new ValidationException(BusinessRule.DOCTOR_DETAILS_REQUIRED, "Doctor ID cannot be empty");
        }
        if (specialty == null || specialty.isBlank()) {
            throw new ValidationException(BusinessRule.DOCTOR_DETAILS_REQUIRED, "Doctor specialty cannot be empty");
        }
        if (dailyLimit < ClinicRules.MIN_DAILY_LIMIT || dailyLimit > ClinicRules.MAX_DAILY_LIMIT) {
            throw new ValidationException(BusinessRule.DOCTOR_LIMIT_RANGE, "Daily appointment limit must be between "
                    + ClinicRules.MIN_DAILY_LIMIT + " and " + ClinicRules.MAX_DAILY_LIMIT);
        }
        this.id = id.trim();
        this.name = NameValidator.validate(name, "Doctor name");
        this.specialty = specialty.trim();
        this.dailyLimit = dailyLimit;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Doctor other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Dr. " + name + " (" + specialty + ")";
    }
}
