package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-03 Time slot rules. "Now" is Monday 2026-11-02 08:00.
 * <ul>
 *   <li>Working hours: start 09:00..16:30 (slot ends by 17:00)</li>
 *   <li>Lunch break: 12:00..13:00 not bookable</li>
 *   <li>Start must be on :00 or :30</li>
 *   <li>Weekdays only</li>
 *   <li>At least 1 hour ahead, at most 30 calendar days ahead</li>
 * </ul>
 */
@DisplayName("FR-03 Time slot validation")
class SlotValidationTest extends ServiceTestBase {

    private void book(LocalDateTime start) {
        service.book(PATIENT_1, DOCTOR_1, start);
    }

    @ParameterizedTest(name = "TC-S-01 start {0} is inside working hours")
    @Tag("BVA")
    @CsvSource({"09:00", "09:30", "11:30", "13:00", "16:00", "16:30"})
    void validWorkingHours(LocalTime time) {
        assertDoesNotThrow(() -> book(TUESDAY.atTime(time)));
    }

    @ParameterizedTest(name = "TC-S-02 start {0} is outside working hours")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"00:00", "08:00", "08:30", "17:00", "17:30", "23:30"})
    void outsideWorkingHoursRejected(LocalTime time) {
        assertThrows(SchedulingException.class, () -> book(TUESDAY.atTime(time)));
    }

    @ParameterizedTest(name = "TC-S-03 start {0} falls in the lunch break")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"12:00", "12:30"})
    void lunchBreakRejected(LocalTime time) {
        assertThrows(SchedulingException.class, () -> book(TUESDAY.atTime(time)));
    }

    @ParameterizedTest(name = "TC-S-04 start {0} is not aligned to a 30 minute slot")
    @Tag("EP")
    @Tag("Negative")
    @CsvSource({"09:01", "09:15", "09:29", "09:31", "16:59"})
    void misalignedMinutesRejected(LocalTime time) {
        assertThrows(ValidationException.class, () -> book(TUESDAY.atTime(time)));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-S-05 start time with seconds is rejected")
    void secondsRejected() {
        assertThrows(ValidationException.class, () -> book(tuesdayAt(10, 0).withSecond(1)));
        assertThrows(ValidationException.class, () -> book(tuesdayAt(10, 0).withNano(1)));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-S-06 null start time is rejected")
    void nullStartRejected() {
        assertThrows(ValidationException.class, () -> book(null));
    }

    @ParameterizedTest(name = "TC-S-07 {0} ({1}) is a working day")
    @Tag("EP")
    @CsvSource({"2026-11-06, Friday", "2026-11-09, Monday"})
    void weekdayAccepted(LocalDate date, String day) {
        assertDoesNotThrow(() -> book(date.atTime(10, 0)));
    }

    @ParameterizedTest(name = "TC-S-08 {0} ({1}) is a weekend")
    @Tag("EP")
    @Tag("Negative")
    @CsvSource({"2026-11-07, Saturday", "2026-11-08, Sunday"})
    void weekendRejected(LocalDate date, String day) {
        assertThrows(SchedulingException.class, () -> book(date.atTime(10, 0)));
    }

    // ---- minimum lead time: 1 hour. Appointment Monday 09:00, the clock is moved around 08:00.

    @ParameterizedTest(name = "TC-S-09 booking 09:00 when now is {0} -> accepted")
    @Tag("BVA")
    @CsvSource({"07:00", "07:59", "08:00"})
    void leadTimeBoundaryAccepted(LocalTime now) {
        clock.setTo(MONDAY.atTime(now));
        assertDoesNotThrow(() -> book(MONDAY.atTime(9, 0)));
    }

    @ParameterizedTest(name = "TC-S-10 booking 09:00 when now is {0} -> rejected")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"08:01", "08:59", "09:00", "10:00"})
    void leadTimeBoundaryRejected(LocalTime now) {
        clock.setTo(MONDAY.atTime(now));
        assertThrows(SchedulingException.class, () -> book(MONDAY.atTime(9, 0)));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-S-11 a date in the past is rejected")
    void pastDateRejected() {
        assertThrows(SchedulingException.class, () -> book(LocalDate.of(2026, 10, 30).atTime(10, 0)));
    }

    // ---- maximum advance: 30 days. Today + 30 = Wednesday 2026-12-02.

    @ParameterizedTest(name = "TC-S-12 booking on {0} (today + {1} days) -> accepted")
    @Tag("BVA")
    @CsvSource({"2026-12-01, 29", "2026-12-02, 30"})
    void maxAdvanceBoundaryAccepted(LocalDate date, int days) {
        assertDoesNotThrow(() -> book(date.atTime(16, 30)));
    }

    @ParameterizedTest(name = "TC-S-13 booking on {0} (today + {1} days) -> rejected")
    @Tag("BVA")
    @Tag("Negative")
    @CsvSource({"2026-12-03, 31", "2027-11-02, 365"})
    void maxAdvanceBoundaryRejected(LocalDate date, int days) {
        assertThrows(SchedulingException.class, () -> book(date.atTime(9, 0)));
    }
}
