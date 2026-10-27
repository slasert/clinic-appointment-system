package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.model.Requester;
import com.clinic.rules.BusinessRule;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-08 Appointment lifecycle. Only SCHEDULED appointments can change status.
 * COMPLETED is allowed from the start time, NO_SHOW from the end time (start + 30 min).
 */
@DisplayName("FR-08 Status transitions")
class StatusTransitionTest extends ServiceTestBase {

    private Appointment appointment;

    @BeforeEach
    void bookAppointment() {
        appointment = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-ST-01 complete one minute before start -> rejected")
    void completeBeforeStartRejected() {
        clock.setTo(tuesdayAt(9, 59));

        assertRejected(SchedulingException.class, BusinessRule.COMPLETION_TIME, () -> service.complete(appointment.getId()));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-ST-02 complete exactly at start -> COMPLETED")
    void completeAtStart() {
        clock.setTo(tuesdayAt(10, 0));

        service.complete(appointment.getId());

        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-ST-03 no-show one minute before the slot ends -> rejected")
    void noShowBeforeEndRejected() {
        clock.setTo(tuesdayAt(10, 29));

        assertRejected(SchedulingException.class, BusinessRule.NO_SHOW_TIME, () -> service.markNoShow(appointment.getId()));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-ST-04 no-show exactly when the slot ends -> NO_SHOW")
    void noShowAtEnd() {
        clock.setTo(tuesdayAt(10, 30));

        service.markNoShow(appointment.getId());

        assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
    }

    /**
     * Decision table of status transitions. For each row the appointment is first brought into
     * {@code from}; then the clock is moved to a time at which {@code operation} would be allowed
     * for a SCHEDULED appointment. So the status is the only condition that can cause a rejection.
     */
    @ParameterizedTest(name = "TC-ST-09 from {0}: {1} -> {2}")
    @Tag("DecisionTable")
    @Tag("Negative")
    @CsvSource({
            "SCHEDULED,   CANCEL,     CANCELLED",
            "SCHEDULED,   RESCHEDULE, RESCHEDULED",
            "SCHEDULED,   COMPLETE,   COMPLETED",
            "SCHEDULED,   NO_SHOW,    NO_SHOW",
            "CANCELLED,   CANCEL,     REJECTED",
            "CANCELLED,   RESCHEDULE, REJECTED",
            "CANCELLED,   COMPLETE,   REJECTED",
            "CANCELLED,   NO_SHOW,    REJECTED",
            "RESCHEDULED, CANCEL,     REJECTED",
            "RESCHEDULED, RESCHEDULE, REJECTED",
            "RESCHEDULED, COMPLETE,   REJECTED",
            "RESCHEDULED, NO_SHOW,    REJECTED",
            "COMPLETED,   CANCEL,     REJECTED",
            "COMPLETED,   RESCHEDULE, REJECTED",
            "COMPLETED,   COMPLETE,   REJECTED",
            "COMPLETED,   NO_SHOW,    REJECTED",
            "NO_SHOW,     CANCEL,     REJECTED",
            "NO_SHOW,     RESCHEDULE, REJECTED",
            "NO_SHOW,     COMPLETE,   REJECTED",
            "NO_SHOW,     NO_SHOW,    REJECTED"
    })
    void transitionTable(AppointmentStatus from, String operation, String expected) {
        bringInto(from);

        if (expected.equals("REJECTED")) {
            assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL, () -> apply(operation));
            assertEquals(from, appointment.getStatus());
        } else {
            apply(operation);
            assertEquals(AppointmentStatus.valueOf(expected), appointment.getStatus());
        }
    }

    private void bringInto(AppointmentStatus status) {
        switch (status) {
            case SCHEDULED -> { }
            case CANCELLED -> service.cancel(appointment.getId(), Requester.STAFF);
            case RESCHEDULED -> service.reschedule(appointment.getId(), tuesdayAt(14, 0), Requester.STAFF);
            case COMPLETED -> {
                clock.setTo(tuesdayAt(10, 30));
                service.complete(appointment.getId());
            }
            case NO_SHOW -> {
                clock.setTo(tuesdayAt(10, 30));
                service.markNoShow(appointment.getId());
            }
        }
    }

    private void apply(String operation) {
        switch (operation) {
            case "CANCEL" -> {
                clock.setTo(NOW);
                service.cancel(appointment.getId(), Requester.STAFF);
            }
            case "RESCHEDULE" -> {
                clock.setTo(NOW);
                service.reschedule(appointment.getId(), tuesdayAt(15, 0), Requester.STAFF);
            }
            case "COMPLETE" -> {
                clock.setTo(tuesdayAt(10, 30));
                service.complete(appointment.getId());
            }
            case "NO_SHOW" -> {
                clock.setTo(tuesdayAt(10, 30));
                service.markNoShow(appointment.getId());
            }
            default -> throw new IllegalArgumentException(operation);
        }
    }

    @Test
    @DisplayName("TC-ST-07 completed and no-show appointments still count towards the doctor's daily limit")
    void finishedAppointmentsOccupySlots() {
        clock.setTo(MONDAY.atTime(7, 0));
        Appointment first = service.book(PATIENT_1, LIMITED_DOCTOR, MONDAY.atTime(9, 0));
        Appointment second = service.book(PATIENT_2, LIMITED_DOCTOR, MONDAY.atTime(9, 30));
        clock.setTo(MONDAY.atTime(10, 0));
        service.complete(first.getId());
        service.markNoShow(second.getId());

        assertRejected(SchedulingException.class, BusinessRule.DOCTOR_DAILY_LIMIT,
                () -> service.book(PATIENT_3, LIMITED_DOCTOR, MONDAY.atTime(15, 0)));
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-ST-10 completing long after the slot (next day) is still allowed")
    void completeLaterIsAllowed() {
        clock.setTo(TUESDAY.plusDays(1).atTime(9, 0));

        service.complete(appointment.getId());

        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }
}
