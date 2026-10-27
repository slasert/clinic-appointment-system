package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Requester;
import com.clinic.rules.BusinessRule;
import com.clinic.support.ServiceTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-10 Appointment notes: 1..200 characters after trimming, at most 5 per appointment,
 * allowed in any status.
 */
@DisplayName("FR-10 Appointment notes")
class NoteTest extends ServiceTestBase {

    private Appointment appointment;

    @BeforeEach
    void bookAppointment() {
        appointment = service.book(PATIENT_1, DOCTOR_1, tuesdayAt(10, 0));
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-N-01 a valid note is stored trimmed")
    void validNote() {
        service.addNote(appointment.getId(), "  Patient asked for a morning slot  ");

        assertEquals(List.of("Patient asked for a morning slot"), appointment.getNotes());
    }

    @ParameterizedTest(name = "TC-N-02 note of length {0} is accepted")
    @Tag("BVA")
    @ValueSource(ints = {1, 2, 199, 200})
    void noteLengthBoundariesAccepted(int length) {
        service.addNote(appointment.getId(), "x".repeat(length));

        assertEquals(length, appointment.getNotes().get(0).length());
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-N-03 note of 201 characters is rejected")
    void noteTooLongRejected() {
        assertRejected(ValidationException.class, BusinessRule.NOTE_LENGTH,
                () -> service.addNote(appointment.getId(), "x".repeat(201)));
        assertTrue(appointment.getNotes().isEmpty());
    }

    @ParameterizedTest(name = "TC-N-04 empty note \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void emptyNoteRejected(String note) {
        assertRejected(ValidationException.class, BusinessRule.NOTE_LENGTH,
                () -> service.addNote(appointment.getId(), note));
        assertTrue(appointment.getNotes().isEmpty());
    }

    @Test
    @Tag("BVA")
    @Tag("Negative")
    @DisplayName("TC-N-05 5th note is accepted, 6th is rejected")
    void maxNotesPerAppointment() {
        for (int i = 1; i <= 5; i++) {
            service.addNote(appointment.getId(), "Note " + i);
        }

        assertRejected(SchedulingException.class, BusinessRule.NOTE_LIMIT,
                () -> service.addNote(appointment.getId(), "Note 6"));
        assertEquals(5, appointment.getNotes().size());
    }

    @Test
    @DisplayName("TC-N-06 a note can be added to a cancelled appointment to record the reason")
    void noteOnCancelledAppointment() {
        service.cancel(appointment.getId(), Requester.PATIENT);

        service.addNote(appointment.getId(), "Cancelled by phone, patient is travelling");

        assertEquals(1, appointment.getNotes().size());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-N-07 the notes list cannot be modified from outside")
    void notesAreReadOnly() {
        assertThrows(UnsupportedOperationException.class, () -> appointment.getNotes().add("x".repeat(500)));
        assertTrue(appointment.getNotes().isEmpty());
    }

    @Test
    @Tag("Negative")
    @DisplayName("TC-N-08 note for an unknown or null appointment is rejected")
    void unknownAppointmentRejected() {
        assertRejected(ValidationException.class, BusinessRule.KNOWN_REFERENCE, () -> service.addNote("A9999", "text"));
        assertRejected(ValidationException.class, BusinessRule.KNOWN_REFERENCE, () -> service.addNote(null, "text"));
    }
}
