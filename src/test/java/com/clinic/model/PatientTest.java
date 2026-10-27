package com.clinic.model;

import com.clinic.exception.ValidationException;
import com.clinic.rules.BusinessRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.Normalizer;

import static com.clinic.support.RuleAssertions.assertRejected;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-01 Patient registration data.
 *
 * <pre>
 * Input  Valid classes                         Invalid classes
 * ID     11 digits, first digit 1-9            10 / 12 digits, leading 0, letter, space, sign, null, empty
 * Name   2-50 chars, letters/space/-/'          null, blank, 1 char, 51 chars, digit, symbol, starts with - or '
 * Age    0..120                                &lt; 0, &gt; 120
 * </pre>
 */
@DisplayName("FR-01 Patient validation")
class PatientTest {

    private static final String VALID_ID = "12345678901";
    private static final String VALID_NAME = "Mehmet Kaya";

    @Test
    @Tag("EP")
    @DisplayName("TC-P-01 valid patient is created and the name is trimmed")
    void validPatient() {
        Patient patient = new Patient(VALID_ID, "  Mehmet Kaya  ", 45);

        assertAll(
                () -> assertEquals(VALID_ID, patient.getId()),
                () -> assertEquals("Mehmet Kaya", patient.getName()),
                () -> assertEquals(45, patient.getAge()));
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-P-02 Turkish characters, hyphen and apostrophe are accepted in names")
    void turkishCharactersAccepted() {
        Patient patient = new Patient(VALID_ID, "Şükrü Çağlar-Öz O'Neil", 30);

        assertEquals("Şükrü Çağlar-Öz O'Neil", patient.getName());
    }

    @ParameterizedTest(name = "TC-P-03 invalid ID \"{0}\" is rejected")
    @Tag("EP")
    @Tag("BVA")
    @Tag("Negative")
    @ValueSource(strings = {
            "1234567890",    // 10 digits (BVA: length - 1)
            "123456789012",  // 12 digits (BVA: length + 1)
            "01234567890",   // leading zero
            "1234567890A",   // contains a letter
            "12345 78901",   // contains a space
            "-1234567890"    // sign character
    })
    void invalidIdRejected(String id) {
        assertRejected(ValidationException.class, BusinessRule.PATIENT_ID_FORMAT, () -> new Patient(id, VALID_NAME, 30));
    }

    @ParameterizedTest(name = "TC-P-04 null/empty ID \"{0}\" is rejected")
    @Tag("Negative")
    @NullAndEmptySource
    void nullOrEmptyIdRejected(String id) {
        assertRejected(ValidationException.class, BusinessRule.PATIENT_ID_FORMAT, () -> new Patient(id, VALID_NAME, 30));
    }

    @ParameterizedTest(name = "TC-P-05 age {0} is accepted")
    @Tag("BVA")
    @ValueSource(ints = {0, 1, 119, 120})
    void ageBoundariesAccepted(int age) {
        assertEquals(age, new Patient(VALID_ID, VALID_NAME, age).getAge());
    }

    @ParameterizedTest(name = "TC-P-06 age {0} is rejected")
    @Tag("BVA")
    @Tag("Negative")
    @ValueSource(ints = {-1, 121, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void ageOutsideBoundariesRejected(int age) {
        assertRejected(ValidationException.class, BusinessRule.PATIENT_AGE_RANGE, () -> new Patient(VALID_ID, VALID_NAME, age));
    }

    @ParameterizedTest(name = "TC-P-07 name of length {0} is accepted")
    @Tag("BVA")
    @ValueSource(ints = {2, 3, 49, 50})
    void nameLengthBoundariesAccepted(int length) {
        String name = "a".repeat(length);

        assertEquals(name, new Patient(VALID_ID, name, 30).getName());
    }

    @ParameterizedTest(name = "TC-P-08 name of length {0} is rejected")
    @Tag("BVA")
    @Tag("Negative")
    @ValueSource(ints = {1, 51})
    void nameLengthOutsideBoundariesRejected(int length) {
        String name = "a".repeat(length);

        assertRejected(ValidationException.class, BusinessRule.NAME_FORMAT, () -> new Patient(VALID_ID, name, 30));
    }

    @Test
    @Tag("BVA")
    @DisplayName("TC-P-09 surrounding spaces do not count towards the 50 character limit")
    void lengthIsMeasuredAfterTrimming() {
        String name = "  " + "a".repeat(50) + "  ";

        assertEquals(50, new Patient(VALID_ID, name, 30).getName().length());
    }

    @ParameterizedTest(name = "TC-P-10 invalid name \"{0}\" is rejected")
    @Tag("EP")
    @Tag("Negative")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Mehmet3", "Ali@Veli", "-Ali", "'Ali"})
    void invalidNameRejected(String name) {
        assertRejected(ValidationException.class, BusinessRule.NAME_FORMAT, () -> new Patient(VALID_ID, name, 30));
    }

    @Test
    @DisplayName("TC-P-11 patients with the same ID are equal")
    void equalityIsBasedOnId() {
        Patient a = new Patient(VALID_ID, "Mehmet Kaya", 45);
        Patient b = new Patient(VALID_ID, "Another Name", 20);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @Tag("EP")
    @DisplayName("TC-P-12 [DEF-03] a name in decomposed Unicode (NFD) is accepted and stored composed (NFC)")
    void decomposedNameAccepted() {
        String composed = "Şükrü Çelik";
        String decomposed = Normalizer.normalize(composed, Normalizer.Form.NFD);

        Patient patient = new Patient(VALID_ID, decomposed, 30);

        assertEquals(composed, patient.getName());
    }

    @Test
    @DisplayName("TC-P-13 the national ID is masked in text output")
    void idIsMaskedInToString() {
        Patient patient = new Patient(VALID_ID, VALID_NAME, 45);

        assertEquals("Mehmet Kaya (123******01)", patient.toString());
    }
}
