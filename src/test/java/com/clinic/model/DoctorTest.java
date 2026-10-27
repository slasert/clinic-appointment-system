package com.clinic.model;

import com.clinic.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-02 Doctor registration data. Daily limit valid range: 1..14 (number of slots in a day).
 */
@DisplayName("FR-02 Doctor validation")
class DoctorTest {

    @Test
    @Tag("EP")
    @DisplayName("TC-D-01 valid doctor is created")
    void validDoctor() {
        Doctor doctor = new Doctor(" D1 ", "Ayşe Yılmaz", " Cardiology ", 12);

        assertEquals("D1", doctor.getId());
        assertEquals("Ayşe Yılmaz", doctor.getName());
        assertEquals("Cardiology", doctor.getSpecialty());
        assertEquals(12, doctor.getDailyLimit());
    }

    @ParameterizedTest(name = "TC-D-02 daily limit {0} is accepted")
    @Tag("BVA")
    @ValueSource(ints = {1, 2, 13, 14})
    void dailyLimitBoundariesAccepted(int limit) {
        assertEquals(limit, new Doctor("D1", "Ayşe Yılmaz", "Cardiology", limit).getDailyLimit());
    }

    @ParameterizedTest(name = "TC-D-03 daily limit {0} is rejected")
    @Tag("BVA")
    @Tag("Negative")
    @ValueSource(ints = {-1, 0, 15})
    void dailyLimitOutsideBoundariesRejected(int limit) {
        assertThrows(ValidationException.class, () -> new Doctor("D1", "Ayşe Yılmaz", "Cardiology", limit));
    }

    @ParameterizedTest(name = "TC-D-04 empty doctor ID \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void emptyIdRejected(String id) {
        assertThrows(ValidationException.class, () -> new Doctor(id, "Ayşe Yılmaz", "Cardiology", 12));
    }

    @ParameterizedTest(name = "TC-D-05 empty specialty \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void emptySpecialtyRejected(String specialty) {
        assertThrows(ValidationException.class, () -> new Doctor("D1", "Ayşe Yılmaz", specialty, 12));
    }

    @ParameterizedTest(name = "TC-D-06 invalid doctor name \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"A", "Dr. Ayşe", "Ayşe2"})
    void invalidNameRejected(String name) {
        assertThrows(ValidationException.class, () -> new Doctor("D1", name, "Cardiology", 12));
    }
}
