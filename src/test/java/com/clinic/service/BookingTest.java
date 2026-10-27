package com.clinic.service;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Requester;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-04 Booking: conflicts and limits.
 *
 * <p>Decision table for a booking request with a valid time slot:</p>
 * <pre>
 *                      R1  R2  R3  R4  R5
 * Doctor free           Y   N   Y   Y   Y
 * Patient free          Y   -   N   Y   Y
 * Doctor under limit    Y   -   -   N   Y
 * Patient under limit   Y   -   -   -   N
 * ---------------------------------------
 * Booked                X
 * Rejected                  X   X   X   X
 * </pre>
 */
@DisplayName("FR-04 Booking")
class BookingTest extends ServiceTestBase {

    @Test
    @Tag("EP")
    @DisplayName("TC-B-01 valid request creates a SCHEDULED appointment")
    void successfulBooking() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        assertAll(
                () -> assertEquals("A0001", a.getId()),
                () -> assertEquals(PATIENT_1, a.getPatient().getId()),
                () -> assertEquals(DOCTOR_1, a.getDoctor().getId()),
                () -> assertEquals(tuesdayAt(10, 0), a.getStart()),
                () -> assertEquals(tuesdayAt(10, 30), a.getEnd()),
                () -> assertEquals(AppointmentStatus.SCHEDULED, a.getStatus()),
                () -> assertFalse(a.isLateCancellation()));
    }

    @Test
    @DisplayName("TC-B-02 appointment IDs are unique and sequential")
    void idsAreSequential() {
        Appointment first = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        Appointment second = service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 30));

        assertNotEquals(first.getId(), second.getId());
        assertEquals("A0002", second.getId());
    }

    @Nested
    @DisplayName("Decision table rules")
    class DecisionTable {

        @Test
        @Tag("DecisionTable")
        @DisplayName("TC-B-03 R1 all conditions true -> booked")
        void r1Booked() {
            assertDoesNotThrow(() -> service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0)));
        }

        @Test
        @Tag("DecisionTable")
        @Tag("Negative")
        @DisplayName("TC-B-04 R2 doctor already booked at that time -> rejected (double booking)")
        void r2DoctorBusy() {
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

            assertThrows(SchedulingException.class, () -> service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0)));
        }

        @Test
        @Tag("DecisionTable")
        @Tag("Negative")
        @DisplayName("TC-B-05 R3 patient already has another doctor at that time -> rejected")
        void r3PatientBusy() {
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

            assertThrows(SchedulingException.class, () -> service.book(PATIENT_1, DOCTOR_2, tuesdayAt(10, 0)));
        }

        @Test
        @Tag("DecisionTable")
        @Tag("BVA")
        @DisplayName("TC-B-06 R4 doctor daily limit (2): 2nd booking accepted, 3rd rejected")
        void r4DoctorLimitReached() {
            service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
            assertDoesNotThrow(() -> service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30)));

            assertThrows(SchedulingException.class, () -> service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(10, 0)));
        }

        @Test
        @Tag("DecisionTable")
        @Tag("BVA")
        @DisplayName("TC-B-07 R5 patient active limit (3): 3rd booking accepted, 4th rejected")
        void r5PatientLimitReached() {
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 0));
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 30));
            assertDoesNotThrow(() -> service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0)));

            assertThrows(SchedulingException.class, () -> service.book(PATIENT_1, DOCTOR_2, tuesdayAt(14, 0)));
        }
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-B-08 two different doctors can see two patients at the same time")
    void differentDoctorsSameTime() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        assertDoesNotThrow(() -> service.book(PATIENT_2, DOCTOR_2, tuesdayAt(10, 0)));
    }

    @Test
    @DisplayName("TC-B-09 a cancelled slot becomes free again")
    void cancelledSlotCanBeRebooked() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        service.cancel(a.getId(), Requester.PATIENT);

        assertDoesNotThrow(() -> service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0)));
    }

    @Test
    @DisplayName("TC-B-10 cancelled appointments do not count towards the doctor's daily limit")
    void cancelledDoesNotCountForDoctorLimit() {
        Appointment a = service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30));
        service.cancel(a.getId(), Requester.PATIENT);

        assertDoesNotThrow(() -> service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(10, 0)));
    }

    @Test
    @DisplayName("TC-B-11 cancelled appointments do not count towards the patient's active limit")
    void cancelledDoesNotCountForPatientLimit() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 0));
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 30));
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        service.cancel(a.getId(), Requester.PATIENT);

        assertDoesNotThrow(() -> service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 30)));
    }

    @Test
    @DisplayName("TC-B-12 the doctor limit is per day: a full day does not block the next day")
    void doctorLimitIsPerDay() {
        service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30));

        assertDoesNotThrow(() -> service.book(PATIENT_3, LIMITED_DOCTOR, TUESDAY.plusDays(1).atTime(9, 0)));
    }

    @Test
    @DisplayName("TC-B-13 a failed booking does not change the system state")
    void failedBookingHasNoSideEffects() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        assertThrows(SchedulingException.class, () -> service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0)));

        assertEquals(0, service.getPatientAppointments(PATIENT_2).size());
        assertEquals(1, service.getDoctorSchedule(DOCTOR_1, TUESDAY).size());
    }

    @ParameterizedTest(name = "TC-B-14 unknown patient ID \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"99999999999"})
    void unknownPatientRejected(String patientId) {
        assertThrows(ValidationException.class, () -> service.book(patientId, DOCTOR_1, tuesdayAt(10, 0)));
    }

    @ParameterizedTest(name = "TC-B-15 unknown doctor ID \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"D99"})
    void unknownDoctorRejected(String doctorId) {
        assertThrows(ValidationException.class, () -> service.book(PATIENT_1, doctorId, tuesdayAt(10, 0)));
    }

    @Nested
    @DisplayName("FR-05 Registration")
    class Registration {

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-01 registering the same patient ID twice is rejected")
        void duplicatePatientRejected() {
            Patient duplicate = new Patient(PATIENT_1, "Other Person", 20);
            assertThrows(ValidationException.class, () -> service.registerPatient(duplicate));
        }

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-02 registering the same doctor ID twice is rejected")
        void duplicateDoctorRejected() {
            Doctor duplicate = new Doctor(DOCTOR_1, "Other Doctor", "Surgery", 5);
            assertThrows(ValidationException.class, () -> service.registerDoctor(duplicate));
        }

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-03 null patient/doctor is rejected")
        void nullRejected() {
            assertThrows(ValidationException.class, () -> service.registerPatient(null));
            assertThrows(ValidationException.class, () -> service.registerDoctor(null));
        }

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-04 service cannot be created without a clock")
        void nullClockRejected() {
            assertThrows(NullPointerException.class, () -> new AppointmentService(null));
        }
    }
}
