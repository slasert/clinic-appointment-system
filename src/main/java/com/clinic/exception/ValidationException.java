package com.clinic.exception;

/**
 * Thrown when an input value is malformed (null, wrong format, out of range).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
