package com.clinic;

import com.clinic.appointment.Appointment;
import com.clinic.appointment.AppointmentService;
import com.clinic.exception.ClinicException;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Requester;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;

/**
 * Console demonstration of the appointment system.
 *
 * <p>The clock is fixed at Monday 2 November 2026 08:00, so the demo produces the same
 * output every time it runs. It does not share any state with the automated tests.</p>
 */
public class Main {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);

    public static void main(String[] args) {
        Clock clock = Clock.fixed(MONDAY.atTime(8, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AppointmentService service = new AppointmentService(clock);
        System.out.println("Clinic Appointment Management System - demo");
        System.out.println("Current time: " + LocalDateTime.now(clock));

        section("1. Registering patients and doctors");
        service.registerDoctor(new Doctor("D1", "Ayse Yilmaz", "Cardiology", 12));
        service.registerDoctor(new Doctor("D2", "Can Ozturk", "Dermatology", 12));
        service.registerPatient(new Patient("12345678901", "Mehmet Kaya", 45));
        service.registerPatient(new Patient("23456789012", "Zeynep Demir", 30));
        System.out.println("[OK]       2 doctors and 2 patients registered");
        attempt("Register patient with a 10-digit ID",
                () -> new Patient("1234567890", "Ali Celik", 60));

        section("2. Booking an appointment");
        Appointment first = attempt("Mehmet Kaya with D1 on Tuesday 10:00",
                () -> service.book("12345678901", "D1", TUESDAY.atTime(10, 0)));

        section("3. Rejecting an overlapping appointment");
        attempt("Zeynep Demir with D1 on Tuesday 10:00 (doctor busy)",
                () -> service.book("23456789012", "D1", TUESDAY.atTime(10, 0)));
        attempt("Mehmet Kaya with D2 on Tuesday 10:00 (patient busy)",
                () -> service.book("12345678901", "D2", TUESDAY.atTime(10, 0)));
        attempt("Zeynep Demir with D1 on Tuesday 10:30 (back-to-back is fine)",
                () -> service.book("23456789012", "D1", TUESDAY.atTime(10, 30)));

        section("4. Rejecting invalid time slots");
        attempt("Tuesday 12:30 (lunch break)", () -> service.book("23456789012", "D2", TUESDAY.atTime(12, 30)));
        attempt("Saturday 10:00 (weekend)", () -> service.book("23456789012", "D2", MONDAY.plusDays(5).atTime(10, 0)));
        attempt("Tuesday 10:15 (not on a slot boundary)", () -> service.book("23456789012", "D2", TUESDAY.atTime(10, 15)));
        attempt("Monday 08:30 (before opening)", () -> service.book("23456789012", "D2", MONDAY.atTime(8, 30)));
        attempt("Last Friday 10:00 (in the past)", () -> service.book("23456789012", "D2", MONDAY.minusDays(3).atTime(10, 0)));

        section("5. Rescheduling");
        Appointment moved = attempt("Move " + first.getId() + " to Tuesday 14:00 (patient, >24h before)",
                () -> service.reschedule(first.getId(), TUESDAY.atTime(14, 0), Requester.PATIENT));
        System.out.println("           old appointment is now " + first.getStatus());

        section("6. Cancelling");
        Appointment today = service.book("23456789012", "D2", MONDAY.atTime(15, 0));
        System.out.println("[OK]       " + today + " booked for this afternoon");
        attempt("Patient cancels " + today.getId() + " (7 hours before)",
                () -> { service.cancel(today.getId(), Requester.PATIENT); return today; });
        attempt("Staff cancels " + today.getId() + " (7 hours before)",
                () -> { service.cancel(today.getId(), Requester.STAFF); return today; });
        attempt("Patient cancels " + moved.getId() + " (more than 24 hours before)",
                () -> { service.cancel(moved.getId(), Requester.PATIENT); return moved; });
        service.addNote(today.getId(), "Patient called: family emergency");

        section("7. Cancellation outcomes");
        for (Appointment a : new Appointment[] {today, moved}) {
            System.out.printf("  %s  status=%-9s late=%-5s notes=%s%n",
                    a.getId(), a.getStatus(), a.isLateCancellation(), a.getNotes());
        }

        section("8. Doctor schedule for Tuesday");
        for (String doctor : new String[] {"D1", "D2"}) {
            System.out.println("  " + doctor + ":");
            service.getDoctorSchedule(doctor, TUESDAY).forEach(a -> System.out.println("    " + a));
            System.out.println("    free slots: " + service.getAvailableSlots(doctor, TUESDAY).size() + " of 14");
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    /** Runs one demo step and prints whether it was accepted or which rule rejected it. */
    private static <T> T attempt(String description, Supplier<T> action) {
        try {
            T result = action.get();
            System.out.println("[OK]       " + description);
            return result;
        } catch (ClinicException e) {
            System.out.println("[REJECTED] " + description);
            System.out.println("           " + e.getMessage());
            return null;
        }
    }
}
