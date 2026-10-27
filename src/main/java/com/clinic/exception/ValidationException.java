package com.clinic.exception;

import com.clinic.rules.BusinessRule;

/**
 * Thrown when an input value is malformed (null, wrong format, out of range, unknown ID).
 */
public class ValidationException extends ClinicException {

    public ValidationException(BusinessRule rule, String message) {
        super(rule, message);
    }
}
