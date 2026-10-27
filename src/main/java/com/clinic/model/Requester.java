package com.clinic.model;

/**
 * Who asks for a cancellation or reschedule. Staff may override the late-cancellation rule.
 */
public enum Requester {
    PATIENT,
    STAFF
}
