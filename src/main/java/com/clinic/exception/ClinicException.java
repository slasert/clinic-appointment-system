package com.clinic.exception;

import com.clinic.rules.BusinessRule;

import java.util.Objects;

/**
 * Base class of every rejection in the system. Carries the business rule that was violated.
 */
public abstract class ClinicException extends RuntimeException {

    private final BusinessRule rule;

    protected ClinicException(BusinessRule rule, String message) {
        super("[" + rule.getId() + "] " + message);
        this.rule = Objects.requireNonNull(rule);
    }

    public BusinessRule getRule() {
        return rule;
    }
}
