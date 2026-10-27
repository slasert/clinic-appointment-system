package com.clinic.appointment;

import com.clinic.exception.SchedulingException;
import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.rules.BusinessRule;
import com.clinic.rules.ClinicRules;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single 30-minute appointment.
 *
 * <p>Other packages can only read an appointment. Creating and changing it is package-private,
 * so every change has to go through {@link AppointmentService}, which enforces the business
 * rules (DEF-04).</p>
 */
public final class Appointment {

    private final String id;
    private final Patient patient;
    private final Doctor doctor;
    private final LocalDateTime start;
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;
    private boolean lateCancellation;
    private final List<String> notes = new ArrayList<>();

    Appointment(String id, Patient patient, Doctor doctor, LocalDateTime start) {
        this.id = Objects.requireNonNull(id, "id");
        this.patient = Objects.requireNonNull(patient, "patient");
        this.doctor = Objects.requireNonNull(doctor, "doctor");
        this.start = Objects.requireNonNull(start, "start");
    }

    void cancel(boolean late) {
        changeStatus(AppointmentStatus.CANCELLED);
        this.lateCancellation = late;
    }

    void markRescheduled() {
        changeStatus(AppointmentStatus.RESCHEDULED);
    }

    void complete() {
        changeStatus(AppointmentStatus.COMPLETED);
    }

    void markNoShow() {
        changeStatus(AppointmentStatus.NO_SHOW);
    }

    /** Notes are allowed in any status, e.g. to record why an appointment was cancelled. */
    void addNote(String note) {
        notes.add(note);
    }

    /** Last line of defence: final statuses can never change, whoever calls. */
    private void changeStatus(AppointmentStatus newStatus) {
        if (status != AppointmentStatus.SCHEDULED) {
            throw new SchedulingException(BusinessRule.STATUS_FINAL,
                    "Cannot change appointment " + id + " from " + status + " to " + newStatus);
        }
        status = newStatus;
    }

    public String getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return start.plusMinutes(ClinicRules.SLOT_MINUTES);
    }

    public LocalDate getDate() {
        return start.toLocalDate();
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public boolean isLateCancellation() {
        return lateCancellation;
    }

    public List<String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    @Override
    public String toString() {
        return id + " " + start + " " + patient.getName() + " -> " + doctor + " [" + status + "]";
    }
}
