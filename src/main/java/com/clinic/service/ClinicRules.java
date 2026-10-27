package com.clinic.service;

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

    /** 09:00-12:00 (6 slots) + 13:00-17:00 (8 slots). */
    public static final int SLOTS_PER_DAY = 14;

    public static final int MIN_LEAD_HOURS = 1;
    public static final int MAX_ADVANCE_DAYS = 30;
    public static final int FREE_CANCELLATION_HOURS = 24;
    public static final int MAX_ACTIVE_APPOINTMENTS_PER_PATIENT = 3;

    private ClinicRules() {
    }
}
