package com.clinic.rules;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Business rule constants of the clinic. Kept in one place so the requirements
 * and the tests refer to the same numbers.
 */
public final class ClinicRules {

    public static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(17, 0);
    public static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    public static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    public static final int SLOT_MINUTES = 30;

    /** Last bookable start time: 16:30. */
    public static final LocalTime LAST_SLOT_START = CLOSING_TIME.minusMinutes(SLOT_MINUTES);

    /** Derived from the times above: 09:00-12:00 (6 slots) + 13:00-17:00 (8 slots) = 14. */
    public static final int SLOTS_PER_DAY = (int) ((Duration.between(OPENING_TIME, LUNCH_START).toMinutes()
            + Duration.between(LUNCH_END, CLOSING_TIME).toMinutes()) / SLOT_MINUTES);

    public static final int MIN_AGE = 0;
    public static final int MAX_AGE = 120;
    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 50;
    public static final int MIN_DAILY_LIMIT = 1;
    public static final int MAX_DAILY_LIMIT = SLOTS_PER_DAY;

    public static final int MIN_LEAD_HOURS = 1;
    public static final int MAX_ADVANCE_DAYS = 30;
    public static final int FREE_CANCELLATION_HOURS = 24;
    public static final int MAX_ACTIVE_APPOINTMENTS_PER_PATIENT = 3;
    public static final int MAX_NOTE_LENGTH = 200;
    public static final int MAX_NOTES_PER_APPOINTMENT = 5;

    private ClinicRules() {
    }
}
