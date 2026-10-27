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
import org.junit.jupiter.params.provider.EnumSource;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-07 Rescheduling. Moving an appointment must be atomic: either the old one becomes
 * RESCHEDULED and a new SCHEDULED one exists, or nothing changes at all.
 * The same 24-hour policy as cancellation applies to the original appointment.
 *
 * <pre>
 * Decision table (original on Tuesday 10:00)
 *                            R1   R2   R3   R4   R5   R6   R7
 * Original SCHEDULED          Y    Y    Y    Y    Y    Y    N
 * Before start                Y    Y    Y    Y    Y    N    -
 * &gt;=24h or requester STAFF   Y    Y    Y    Y    N    -    -
 * Target slot valid           Y    Y    N    Y    -    -    -
 * Target slot free            Y    N    -    Y    -    -    -
 * Target differs              Y    Y    Y    N    -    -    -
 * -----------------------------------------------------------
 * Result                    MOVE BR15 BR12 BR24 BR19 BR20 BR21
 * </pre>
 */
@DisplayName("FR-07 Rescheduling")
class RescheduleTest extends ServiceTestBase {

    private Appointment original;

    @BeforeEach
    void bookOriginal() {
        original = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
    }

    private void assertOriginalUnchanged() {
        assertEquals(AppointmentStatus.SCHEDULED, original.getStatus());
        assertEquals(1, storedAppointments());
    }

    @Test
    @Tag("DecisionTable")
    @DisplayName("TC-RS-01 R1 successful reschedule: old RESCHEDULED, new SCHEDULED with same patient/doctor")
    void successfulReschedule() {
        Appointment moved = service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT);

        assertAll(
                () -> assertEquals(AppointmentStatus.RESCHEDULED, original.getStatus()),
                () -> assertEquals(AppointmentStatus.SCHEDULED, moved.getStatus()),
                () -> assertEquals(tuesdayAt(14, 0), moved.getStart()),
                () -> assertEquals(original.getPatient(), moved.getPatient()),
                () -> assertEquals(original.getDoctor(), moved.getDoctor()));
    }

    @Test
    @DisplayName("TC-RS-02 the old slot becomes free after rescheduling")
    void oldSlotFreed() {
        service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT);

        Appointment other = service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0));

        assertEquals(tuesdayAt(10, 0), other.getStart());
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-RS-03 R2 target slot busy -> rejected and original stays SCHEDULED (atomicity)")
    void busyTargetLeavesOriginalUnchanged() {
        service.book(PATIENT_2, DOCTOR_1, tuesdayAt(14, 0));

        assertRejected(SchedulingException.class, BusinessRule.DOCTOR_CONFLICT,
                () -> service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT));
        assertEquals(AppointmentStatus.SCHEDULED, original.getStatus());
        assertEquals(1, service.getPatientAppointments(PATIENT_1).size());
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-RS-04 R3 invalid target time (lunch) -> rejected and original unchanged")
    void invalidTargetLeavesOriginalUnchanged() {
        assertRejected(SchedulingException.class, BusinessRule.LUNCH_BREAK,
                () -> service.reschedule(original.getId(), tuesdayAt(12, 0), Requester.PATIENT));
        assertOriginalUnchanged();
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-RS-05 R4 rescheduling to the same time is rejected")
    void sameTimeRejected() {
        assertRejected(SchedulingException.class, BusinessRule.SAME_TIME_RESCHEDULE,
                () -> service.reschedule(original.getId(), tuesdayAt(10, 0), Requester.PATIENT));
        assertOriginalUnchanged();
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-06 null target time or null requester is rejected")
    void nullInputsRejected() {
        assertRejected(ValidationException.class, BusinessRule.REQUIRED_INPUT,
                () -> service.reschedule(original.getId(), null, Requester.PATIENT));
        assertRejected(ValidationException.class, BusinessRule.REQUIRED_INPUT,
                () -> service.reschedule(original.getId(), tuesdayAt(14, 0), null));
        assertOriginalUnchanged();
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-RS-07 patient at the active limit (3) can still move one of them")
    void patientAtLimitCanReschedule() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 30));
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(11, 0));

        Appointment moved = service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.PATIENT);

        assertEquals(AppointmentStatus.SCHEDULED, moved.getStatus());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-RS-08 doctor at the daily limit can move an appointment within the same day")
    void doctorAtLimitCanRescheduleSameDay() {
        Appointment a = service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(9, 30));

        Appointment moved = service.reschedule(a.getId(), tuesdayAt(15, 0), Requester.STAFF);

        assertEquals(2, service.getDoctorSchedule(LIMITED_DOCTOR, TUESDAY).size());
        assertEquals(tuesdayAt(15, 0), moved.getStart());
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-RS-09 R5 patient cannot reschedule less than 24h before, staff can")
    void lateReschedulePolicy() {
        clock.setTo(tuesdayAt(9, 0));   // one hour before the appointment

        assertRejected(SchedulingException.class, BusinessRule.LATE_CHANGE,
                () -> service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.PATIENT));
        assertOriginalUnchanged();

        Appointment moved = service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.STAFF);
        assertEquals(AppointmentStatus.SCHEDULED, moved.getStatus());
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-RS-10 R7 a rescheduled appointment cannot be rescheduled again")
    void rescheduledCannotBeMovedAgain() {
        service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT);

        assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL,
                () -> service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.PATIENT));
        assertEquals(2, storedAppointments());
    }

    @ParameterizedTest(name = "TC-RS-11 R6 {0} cannot reschedule after the start")
    @Tag("DecisionTable")
    @Tag("Negative")
    @EnumSource(Requester.class)
    void rescheduleAfterStartRejected(Requester requester) {
        clock.setTo(tuesdayAt(10, 0));

        assertRejected(SchedulingException.class, BusinessRule.ALREADY_STARTED,
                () -> service.reschedule(original.getId(), tuesdayAt(15, 0), requester));
        assertOriginalUnchanged();
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-12 target slot where the patient already sees another doctor -> rejected")
    void patientConflictAtTarget() {
        service.book(PATIENT_1, DOCTOR_2, tuesdayAt(14, 0));

        assertRejected(SchedulingException.class, BusinessRule.PATIENT_CONFLICT,
                () -> service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT));
        assertEquals(AppointmentStatus.SCHEDULED, original.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-13 doctor at the daily limit cannot receive an appointment moved from another day")
    void doctorLimitAppliesToTargetDay() {
        service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(9, 30));
        Appointment otherDay = service.book(PATIENT_4, LIMITED_DOCTOR, TUESDAY.plusDays(1).atTime(9, 0));

        assertRejected(SchedulingException.class, BusinessRule.DOCTOR_DAILY_LIMIT,
                () -> service.reschedule(otherDay.getId(), tuesdayAt(15, 0), Requester.STAFF));
        assertEquals(AppointmentStatus.SCHEDULED, otherDay.getStatus());
    }
}
