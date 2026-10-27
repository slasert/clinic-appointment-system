package com.clinic.support;

import com.clinic.exception.ClinicException;
import com.clinic.rules.BusinessRule;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Asserts that an operation is rejected for one specific reason. Checking only the
 * exception type is not enough: many different rules throw SchedulingException, so a
 * test could otherwise pass because of the wrong rule.
 */
public final class RuleAssertions {

    private RuleAssertions() {
    }

    public static <T extends ClinicException> T assertRejected(Class<T> type, BusinessRule rule, Executable action) {
        T exception = assertThrows(type, action);
        assertEquals(rule, exception.getRule(), () -> "Rejected for the wrong reason: " + exception.getMessage());
        return exception;
    }
}
