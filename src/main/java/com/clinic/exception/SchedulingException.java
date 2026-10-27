package com.clinic.exception;

/**
 * Thrown when a well-formed request violates a clinic business rule
 * (double booking, working hours, cancellation policy, invalid status change...).
 */
public class SchedulingException extends RuntimeException {

    public SchedulingException(String message) {
        super(message);
    }
}
