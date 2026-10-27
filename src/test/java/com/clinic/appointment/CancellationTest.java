package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Requester;
import com.clinic.rules.BusinessRule;
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

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * FR-06 Cancellation policy. The appointment is on Tuesday 10:00.
 *
 * <pre>
 *                               R1    R2    R3    R4    R5    R6
 * Status is SCHEDULED            Y     Y     Y     Y     Y     N
 * Time left before start      &gt;=24h &gt;=24h  &lt;24h  &lt;24h  &lt;=0    -
 * Requester                      P     S     P     S     -     -
 * --------------------------------------------------------------
 * Result                      CANCEL CANCEL BR-19 CANCEL BR-20 BR-21
 * Late flag                     no    no     -    yes    -     -
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

        assertRejected(SchedulingException.class, BusinessRule.LATE_CHANGE,
                () -> service.cancel(appointment.getId(), Requester.PATIENT));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @ParameterizedTest(name = "TC-C-03 {0} cancels at/after start -> rejected")
    @Tag("DecisionTable")
    @Tag("BVA")
    @Tag("Negative")
    @EnumSource(Requester.class)   // R5
    void cancelAfterStartRejected(Requester requester) {
        clock.setTo(APPOINTMENT_START);
        assertRejected(SchedulingException.class, BusinessRule.ALREADY_STARTED,
                () -> service.cancel(appointment.getId(), requester));

        clock.setTo(APPOINTMENT_START.plusMinutes(1));
        assertRejected(SchedulingException.class, BusinessRule.ALREADY_STARTED,
                () -> service.cancel(appointment.getId(), requester));

        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-C-04 R6 an already cancelled appointment cannot be cancelled again")
    void doubleCancellationRejected() {
        service.cancel(appointment.getId(), Requester.PATIENT);

        assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL,
                () -> service.cancel(appointment.getId(), Requester.STAFF));
        assertFalse(appointment.isLateCancellation());
    }

    /** Added after mutant M13 survived: the status must be reported before any time rule. */
    @ParameterizedTest(name = "TC-C-09 cancelled appointment, {0} retries {1} before start -> reason is BR-21, not a time rule")
    @Tag("DecisionTable")
    @Tag("Negative")
    @CsvSource({"PATIENT, PT2H", "STAFF, PT-1H"})
    void statusIsReportedBeforeTimeRules(Requester requester, Duration before) {
        service.cancel(appointment.getId(), Requester.STAFF);
        clock.setTo(APPOINTMENT_START.minus(before));

        assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL,
                () -> service.cancel(appointment.getId(), requester));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-C-05 null requester is rejected and the appointment stays SCHEDULED")
    void nullRequesterRejected() {
        assertRejected(ValidationException.class, BusinessRule.REQUIRED_INPUT,
                () -> service.cancel(appointment.getId(), null));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-C-06 unknown or null appointment ID is rejected")
    void unknownAppointmentRejected() {
        assertRejected(ValidationException.class, BusinessRule.KNOWN_REFERENCE,
                () -> service.cancel("A9999", Requester.STAFF));
        assertRejected(ValidationException.class, BusinessRule.KNOWN_REFERENCE,
                () -> service.cancel(null, Requester.STAFF));
    }

    @Test
    @DisplayName("TC-C-07 a normal cancellation is not flagged as late")
    void normalCancellationNotLate() {
        service.cancel(appointment.getId(), Requester.STAFF);

        assertFalse(appointment.isLateCancellation());
    }

    @Test
    @DisplayName("TC-C-08 a cancelled appointment leaves the doctor's schedule but stays in the patient history")
    void cancelledAppointmentLeavesSchedule() {
        service.cancel(appointment.getId(), Requester.STAFF);

        assertEquals(0, service.getDoctorSchedule(DOCTOR_1, TUESDAY).size());
        assertEquals(1, service.getPatientAppointments(PATIENT_1).size());
    }
}
