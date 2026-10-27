package com.clinic;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.appointment.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Requester;
import com.clinic.appointment.AppointmentService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Small console demonstration of the appointment system.
 * The clock is fixed so the demo always produces the same output.
 */
public class Main {

    public static void main(String[] args) {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 11, 2, 8, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AppointmentService service = new AppointmentService(clock);

        service.registerDoctor(new Doctor("D1", "Ayşe Yılmaz", "Cardiology", 12));
        service.registerPatient(new Patient("12345678901", "Mehmet Kaya", 45));
        service.registerPatient(new Patient("23456789012", "Zeynep Demir", 30));

        LocalDate tuesday = LocalDate.of(2026, 11, 3);
        Appointment first = service.book("12345678901", "D1", tuesday.atTime(10, 0));
        System.out.println("Booked:      " + first);

        tryAction("Double booking", () -> service.book("23456789012", "D1", tuesday.atTime(10, 0)));
        tryAction("Lunch break", () -> service.book("23456789012", "D1", tuesday.atTime(12, 30)));

        Appointment moved = service.reschedule(first.getId(), tuesday.atTime(14, 0), Requester.PATIENT);
        System.out.println("Rescheduled: " + moved);

        System.out.println("\nSchedule of D1 on " + tuesday + ":");
        service.getDoctorSchedule("D1", tuesday).forEach(a -> System.out.println("  " + a));
        System.out.println("Free slots: " + service.getAvailableSlots("D1", tuesday).size());
    }

    private static void tryAction(String label, Runnable action) {
        try {
            action.run();
            System.out.println(label + ": succeeded");
        } catch (ValidationException | SchedulingException e) {
            System.out.println(label + ": rejected -> " + e.getMessage());
        }
    }
}
