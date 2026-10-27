package com.clinic.appointment;

import com.clinic.exception.ClinicException;
import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-03 Time slot rules, tested through {@link AppointmentService#book}. "Now" is Monday 2026-11-02 08:00.
 *
 * <pre>
 * Boundary            below (invalid)  at (valid)     above
 * Opening 09:00       08:30            09:00          09:30 valid
 * Last start 16:30    16:00 valid      16:30          17:00 invalid
 * Lunch 12:00-13:00   11:30 valid      12:00 invalid  12:30 invalid, 13:00 valid
 * Lead time 1 h       now 08:01        now 08:00      now 07:59 valid
 * Advance 30 days     29 days valid    30 days        31 days invalid
 * </pre>
 * Every rejection also checks that no appointment was stored.
 */
@DisplayName("FR-03 Time slot validation")
class SlotValidationTest extends ServiceTestBase {

    private Appointment book(LocalDateTime start) {
        return service.book(PATIENT_1, DOCTOR_1, start);
    }

    private void assertRejectedAndNothingStored(Class<? extends ClinicException> type, BusinessRule rule, LocalDateTime start) {
        assertRejected(type, rule, () -> book(start));
        assertEquals(0, storedAppointments());
    }

    @ParameterizedTest(name = "TC-S-01 start {0} is inside working hours")
    @Tag("BVA")
    @Tag("EP")
    @CsvSource({"09:00", "09:30", "11:30", "13:00", "16:00", "16:30"})
    void validWorkingHours(LocalTime time) {
        Appointment appointment = book(TUESDAY.atTime(time));

        assertEquals(TUESDAY.atTime(time), appointment.getStart());
    }

    @ParameterizedTest(name = "TC-S-02 start {0} is outside working hours")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"00:00", "08:00", "08:30", "17:00", "17:30", "23:30"})
    void outsideWorkingHoursRejected(LocalTime time) {
        assertRejectedAndNothingStored(SchedulingException.class, BusinessRule.WORKING_HOURS, TUESDAY.atTime(time));
    }

    @ParameterizedTest(name = "TC-S-03 start {0} falls in the lunch break")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"12:00", "12:30"})
    void lunchBreakRejected(LocalTime time) {
        assertRejectedAndNothingStored(SchedulingException.class, BusinessRule.LUNCH_BREAK, TUESDAY.atTime(time));
    }

    @ParameterizedTest(name = "TC-S-04 start {0} is not aligned to a 30 minute slot")
    @Tag("EP")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"09:01", "09:15", "09:29", "09:31", "16:59"})
    void misalignedMinutesRejected(LocalTime time) {
        assertRejectedAndNothingStored(ValidationException.class, BusinessRule.SLOT_ALIGNMENT, TUESDAY.atTime(time));
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-S-05 start time with one second or one nanosecond is rejected")
    void secondsRejected() {
        assertRejectedAndNothingStored(ValidationException.class, BusinessRule.SLOT_ALIGNMENT, tuesdayAt(10, 0).withSecond(1));
        assertRejectedAndNothingStored(ValidationException.class, BusinessRule.SLOT_ALIGNMENT, tuesdayAt(10, 0).withNano(1));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-S-06 null start time is rejected")
    void nullStartRejected() {
        assertRejectedAndNothingStored(ValidationException.class, BusinessRule.REQUIRED_INPUT, null);
    }

    @ParameterizedTest(name = "TC-S-07 {0} ({1}) is a working day")
    @Tag("EP")
    @CsvSource({"2026-11-06, Friday", "2026-11-09, Monday"})
    void weekdayAccepted(LocalDate date, String day) {
        Appointment appointment = book(date.atTime(10, 0));

        assertEquals(date, appointment.getDate());
    }

    @ParameterizedTest(name = "TC-S-08 {0} ({1}) is a weekend")
    @Tag("EP")
    @Tag("Negative")
    @CsvSource({"2026-11-07, Saturday", "2026-11-08, Sunday"})
    void weekendRejected(LocalDate date, String day) {
        assertRejectedAndNothingStored(SchedulingException.class, BusinessRule.WORKING_DAY, date.atTime(10, 0));
    }

    // ---- minimum lead time: 1 hour. Appointment Monday 09:00, the clock is moved around 08:00.

    @ParameterizedTest(name = "TC-S-09 booking 09:00 when now is {0} -> accepted")
    @Tag("BVA")
    @CsvSource({"07:00", "07:59", "08:00"})
    void leadTimeBoundaryAccepted(LocalTime now) {
        clock.setTo(MONDAY.atTime(now));

        Appointment appointment = book(MONDAY.atTime(9, 0));

        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @ParameterizedTest(name = "TC-S-10 booking 09:00 when now is {0} -> rejected")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"08:01", "08:59", "09:00", "10:00"})
    void leadTimeBoundaryRejected(LocalTime now) {
        clock.setTo(MONDAY.atTime(now));

        assertRejectedAndNothingStored(SchedulingException.class, BusinessRule.MIN_LEAD_TIME, MONDAY.atTime(9, 0));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-S-11 a date in the past is rejected")
    void pastDateRejected() {
        assertRejectedAndNothingStored(SchedulingException.class, BusinessRule.MIN_LEAD_TIME,
                LocalDate.of(2026, 10, 30).atTime(10, 0));
    }

    // ---- maximum advance: 30 days. Today + 30 = Wednesday 2026-12-02.

    @ParameterizedTest(name = "TC-S-12 booking on {0} (today + {1} days) -> accepted")
    @Tag("BVA")
    @CsvSource({"2026-12-01, 29", "2026-12-02, 30"})
    void maxAdvanceBoundaryAccepted(LocalDate date, int days) {
        Appointment appointment = book(date.atTime(16, 30));

        assertEquals(date, appointment.getDate());
    }

    @ParameterizedTest(name = "TC-S-13 booking on {0} (today + {1} days) -> rejected")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"2026-12-03, 31", "2027-11-02, 365"})
    void maxAdvanceBoundaryRejected(LocalDate date, int days) {
        assertRejectedAndNothingStored(SchedulingException.class, BusinessRule.MAX_ADVANCE, date.atTime(9, 0));
    }
}
