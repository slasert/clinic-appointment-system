package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import com.clinic.rules.ClinicRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of {@link TimeSlotPolicy} without a service or clock. The boundaries themselves are
 * covered in {@link SlotValidationTest}; this class checks what only the policy decides:
 * the order of the rules, the mapping to exception types and the daily slot grid.
 */
@DisplayName("TimeSlotPolicy unit tests")
class TimeSlotPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 8, 0);

    @ParameterizedTest(name = "TC-TP-01 {0} breaks several rules -> first reported is {1}")
    @CsvSource({
            "2026-11-07T12:15, SLOT_ALIGNMENT",   // misaligned + weekend + lunch
            "2026-11-07T12:00, WORKING_DAY",      // weekend + lunch
            "2026-11-02T08:30, WORKING_HOURS",    // before opening + inside lead time
            "2026-12-31T12:00, LUNCH_BREAK"       // lunch + too far ahead
    })
    void rulesAreCheckedInFixedOrder(LocalDateTime start, BusinessRule expected) {
        assertEquals(Optional.of(expected), TimeSlotPolicy.findViolation(start, NOW));
    }

    @Test
    @DisplayName("TC-TP-02 a valid start has no violation")
    void validStartHasNoViolation() {
        assertTrue(TimeSlotPolicy.findViolation(LocalDateTime.of(2026, 11, 3, 10, 0), NOW).isEmpty());
        assertDoesNotThrow(() -> TimeSlotPolicy.validate(LocalDateTime.of(2026, 11, 3, 10, 0), NOW));
    }

    @Test
    @DisplayName("TC-TP-03 malformed input -> ValidationException, rule violation -> SchedulingException")
    void exceptionTypeDependsOnRule() {
        assertRejected(ValidationException.class, BusinessRule.REQUIRED_INPUT, () -> TimeSlotPolicy.validate(null, NOW));
        assertRejected(ValidationException.class, BusinessRule.SLOT_ALIGNMENT,
                () -> TimeSlotPolicy.validate(LocalDateTime.of(2026, 11, 3, 10, 10), NOW));
        assertRejected(SchedulingException.class, BusinessRule.LUNCH_BREAK,
                () -> TimeSlotPolicy.validate(LocalDateTime.of(2026, 11, 3, 12, 0), NOW));
    }

    @Test
    @DisplayName("TC-TP-04 the daily grid has 14 slots, 09:00-11:30 and 13:00-16:30, none in lunch")
    void dailySlotGrid() {
        LocalDate day = LocalDate.of(2026, 11, 3);

        List<LocalDateTime> slots = TimeSlotPolicy.dailySlots(day);

        assertEquals(ClinicRules.SLOTS_PER_DAY, slots.size());
        assertEquals(day.atTime(9, 0), slots.get(0));
        assertEquals(day.atTime(11, 30), slots.get(5));
        assertEquals(day.atTime(13, 0), slots.get(6));
        assertEquals(day.atTime(16, 30), slots.get(slots.size() - 1));
        assertFalse(slots.contains(day.atTime(12, 0)) || slots.contains(day.atTime(12, 30)));
    }
}
