package com.clinic.model;

import com.clinic.exception.SchedulingException;
import com.clinic.service.ClinicRules;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single 30-minute appointment. Status changes are only allowed from SCHEDULED.
 */
public final class Appointment {

    private final String id;
    private final Patient patient;
    private final Doctor doctor;
    private final LocalDateTime start;
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;
    private boolean lateCancellation;

    public Appointment(String id, Patient patient, Doctor doctor, LocalDateTime start) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.start = start;
    }

    public void cancel(boolean late) {
        changeStatus(AppointmentStatus.CANCELLED);
        this.lateCancellation = late;
    }

    public void markRescheduled() {
        changeStatus(AppointmentStatus.RESCHEDULED);
    }

    public void complete() {
        changeStatus(AppointmentStatus.COMPLETED);
    }

    public void markNoShow() {
        changeStatus(AppointmentStatus.NO_SHOW);
    }

    private void changeStatus(AppointmentStatus newStatus) {
        if (status != AppointmentStatus.SCHEDULED) {
            throw new SchedulingException("Cannot change appointment " + id + " from " + status + " to " + newStatus);
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

    @Override
    public String toString() {
        return id + " " + start + " " + patient.getName() + " -> " + doctor + " [" + status + "]";
    }
}
