package com.clinic.appointment;

/**
 * Lifecycle of an appointment. Only SCHEDULED can change; every other status is final.
 *
 * <pre>
 * SCHEDULED -> CANCELLED | RESCHEDULED | COMPLETED | NO_SHOW
 * </pre>
 */
public enum AppointmentStatus {
    SCHEDULED,
    CANCELLED,
    RESCHEDULED,
    COMPLETED,
    NO_SHOW;

    /** Whether the appointment still uses one of the doctor's daily slots. */
    public boolean occupiesSlot() {
        return this == SCHEDULED || this == COMPLETED || this == NO_SHOW;
    }
}
