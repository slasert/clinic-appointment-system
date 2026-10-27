package com.clinic.rules;

/**
 * Catalogue of the clinic's business rules. Every rejection names the rule it enforces,
 * so tests can check that an operation failed for the right reason and the
 * traceability matrix can refer to stable IDs.
 */
public enum BusinessRule {

    PATIENT_ID_FORMAT("BR-01", "Patient ID must be 11 digits and cannot start with 0"),
    NAME_FORMAT("BR-02", "Name must be " + ClinicRules.NAME_MIN_LENGTH + "-" + ClinicRules.NAME_MAX_LENGTH + " characters: letters, spaces, hyphens or apostrophes"),
    PATIENT_AGE_RANGE("BR-03", "Patient age must be between " + ClinicRules.MIN_AGE + " and " + ClinicRules.MAX_AGE),
    DOCTOR_DETAILS_REQUIRED("BR-04", "Doctor ID and specialty cannot be empty"),
    DOCTOR_LIMIT_RANGE("BR-05", "Doctor daily limit must be between "
            + ClinicRules.MIN_DAILY_LIMIT + " and " + ClinicRules.MAX_DAILY_LIMIT),
    UNIQUE_ID("BR-06", "Patient and doctor IDs must be unique"),
    KNOWN_REFERENCE("BR-07", "Patient, doctor or appointment does not exist"),
    REQUIRED_INPUT("BR-08", "A required value is missing"),

    SLOT_ALIGNMENT("BR-09", "Appointments must start on the hour or half hour"),
    WORKING_DAY("BR-10", "The clinic is closed on weekends"),
    WORKING_HOURS("BR-11", "Appointments must start between " + ClinicRules.OPENING_TIME + " and " + ClinicRules.LAST_SLOT_START),
    LUNCH_BREAK("BR-12", "No appointments during the " + ClinicRules.LUNCH_START + "-" + ClinicRules.LUNCH_END + " lunch break"),
    MIN_LEAD_TIME("BR-13", "Appointments must be booked at least " + ClinicRules.MIN_LEAD_HOURS + " hour(s) in advance"),
    MAX_ADVANCE("BR-14", "Appointments cannot be booked more than " + ClinicRules.MAX_ADVANCE_DAYS + " days ahead"),

    DOCTOR_CONFLICT("BR-15", "The doctor already has an appointment at this time"),
    PATIENT_CONFLICT("BR-16", "The patient already has an appointment at this time"),
    DOCTOR_DAILY_LIMIT("BR-17", "The doctor has reached the daily appointment limit"),
    PATIENT_ACTIVE_LIMIT("BR-18", "The patient already has " + ClinicRules.MAX_ACTIVE_APPOINTMENTS_PER_PATIENT + " upcoming appointments"),

    LATE_CHANGE("BR-19", "Less than " + ClinicRules.FREE_CANCELLATION_HOURS + " hours before the start only staff can cancel or reschedule"),
    ALREADY_STARTED("BR-20", "The appointment has already started"),
    STATUS_FINAL("BR-21", "Only SCHEDULED appointments can be changed"),
    COMPLETION_TIME("BR-22", "An appointment cannot be completed before it starts"),
    NO_SHOW_TIME("BR-23", "An appointment cannot be marked no-show before its slot ends"),
    SAME_TIME_RESCHEDULE("BR-24", "The new time must differ from the current time"),

    NOTE_LENGTH("BR-25", "A note must be 1-" + ClinicRules.MAX_NOTE_LENGTH + " characters"),
    NOTE_LIMIT("BR-26", "An appointment can have at most " + ClinicRules.MAX_NOTES_PER_APPOINTMENT + " notes");

    private final String id;
    private final String description;

    BusinessRule(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
