package com.clinic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SmokeTest {

    @Test
    @DisplayName("TC-DEMO-01 console demo completes and reports conflicts and cancellation outcomes")
    void smokeTest() {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            Main.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        String text = captured.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("[BR-15]"), "Doctor conflict must be explained");
        assertTrue(text.contains("[BR-16]"), "Patient conflict must be explained");
        assertTrue(text.contains("late=true"), "Staff cancellation must record lateness");
        assertTrue(text.contains("old appointment is now RESCHEDULED"));
        assertTrue(text.contains("free slots:"), "Demo must reach its final schedule step");
    }
}
