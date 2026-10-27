package com.clinic.service;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Requester;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-07 Rescheduling. Moving an appointment must be atomic: either the old one becomes
 * RESCHEDULED and a new SCHEDULED one exists, or nothing changes at all.
 * The same 24-hour policy as cancellation applies to the original appointment.
 */
@DisplayName("FR-07 Rescheduling")
class RescheduleTest extends ServiceTestBase {

    private Appointment original;

    @BeforeEach
    void bookOriginal() {
        original = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-RS-01 successful reschedule: old RESCHEDULED, new SCHEDULED with same patient/doctor")
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

        assertDoesNotThrow(() -> service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0)));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-03 target slot busy -> rejected and original stays SCHEDULED (atomicity)")
    void busyTargetLeavesOriginalUnchanged() {
        service.book(PATIENT_2, DOCTOR_1, tuesdayAt(14, 0));

        assertThrows(SchedulingException.class,
                () -> service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT));
        assertEquals(AppointmentStatus.SCHEDULED, original.getStatus());
        assertEquals(1, service.getPatientAppointments(PATIENT_1).size());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-04 invalid target time (lunch) -> rejected and original unchanged")
    void invalidTargetLeavesOriginalUnchanged() {
        assertThrows(SchedulingException.class,
                () -> service.reschedule(original.getId(), tuesdayAt(12, 0), Requester.PATIENT));
        assertEquals(AppointmentStatus.SCHEDULED, original.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-05 rescheduling to the same time is rejected")
    void sameTimeRejected() {
        assertThrows(SchedulingException.class,
                () -> service.reschedule(original.getId(), tuesdayAt(10, 0), Requester.PATIENT));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-06 null target time is rejected")
    void nullTargetRejected() {
        assertThrows(ValidationException.class,
                () -> service.reschedule(original.getId(), null, Requester.PATIENT));
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-RS-07 patient at the active limit (3) can still move one of them")
    void patientAtLimitCanReschedule() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 30));
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(11, 0));

        assertDoesNotThrow(() -> service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.PATIENT));
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-RS-08 doctor at the daily limit can move an appointment within the same day")
    void doctorAtLimitCanRescheduleSameDay() {
        Appointment a = service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(9, 30));

        assertDoesNotThrow(() -> service.reschedule(a.getId(), tuesdayAt(15, 0), Requester.STAFF));
    }

    @Test
    @Tag("DecisionTable")
    @Tag("Negative")
    @DisplayName("TC-RS-09 patient cannot reschedule less than 24h before, staff can")
    void lateReschedulePolicy() {
        clock.setTo(tuesdayAt(9, 0));   // one hour before the appointment

        assertThrows(SchedulingException.class,
                () -> service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.PATIENT));
        assertDoesNotThrow(() -> service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.STAFF));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-RS-10 a rescheduled appointment cannot be rescheduled again")
    void rescheduledCannotBeMovedAgain() {
        service.reschedule(original.getId(), tuesdayAt(14, 0), Requester.PATIENT);

        assertThrows(SchedulingException.class,
                () -> service.reschedule(original.getId(), tuesdayAt(15, 0), Requester.PATIENT));
    }
}
