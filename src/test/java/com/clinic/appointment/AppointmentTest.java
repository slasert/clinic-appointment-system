package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.rules.BusinessRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests of the invariants that {@link Appointment} protects by itself.
 *
 * <p>Through the public API these checks are never reached, because {@link AppointmentService}
 * checks the status first. They are the last line of defence if the service check is ever removed
 * (exactly mutant M13), so they are tested directly here, in the same package.</p>
 */
@DisplayName("Appointment invariants")
class AppointmentTest {

    private static final Patient PATIENT = new Patient("12345678901", "Mehmet Kaya", 45);
    private static final Doctor DOCTOR = new Doctor("D1", "Ayşe Yılmaz", "Cardiology", 12);
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 3, 10, 0);

    private Appointment appointment;

    @BeforeEach
    void createAppointment() {
        appointment = new Appointment("A0001", PATIENT, DOCTOR, START);
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-A-01 once a status is final, no further status change is possible")
    void finalStatusCannotChange() {
        appointment.complete();

        assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL, () -> appointment.cancel(false));
        assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL, appointment::markNoShow);
        assertRejected(SchedulingException.class, BusinessRule.STATUS_FINAL, appointment::markRescheduled);
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-A-02 an appointment cannot be created with a missing id, patient, doctor or start")
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new Appointment(null, PATIENT, DOCTOR, START));
        assertThrows(NullPointerException.class, () -> new Appointment("A0001", null, DOCTOR, START));
        assertThrows(NullPointerException.class, () -> new Appointment("A0001", PATIENT, null, START));
        assertThrows(NullPointerException.class, () -> new Appointment("A0001", PATIENT, DOCTOR, null));
    }

    @Test
    @DisplayName("TC-A-03 end time and text form are derived from the start")
    void derivedValues() {
        assertEquals(START.plusMinutes(30), appointment.getEnd());
        assertEquals("A0001 2026-11-03T10:00 Mehmet Kaya -> Dr. Ayşe Yılmaz (Cardiology) [SCHEDULED]",
                appointment.toString());
    }
}
