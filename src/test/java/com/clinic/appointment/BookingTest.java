package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Requester;
import com.clinic.rules.BusinessRule;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-04 Booking: conflicts and limits; FR-05 registration.
 *
 * <p>Decision table for a booking request with a valid time slot (conditions are checked in this order):</p>
 * <pre>
 *                      R1  R2  R3  R4  R5
 * Doctor free           Y   N   Y   Y   Y
 * Patient free          Y   -   N   Y   Y
 * Doctor under limit    Y   -   -   N   Y
 * Patient under limit   Y   -   -   -   N
 * ---------------------------------------
 * Result              BOOK BR15 BR16 BR17 BR18
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
                () -> assertFalse(a.isLateCancellation()),
                () -> assertEquals(a, service.findAppointment("A0001")));
    }

    @Test
    @DisplayName("TC-B-02 appointment IDs are unique and sequential")
    void idsAreSequential() {
        Appointment first = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        Appointment second = service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 30));

        assertEquals("A0001", first.getId());
        assertEquals("A0002", second.getId());
    }

    @Nested
    @DisplayName("Decision table rules")
    class DecisionTable {

        @Test
        @Tag("DecisionTable")
        @DisplayName("TC-B-03 R1 all conditions true -> booked")
        void r1Booked() {
            Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

            assertEquals(AppointmentStatus.SCHEDULED, a.getStatus());
            assertEquals(1, storedAppointments());
        }

        @Test
        @Tag("DecisionTable")
        @Tag("Negative")
        @DisplayName("TC-B-04 R2 doctor already booked at that time -> rejected (double booking)")
        void r2DoctorBusy() {
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

            assertRejected(SchedulingException.class, BusinessRule.DOCTOR_CONFLICT,
                    () -> service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0)));
            assertEquals(1, storedAppointments());
        }

        @Test
        @Tag("DecisionTable")
        @Tag("Negative")
        @DisplayName("TC-B-05 R3 patient already has another doctor at that time -> rejected")
        void r3PatientBusy() {
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

            assertRejected(SchedulingException.class, BusinessRule.PATIENT_CONFLICT,
                    () -> service.book(PATIENT_1, DOCTOR_2, tuesdayAt(10, 0)));
            assertEquals(1, storedAppointments());
        }

        @Test
        @Tag("DecisionTable")
        @Tag("BVA")
        @DisplayName("TC-B-06 R4 doctor daily limit (2): 2nd booking accepted, 3rd rejected")
        void r4DoctorLimitReached() {
            service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
            service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30));   // at the limit: accepted

            assertRejected(SchedulingException.class, BusinessRule.DOCTOR_DAILY_LIMIT,
                    () -> service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(10, 0)));
            assertEquals(2, storedAppointments());
        }

        @Test
        @Tag("DecisionTable")
        @Tag("BVA")
        @DisplayName("TC-B-07 R5 patient active limit (3): 3rd booking accepted, 4th rejected")
        void r5PatientLimitReached() {
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 0));
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 30));
            service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));         // at the limit: accepted

            assertRejected(SchedulingException.class, BusinessRule.PATIENT_ACTIVE_LIMIT,
                    () -> service.book(PATIENT_1, DOCTOR_2, tuesdayAt(14, 0)));
            assertEquals(3, storedAppointments());
        }
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-B-08 two different doctors can see two patients at the same time")
    void differentDoctorsSameTime() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        service.book(PATIENT_2, DOCTOR_2, tuesdayAt(10, 0));

        assertEquals(2, storedAppointments());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-B-18 doctor back-to-back: slots directly before and after a booked slot are free")
    void doctorBackToBackAllowed() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        service.book(PATIENT_2, DOCTOR_1, tuesdayAt(9, 30));
        service.book(PATIENT_3, DOCTOR_1, tuesdayAt(10, 30));

        assertEquals(3, service.getDoctorSchedule(DOCTOR_1, TUESDAY).size());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-B-19 patient back-to-back with two doctors is allowed")
    void patientBackToBackAllowed() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        service.book(PATIENT_1, DOCTOR_2, tuesdayAt(10, 30));

        assertEquals(2, service.getPatientAppointments(PATIENT_1).size());
    }

    @Test
    @DisplayName("TC-B-09 a cancelled slot becomes free again")
    void cancelledSlotCanBeRebooked() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        service.cancel(a.getId(), Requester.PATIENT);

        Appointment rebooked = service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0));

        assertEquals(AppointmentStatus.SCHEDULED, rebooked.getStatus());
    }

    @Test
    @DisplayName("TC-B-10 cancelled appointments do not count towards the doctor's daily limit")
    void cancelledDoesNotCountForDoctorLimit() {
        Appointment a = service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30));
        service.cancel(a.getId(), Requester.PATIENT);

        service.book(PATIENT_3, LIMITED_DOCTOR, tuesdayAt(10, 0));

        assertEquals(2, service.getDoctorSchedule(LIMITED_DOCTOR, TUESDAY).size());
    }

    @Test
    @DisplayName("TC-B-11 cancelled appointments do not count towards the patient's active limit")
    void cancelledDoesNotCountForPatientLimit() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 0));
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(9, 30));
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        service.cancel(a.getId(), Requester.PATIENT);

        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 30));

        assertEquals(4, service.getPatientAppointments(PATIENT_1).size());
    }

    @Test
    @DisplayName("TC-B-12 the doctor limit is per day: a full day does not block the next day")
    void doctorLimitIsPerDay() {
        service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30));

        Appointment nextDay = service.book(PATIENT_3, LIMITED_DOCTOR, TUESDAY.plusDays(1).atTime(9, 0));

        assertEquals(TUESDAY.plusDays(1), nextDay.getDate());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-B-13 a failed booking does not change schedules, history or the next ID")
    void failedBookingHasNoSideEffects() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        assertThrows(SchedulingException.class, () -> service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 0)));

        assertEquals(0, service.getPatientAppointments(PATIENT_2).size());
        assertEquals(1, service.getDoctorSchedule(DOCTOR_1, TUESDAY).size());
        assertEquals("A0002", service.book(PATIENT_2, DOCTOR_1, tuesdayAt(10, 30)).getId());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-B-16 [DEF-01] past appointments that were never closed do not count as active")
    void pastScheduledAppointmentsAreNotActive() {
        clock.setTo(MONDAY.atTime(7, 0));
        service.book(PATIENT_1, DOCTOR_1, MONDAY.atTime(9, 0));
        service.book(PATIENT_1, DOCTOR_1, MONDAY.atTime(9, 30));
        service.book(PATIENT_1, DOCTOR_1, MONDAY.atTime(10, 0));
        // Next day: the three visits are over but staff never marked them COMPLETED.
        clock.setTo(TUESDAY.atTime(8, 0));

        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(14, 0));

        assertEquals(AppointmentStatus.SCHEDULED, a.getStatus());
    }

    @Test
    @DisplayName("TC-B-17 [DEF-02] appointment IDs do not depend on the default locale")
    void idsAreLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"));

            Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

            assertEquals("A0001", a.getId());
        } finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest(name = "TC-B-14 unknown patient ID \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"99999999999"})
    void unknownPatientRejected(String patientId) {
        assertRejected(ValidationException.class, BusinessRule.KNOWN_REFERENCE,
                () -> service.book(patientId, DOCTOR_1, tuesdayAt(10, 0)));
        assertEquals(0, storedAppointments());
    }

    @ParameterizedTest(name = "TC-B-15 unknown doctor ID \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"D99", "d1"})
    void unknownDoctorRejected(String doctorId) {
        assertRejected(ValidationException.class, BusinessRule.KNOWN_REFERENCE,
                () -> service.book(PATIENT_1, doctorId, tuesdayAt(10, 0)));
        assertEquals(0, storedAppointments());
    }

    @Nested
    @DisplayName("FR-05 Registration")
    class Registration {

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-01 registering the same patient ID twice is rejected; the first patient is kept")
        void duplicatePatientRejected() {
            Patient duplicate = new Patient(PATIENT_1, "Other Person", 20);

            assertRejected(ValidationException.class, BusinessRule.UNIQUE_ID, () -> service.registerPatient(duplicate));
            assertEquals("Mehmet Kaya",
                    service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0)).getPatient().getName());
        }

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-02 registering the same doctor ID twice is rejected; the first doctor is kept")
        void duplicateDoctorRejected() {
            Doctor duplicate = new Doctor(DOCTOR_1, "Other Doctor", "Surgery", 5);

            assertRejected(ValidationException.class, BusinessRule.UNIQUE_ID, () -> service.registerDoctor(duplicate));
            assertEquals("Cardiology",
                    service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0)).getDoctor().getSpecialty());
        }

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-03 null patient/doctor is rejected")
        void nullRejected() {
            assertRejected(ValidationException.class, BusinessRule.REQUIRED_INPUT, () -> service.registerPatient(null));
            assertRejected(ValidationException.class, BusinessRule.REQUIRED_INPUT, () -> service.registerDoctor(null));
        }

        @Test
        @Tag("Negative")
        @DisplayName("TC-R-04 service cannot be created without a clock")
        void nullClockRejected() {
            assertThrows(NullPointerException.class, () -> new AppointmentService(null));
        }
    }
}
