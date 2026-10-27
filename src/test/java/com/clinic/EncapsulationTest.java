package com.clinic;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Appointment;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DEF-04 reproduction: code outside the service package receives Appointment objects
 * from the service. It must not be able to change them in a way that breaks FR-08 / FR-10.
 */
@DisplayName("DEF-04 Rules cannot be bypassed from outside the service")
class EncapsulationTest extends ServiceTestBase {

    @Test
    @DisplayName("TC-E-01 a 500 character note added directly to a returned appointment is rejected")
    void noteRuleCannotBeBypassed() {
        Appointment appointment = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        assertThrows(ValidationException.class, () -> appointment.addNote("x".repeat(500)));
    }

    @Test
    @DisplayName("TC-E-02 a returned appointment cannot be completed before it starts")
    void completionRuleCannotBeBypassed() {
        Appointment appointment = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        assertThrows(SchedulingException.class, appointment::complete);
    }
}
