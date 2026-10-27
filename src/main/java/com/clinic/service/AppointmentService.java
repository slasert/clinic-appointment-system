package com.clinic.service;

import com.clinic.exception.SchedulingException;
import com.clinic.exception.ValidationException;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.model.Requester;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Central service that books, cancels and reschedules appointments while
 * enforcing the clinic rules defined in {@link ClinicRules}.
 *
 * <p>The current time is taken from an injected {@link Clock} so that every
 * time-dependent rule can be tested deterministically.</p>
 */
public class AppointmentService {

    private final Clock clock;
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private final Map<String, Doctor> doctors = new LinkedHashMap<>();
    private final Map<String, Appointment> appointments = new LinkedHashMap<>();
    private int nextAppointmentNumber = 1;

    public AppointmentService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    // ---------------------------------------------------------------- registration

    public void registerPatient(Patient patient) {
        if (patient == null) {
            throw new ValidationException("Patient cannot be null");
        }
        if (patients.containsKey(patient.getId())) {
            throw new ValidationException("Patient " + patient.getId() + " is already registered");
        }
        patients.put(patient.getId(), patient);
    }

    public void registerDoctor(Doctor doctor) {
        if (doctor == null) {
            throw new ValidationException("Doctor cannot be null");
        }
        if (doctors.containsKey(doctor.getId())) {
            throw new ValidationException("Doctor " + doctor.getId() + " is already registered");
        }
        doctors.put(doctor.getId(), doctor);
    }

    // ---------------------------------------------------------------- booking

    /**
     * Books a 30-minute appointment.
     *
     * @throws ValidationException  unknown patient/doctor or malformed start time
     * @throws SchedulingException  a clinic rule prevents the booking
     */
    public Appointment book(String patientId, String doctorId, LocalDateTime start) {
        Patient patient = findPatient(patientId);
        Doctor doctor = findDoctor(doctorId);
        validateSlot(start);
        checkAvailability(patient, doctor, start, null);

        Appointment appointment = new Appointment(nextId(), patient, doctor, start);
        appointments.put(appointment.getId(), appointment);
        return appointment;
    }

    /**
     * Cancels an appointment according to the cancellation policy:
     * <ul>
     *   <li>at least 24 hours before the start: anyone may cancel</li>
     *   <li>less than 24 hours before: only staff, recorded as a late cancellation</li>
     *   <li>after the start time: not allowed</li>
     * </ul>
     */
    public void cancel(String appointmentId, Requester requester) {
        Appointment appointment = findAppointment(appointmentId);
        boolean late = checkChangePolicy(appointment, requester);
        appointment.cancel(late);
    }

    /**
     * Moves an appointment to a new slot as one atomic operation: either the old
     * appointment becomes RESCHEDULED and a new one is created, or nothing changes.
     */
    public Appointment reschedule(String appointmentId, LocalDateTime newStart, Requester requester) {
        Appointment original = findAppointment(appointmentId);
        checkChangePolicy(original, requester);
        validateSlot(newStart);
        if (newStart.equals(original.getStart())) {
            throw new SchedulingException("New time is the same as the current appointment time");
        }
        checkAvailability(original.getPatient(), original.getDoctor(), newStart, original);

        Appointment moved = new Appointment(nextId(), original.getPatient(), original.getDoctor(), newStart);
        original.markRescheduled();
        appointments.put(moved.getId(), moved);
        return moved;
    }

    /** Marks a scheduled appointment as completed. Allowed once the appointment has started. */
    public void complete(String appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        requireScheduled(appointment);
        if (now().isBefore(appointment.getStart())) {
            throw new SchedulingException("Appointment cannot be completed before it starts");
        }
        appointment.complete();
    }

    /** Marks a scheduled appointment as no-show. Allowed once the appointment slot has ended. */
    public void markNoShow(String appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        requireScheduled(appointment);
        if (now().isBefore(appointment.getEnd())) {
            throw new SchedulingException("Appointment cannot be marked as no-show before its slot ends");
        }
        appointment.markNoShow();
    }

    // ---------------------------------------------------------------- queries

    public Appointment findAppointment(String appointmentId) {
        Appointment appointment = appointmentId == null ? null : appointments.get(appointmentId);
        if (appointment == null) {
            throw new ValidationException("Unknown appointment: " + appointmentId);
        }
        return appointment;
    }

    /** Scheduled appointments of a doctor on the given date, ordered by time. */
    public List<Appointment> getDoctorSchedule(String doctorId, LocalDate date) {
        Doctor doctor = findDoctor(doctorId);
        if (date == null) {
            throw new ValidationException("Date cannot be null");
        }
        return appointments.values().stream()
                .filter(a -> a.getDoctor().equals(doctor))
                .filter(a -> a.getDate().equals(date))
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .sorted(Comparator.comparing(Appointment::getStart))
                .toList();
    }

    /** All appointments of a patient (any status), ordered by time. */
    public List<Appointment> getPatientAppointments(String patientId) {
        Patient patient = findPatient(patientId);
        return appointments.values().stream()
                .filter(a -> a.getPatient().equals(patient))
                .sorted(Comparator.comparing(Appointment::getStart))
                .toList();
    }

    /** Start times on the given date that a doctor could still accept a booking for. */
    public List<LocalDateTime> getAvailableSlots(String doctorId, LocalDate date) {
        Doctor doctor = findDoctor(doctorId);
        if (date == null) {
            throw new ValidationException("Date cannot be null");
        }
        List<LocalDateTime> free = new ArrayList<>();
        if (countDoctorAppointments(doctor, date, null) >= doctor.getDailyLimit()) {
            return free;
        }
        for (LocalTime time = ClinicRules.OPENING_TIME;
             !time.isAfter(ClinicRules.LAST_SLOT_START);
             time = time.plusMinutes(ClinicRules.SLOT_MINUTES)) {
            LocalDateTime start = date.atTime(time);
            if (isBookableTime(start) && !isDoctorBusy(doctor, start, null)) {
                free.add(start);
            }
        }
        return free;
    }

    // ---------------------------------------------------------------- rules

    /** Throws if the start time breaks any time-related rule. */
    void validateSlot(LocalDateTime start) {
        if (start == null) {
            throw new ValidationException("Start time cannot be null");
        }
        if (start.getSecond() != 0 || start.getNano() != 0
                || start.getMinute() % ClinicRules.SLOT_MINUTES != 0) {
            throw new ValidationException("Appointments must start on the hour or half hour");
        }
        DayOfWeek day = start.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new SchedulingException("The clinic is closed on weekends");
        }
        LocalTime time = start.toLocalTime();
        if (time.isBefore(ClinicRules.OPENING_TIME) || time.isAfter(ClinicRules.LAST_SLOT_START)) {
            throw new SchedulingException("Appointments must be between "
                    + ClinicRules.OPENING_TIME + " and " + ClinicRules.CLOSING_TIME);
        }
        if (!time.isBefore(ClinicRules.LUNCH_START) && time.isBefore(ClinicRules.LUNCH_END)) {
            throw new SchedulingException("No appointments during the lunch break");
        }
        LocalDateTime now = now();
        if (start.isBefore(now.plusHours(ClinicRules.MIN_LEAD_HOURS))) {
            throw new SchedulingException("Appointments must be booked at least "
                    + ClinicRules.MIN_LEAD_HOURS + " hour in advance");
        }
        if (start.toLocalDate().isAfter(now.toLocalDate().plusDays(ClinicRules.MAX_ADVANCE_DAYS))) {
            throw new SchedulingException("Appointments cannot be booked more than "
                    + ClinicRules.MAX_ADVANCE_DAYS + " days ahead");
        }
    }

    private boolean isBookableTime(LocalDateTime start) {
        try {
            validateSlot(start);
            return true;
        } catch (ValidationException | SchedulingException e) {
            return false;
        }
    }

    /**
     * Checks conflicts and limits. {@code ignored} is the appointment being
     * rescheduled, which must not count against itself.
     */
    private void checkAvailability(Patient patient, Doctor doctor, LocalDateTime start, Appointment ignored) {
        if (isDoctorBusy(doctor, start, ignored)) {
            throw new SchedulingException(doctor + " already has an appointment at " + start);
        }
        boolean patientBusy = appointments.values().stream()
                .filter(a -> a != ignored)
                .anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                        && a.getPatient().equals(patient)
                        && a.getStart().equals(start));
        if (patientBusy) {
            throw new SchedulingException(patient + " already has an appointment at " + start);
        }
        if (countDoctorAppointments(doctor, start.toLocalDate(), ignored) >= doctor.getDailyLimit()) {
            throw new SchedulingException(doctor + " has reached the daily limit of "
                    + doctor.getDailyLimit() + " appointments");
        }
        // Only upcoming appointments are active; past ones left in SCHEDULED must not block the patient (DEF-01).
        LocalDateTime now = now();
        long active = appointments.values().stream()
                .filter(a -> a != ignored)
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED && a.getPatient().equals(patient))
                .filter(a -> a.getEnd().isAfter(now))
                .count();
        if (active >= ClinicRules.MAX_ACTIVE_APPOINTMENTS_PER_PATIENT) {
            throw new SchedulingException(patient + " already has "
                    + ClinicRules.MAX_ACTIVE_APPOINTMENTS_PER_PATIENT + " active appointments");
        }
    }

    private boolean isDoctorBusy(Doctor doctor, LocalDateTime start, Appointment ignored) {
        return appointments.values().stream()
                .filter(a -> a != ignored)
                .anyMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                        && a.getDoctor().equals(doctor)
                        && a.getStart().equals(start));
    }

    private long countDoctorAppointments(Doctor doctor, LocalDate date, Appointment ignored) {
        return appointments.values().stream()
                .filter(a -> a != ignored)
                .filter(a -> a.getStatus().occupiesSlot())
                .filter(a -> a.getDoctor().equals(doctor) && a.getDate().equals(date))
                .count();
    }

    /**
     * Applies the cancellation/reschedule policy decision table.
     *
     * @return true if the change is a late change (less than 24 hours before the start)
     */
    private boolean checkChangePolicy(Appointment appointment, Requester requester) {
        if (requester == null) {
            throw new ValidationException("Requester cannot be null");
        }
        requireScheduled(appointment);
        LocalDateTime now = now();
        if (!now.isBefore(appointment.getStart())) {
            throw new SchedulingException("Appointment has already started");
        }
        boolean late = now.plusHours(ClinicRules.FREE_CANCELLATION_HOURS).isAfter(appointment.getStart());
        if (late && requester == Requester.PATIENT) {
            throw new SchedulingException("Patients cannot change an appointment less than "
                    + ClinicRules.FREE_CANCELLATION_HOURS + " hours before it starts; please contact the clinic");
        }
        return late;
    }

    private void requireScheduled(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new SchedulingException("Appointment " + appointment.getId()
                    + " is " + appointment.getStatus() + " and cannot be changed");
        }
    }

    private Patient findPatient(String patientId) {
        Patient patient = patientId == null ? null : patients.get(patientId);
        if (patient == null) {
            throw new ValidationException("Unknown patient: " + patientId);
        }
        return patient;
    }

    private Doctor findDoctor(String doctorId) {
        Doctor doctor = doctorId == null ? null : doctors.get(doctorId);
        if (doctor == null) {
            throw new ValidationException("Unknown doctor: " + doctorId);
        }
        return doctor;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String nextId() {
        return String.format("A%04d", nextAppointmentNumber++);
    }
}
