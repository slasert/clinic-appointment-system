package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Requester;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-06 Cancellation policy. The appointment is on Tuesday 10:00.
 *
 * <pre>
 *                               R1   R2   R3   R4   R5   R6
 * Status is SCHEDULED            Y    Y    Y    Y    Y    N
 * Time left before start      >=24h >=24h <24h <24h  <=0   -
 * Requester                      P    S    P    S    -    -
 * ----------------------------------------------------------
 * Cancelled (normal)             X    X
 * Cancelled (late flag)                         X
 * Rejected                                 X         X    X
 * </pre>
 */
@DisplayName("FR-06 Cancellation policy")
class CancellationTest extends ServiceTestBase {

    private static final LocalDateTime APPOINTMENT_START = tuesdayAt(10, 0);
    private Appointment appointment;

    @BeforeEach
    void bookAppointment() {
        appointment = service.book(PATIENT_1, DOCTOR_1, APPOINTMENT_START);
    }

    @ParameterizedTest(name = "TC-C-01 {0} cancels {1} before start -> CANCELLED, late={2}")
    @Tag("DecisionTable")
    @Tag("BVA")
    @CsvSource({
            // requester, time before start, late flag
            "PATIENT, PT26H,    false",   // R1
            "PATIENT, PT24H,    false",   // R1 boundary: exactly 24 hours is still free
            "STAFF,   PT24H,    false",   // R2
            "STAFF,   PT23H59M, true",    // R4 boundary: one minute late
            "STAFF,   PT1M,     true"     // R4
    })
    void allowedCancellations(Requester requester, Duration before, boolean expectedLate) {
        clock.setTo(APPOINTMENT_START.minus(before));

        service.cancel(appointment.getId(), requester);

        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals(expectedLate, appointment.isLateCancellation());
    }

    @ParameterizedTest(name = "TC-C-02 PATIENT cancels {0} before start -> rejected")
    @Tag("DecisionTable")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"PT23H59M", "PT2H", "PT1M"})   // R3
    void patientLateCancellationRejected(Duration before) {
        clock.setTo(APPOINTMENT_START.minus(before));

        assertThrows(SchedulingException.class, () -> service.cancel(appointment.getId(), Requester.PATIENT));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @ParameterizedTest(name = "TC-C-03 {0} cancels at/after start -> rejected")
    @Tag("DecisionTable")
    @Tag("BVA")
    @Tag("Negative")
    @EnumSource(Requester.class)   // R5
    void cancelAfterStartRejected(Requester requester) {
        clock.setTo(APPOINTMENT_START);
        assertThrows(SchedulingException.class, () -> service.cancel(appointment.getId(), requester));

        clock.setTo(APPOINTMENT_START.plusMinutes(1));
        assertThrows(SchedulingException.class, () -> service.cancel(appointment.getId(), requester));
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-C-04 R6 an already cancelled appointment cannot be cancelled again")
    void doubleCancellationRejected() {
        service.cancel(appointment.getId(), Requester.PATIENT);

        SchedulingException e = assertThrows(SchedulingException.class,
                () -> service.cancel(appointment.getId(), Requester.STAFF));
        assertTrue(e.getMessage().contains("CANCELLED"));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-C-05 null requester is rejected")
    void nullRequesterRejected() {
        assertThrows(ValidationException.class, () -> service.cancel(appointment.getId(), null));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-C-06 unknown or null appointment ID is rejected")
    void unknownAppointmentRejected() {
        assertThrows(ValidationException.class, () -> service.cancel("A9999", Requester.STAFF));
        assertThrows(ValidationException.class, () -> service.cancel(null, Requester.STAFF));
    }

    @Test
    @DisplayName("TC-C-07 a normal cancellation is not flagged as late")
    void normalCancellationNotLate() {
        service.cancel(appointment.getId(), Requester.STAFF);

        assertFalse(appointment.isLateCancellation());
    }
}
