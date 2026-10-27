package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import com.clinic.rules.ClinicRules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Time rules for an appointment start (BR-08 to BR-14).
 *
 * <p>Every method is a pure function of the requested start and the current time,
 * so the rules can be tested without a service and without a clock.</p>
 */
final class TimeSlotPolicy {

    private TimeSlotPolicy() {
    }

    /**
     * Returns the first rule the start time breaks, or empty if it can be booked.
     * Rules are checked in a fixed order so the reported reason is predictable.
     */
    static Optional<BusinessRule> findViolation(LocalDateTime start, LocalDateTime now) {
        if (start == null) {
            return Optional.of(BusinessRule.REQUIRED_INPUT);
        }
        if (start.getSecond() != 0 || start.getNano() != 0 || start.getMinute() % ClinicRules.SLOT_MINUTES != 0) {
            return Optional.of(BusinessRule.SLOT_ALIGNMENT);
        }
        DayOfWeek day = start.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return Optional.of(BusinessRule.WORKING_DAY);
        }
        LocalTime time = start.toLocalTime();
        if (time.isBefore(ClinicRules.OPENING_TIME) || time.isAfter(ClinicRules.LAST_SLOT_START)) {
            return Optional.of(BusinessRule.WORKING_HOURS);
        }
        if (!time.isBefore(ClinicRules.LUNCH_START) && time.isBefore(ClinicRules.LUNCH_END)) {
            return Optional.of(BusinessRule.LUNCH_BREAK);
        }
        if (start.isBefore(now.plusHours(ClinicRules.MIN_LEAD_HOURS))) {
            return Optional.of(BusinessRule.MIN_LEAD_TIME);
        }
        if (start.toLocalDate().isAfter(now.toLocalDate().plusDays(ClinicRules.MAX_ADVANCE_DAYS))) {
            return Optional.of(BusinessRule.MAX_ADVANCE);
        }
        return Optional.empty();
    }

    /**
     * Throws if the start time breaks a rule: {@link ValidationException} for malformed input
     * (missing or not on a slot boundary), {@link SchedulingException} for a clinic rule.
     */
    static void validate(LocalDateTime start, LocalDateTime now) {
        Optional<BusinessRule> violation = findViolation(start, now);
        if (violation.isEmpty()) {
            return;
        }
        BusinessRule rule = violation.get();
        String message = rule.getDescription() + (start == null ? "" : " (requested " + start + ")");
        if (rule == BusinessRule.REQUIRED_INPUT || rule == BusinessRule.SLOT_ALIGNMENT) {
            throw new ValidationException(rule, message);
        }
        throw new SchedulingException(rule, message);
    }

    /** All slot start times of a working day (09:00 ... 16:30 without lunch), before date-specific rules. */
    static List<LocalDateTime> dailySlots(LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();
        for (LocalTime time = ClinicRules.OPENING_TIME;
             !time.isAfter(ClinicRules.LAST_SLOT_START);
             time = time.plusMinutes(ClinicRules.SLOT_MINUTES)) {
            if (time.isBefore(ClinicRules.LUNCH_START) || !time.isBefore(ClinicRules.LUNCH_END)) {
                slots.add(date.atTime(time));
            }
        }
        return slots;
    }
}
