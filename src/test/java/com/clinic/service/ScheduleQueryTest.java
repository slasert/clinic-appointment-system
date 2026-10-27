package com.clinic.service;

import com.clinic.exception.ValidationException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Requester;
import com.clinic.rules.ClinicRules;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-09 Schedule queries: doctor's daily schedule, patient history and free slots.
 */
@DisplayName("FR-09 Schedule queries")
class ScheduleQueryTest extends ServiceTestBase {

    @Test
    @DisplayName("TC-Q-01 an empty working day has 14 free slots: 09:00..11:30 and 13:00..16:30")
    void emptyDayHasFourteenSlots() {
        List<LocalDateTime> slots = service.getAvailableSlots(DOCTOR_1, TUESDAY);

        assertEquals(ClinicRules.SLOTS_PER_DAY, slots.size());
        assertEquals(tuesdayAt(9, 0), slots.get(0));
        assertEquals(tuesdayAt(11, 30), slots.get(5));
        assertEquals(tuesdayAt(13, 0), slots.get(6));
        assertEquals(tuesdayAt(16, 30), slots.get(13));
    }

    @Test
    @DisplayName("TC-Q-02 booked slots are not offered, cancelled ones are offered again")
    void bookedSlotsRemoved() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
        assertFalse(service.getAvailableSlots(DOCTOR_1, TUESDAY).contains(tuesdayAt(10, 0)));

        service.cancel(a.getId(), Requester.PATIENT);
        assertTrue(service.getAvailableSlots(DOCTOR_1, TUESDAY).contains(tuesdayAt(10, 0)));
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-Q-03 no free slots on a weekend or beyond 30 days")
    void noSlotsOnInvalidDays() {
        assertTrue(service.getAvailableSlots(DOCTOR_1, LocalDate.of(2026, 11, 7)).isEmpty());
        assertTrue(service.getAvailableSlots(DOCTOR_1, LocalDate.of(2026, 12, 3)).isEmpty());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-Q-04 today only offers slots at least 1 hour ahead (now 10:00 -> first slot 11:00)")
    void todayRespectsLeadTime() {
        clock.setTo(MONDAY.atTime(10, 0));

        List<LocalDateTime> slots = service.getAvailableSlots(DOCTOR_1, MONDAY);

        assertEquals(MONDAY.atTime(11, 0), slots.get(0));
    }

    @Test
    @DisplayName("TC-Q-05 a doctor at the daily limit has no free slots")
    void noSlotsWhenLimitReached() {
        service.book(PATIENT_1, LIMITED_DOCTOR, tuesdayAt(9, 0));
        service.book(PATIENT_2, LIMITED_DOCTOR, tuesdayAt(9, 30));

        assertTrue(service.getAvailableSlots(LIMITED_DOCTOR, TUESDAY).isEmpty());
    }

    @Test
    @DisplayName("TC-Q-06 every offered slot can actually be booked")
    void offeredSlotsAreBookable() {
        clock.setTo(MONDAY.atTime(10, 0));
        String[] patients = {PATIENT_1, PATIENT_2, PATIENT_3, PATIENT_4};
        int i = 0;
        for (LocalDateTime slot : service.getAvailableSlots(DOCTOR_2, MONDAY)) {
            service.book(patients[i++ % patients.length], DOCTOR_2, slot);
        }
        assertTrue(service.getAvailableSlots(DOCTOR_2, MONDAY).isEmpty());
    }

    @Test
    @DisplayName("TC-Q-07 doctor schedule is sorted by time and excludes cancelled appointments")
    void doctorScheduleSortedAndFiltered() {
        service.book(PATIENT_1, DOCTOR_1, tuesdayAt(15, 0));
        service.book(PATIENT_2, DOCTOR_1, tuesdayAt(9, 0));
        Appointment cancelled = service.book(PATIENT_3, DOCTOR_1, tuesdayAt(11, 0));
        service.book(PATIENT_4, DOCTOR_2, tuesdayAt(10, 0));
        service.cancel(cancelled.getId(), Requester.STAFF);

        List<Appointment> schedule = service.getDoctorSchedule(DOCTOR_1, TUESDAY);

        assertEquals(2, schedule.size());
        assertEquals(tuesdayAt(9, 0), schedule.get(0).getStart());
        assertEquals(tuesdayAt(15, 0), schedule.get(1).getStart());
    }

    @Test
    @DisplayName("TC-Q-08 patient history contains every status, sorted by time")
    void patientHistory() {
        Appointment a = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(15, 0));
        service.book(PATIENT_1, DOCTOR_2, tuesdayAt(9, 0));
        service.cancel(a.getId(), Requester.PATIENT);

        List<Appointment> history = service.getPatientAppointments(PATIENT_1);

        assertEquals(2, history.size());
        assertEquals(tuesdayAt(9, 0), history.get(0).getStart());
        assertEquals(AppointmentStatus.CANCELLED, history.get(1).getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-Q-09 queries with unknown IDs or null date are rejected")
    void invalidQueryArguments() {
        assertThrows(ValidationException.class, () -> service.getAvailableSlots("D99", TUESDAY));
        assertThrows(ValidationException.class, () -> service.getAvailableSlots(DOCTOR_1, null));
        assertThrows(ValidationException.class, () -> service.getDoctorSchedule(DOCTOR_1, null));
        assertThrows(ValidationException.class, () -> service.getPatientAppointments("99999999999"));
        assertThrows(ValidationException.class, () -> service.findAppointment("A0001"));
    }
}
