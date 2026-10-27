package com.clinic.exception;

import com.clinic.rules.BusinessRule;

/**
 * Thrown when a well-formed request violates a clinic business rule
 * (double booking, working hours, cancellation policy, invalid status change...).
 */
public class SchedulingException extends ClinicException {

    public SchedulingException(BusinessRule rule, String message) {
        super(rule, message);
    }
}
