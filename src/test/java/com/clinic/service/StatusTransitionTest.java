package com.clinic.service;

import com.clinic.exception.SchedulingException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Requester;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-08 Appointment lifecycle. Only SCHEDULED appointments can change status.
 * COMPLETED is allowed from the start time, NO_SHOW from the end time (start + 30 min).
 */
@DisplayName("FR-08 Status transitions")
class StatusTransitionTest extends ServiceTestBase {

    private Appointment appointment;

    @BeforeEach
    void bookAppointment() {
        appointment = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-ST-01 complete one minute before start -> rejected")
    void completeBeforeStartRejected() {
        clock.setTo(tuesdayAt(9, 59));

        assertThrows(SchedulingException.class, () -> service.complete(appointment.getId()));
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-ST-02 complete exactly at start -> COMPLETED")
    void completeAtStart() {
        clock.setTo(tuesdayAt(10, 0));

        service.complete(appointment.getId());

        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-ST-03 no-show one minute before the slot ends -> rejected")
    void noShowBeforeEndRejected() {
        clock.setTo(tuesdayAt(10, 29));

        assertThrows(SchedulingException.class, () -> service.markNoShow(appointment.getId()));
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-ST-04 no-show exactly when the slot ends -> NO_SHOW")
    void noShowAtEnd() {
        clock.setTo(tuesdayAt(10, 30));

        service.markNoShow(appointment.getId());

        assertEquals(AppointmentStatus.NO_SHOW, appointment.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-ST-05 a completed appointment cannot be cancelled, completed again or marked no-show")
    void completedIsFinal() {
        clock.setTo(tuesdayAt(10, 30));
        service.complete(appointment.getId());

        assertThrows(SchedulingException.class, () -> service.cancel(appointment.getId(), Requester.STAFF));
        assertThrows(SchedulingException.class, () -> service.complete(appointment.getId()));
        assertThrows(SchedulingException.class, () -> service.markNoShow(appointment.getId()));
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-ST-06 a cancelled appointment cannot be completed or marked no-show")
    void cancelledIsFinal() {
        service.cancel(appointment.getId(), Requester.PATIENT);
        clock.setTo(tuesdayAt(11, 0));

        assertThrows(SchedulingException.class, () -> service.complete(appointment.getId()));
        assertThrows(SchedulingException.class, () -> service.markNoShow(appointment.getId()));
    }

    @Test
    @DisplayName("TC-ST-07 completed and no-show appointments still count towards the doctor's daily limit")
    void finishedAppointmentsOccupySlots() {
        clock.setTo(MONDAY.atTime(7, 0));
        Appointment first = service.book(PATIENT_1, LIMITED_DOCTOR, MONDAY.atTime(9, 0));
        Appointment second = service.book(PATIENT_2, LIMITED_DOCTOR, MONDAY.atTime(9, 30));

        clock.setTo(MONDAY.atTime(10, 0));
        service.complete(first.getId());
        service.markNoShow(second.getId());

        assertThrows(SchedulingException.class, () -> service.book(PATIENT_3, LIMITED_DOCTOR, MONDAY.atTime(15, 0)));
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-ST-08 status change on the model itself is guarded too")
    void modelGuardsTransitions() {
        appointment.cancel(false);

        assertThrows(SchedulingException.class, appointment::complete);
        assertThrows(SchedulingException.class, appointment::markNoShow);
        assertThrows(SchedulingException.class, appointment::markRescheduled);
    }
}
