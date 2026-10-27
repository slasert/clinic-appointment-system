package com.clinic;

import com.clinic.appointment.Appointment;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEF-04 regression tests. This class is deliberately in another package than
 * {@code AppointmentService}: code here receives Appointment objects from the service
 * and must not be able to change them past the business rules.
 *
 * <p>Before the fix, {@code appointment.addNote("x".repeat(500))} and {@code appointment.complete()}
 * compiled and succeeded from here (see docs/test-results/run-5). After the fix those calls
 * no longer compile, so the regression check is on the public API itself.</p>
 */
@DisplayName("DEF-04 Rules cannot be bypassed from outside the appointment package")
class EncapsulationTest extends ServiceTestBase {

    @Test
    @DisplayName("TC-E-01 Appointment exposes only read methods publicly")
    void appointmentHasNoPublicMutators() {
        List<String> publicMethods = Arrays.stream(Appointment.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .toList();

        assertTrue(publicMethods.stream().allMatch(n -> n.startsWith("get") || n.startsWith("is") || n.equals("toString")),
                "Unexpected public methods: " + publicMethods);
    }

    @Test
    @DisplayName("TC-E-02 Appointment has no public constructor")
    void appointmentCannotBeCreatedOutsideTheService() {
        assertEquals(0, Appointment.class.getConstructors().length);
    }

    @Test
    @DisplayName("TC-E-03 notes returned by an appointment cannot be modified from outside")
    void notesAreReadOnly() {
        Appointment appointment = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));

        assertThrows(UnsupportedOperationException.class, () -> appointment.getNotes().add("x".repeat(500)));
        assertTrue(appointment.getNotes().isEmpty());
    }
}
