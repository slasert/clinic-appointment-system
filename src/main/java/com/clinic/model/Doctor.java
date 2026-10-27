package com.clinic.model;

import com.clinic.exception.ValidationException;
import com.clinic.service.ClinicRules;

import java.util.Objects;

/**
 * A doctor working at the clinic. Instances are immutable and always valid.
 */
public final class Doctor {

    public static final int MIN_DAILY_LIMIT = 1;
    public static final int MAX_DAILY_LIMIT = ClinicRules.SLOTS_PER_DAY;

    private final String id;
    private final String name;
    private final String specialty;
    private final int dailyLimit;

    public Doctor(String id, String name, String specialty, int dailyLimit) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Doctor ID cannot be empty");
        }
        if (specialty == null || specialty.isBlank()) {
            throw new ValidationException("Doctor specialty cannot be empty");
        }
        if (dailyLimit < MIN_DAILY_LIMIT || dailyLimit > MAX_DAILY_LIMIT) {
            throw new ValidationException("Daily appointment limit must be between "
                    + MIN_DAILY_LIMIT + " and " + MAX_DAILY_LIMIT);
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
