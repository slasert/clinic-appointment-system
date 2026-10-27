package com.clinic.support;

import com.clinic.model.Doctor;
import com.clinic.model.Patient;
import com.clinic.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Common fixture: a fresh service for every test, "now" = Monday 2 Nov 2026 08:00,
 * two doctors and four patients.
 */
public abstract class ServiceTestBase {

    protected static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    protected static final LocalDate TUESDAY = LocalDate.of(2026, 11, 3);
    protected static final LocalDateTime NOW = MONDAY.atTime(8, 0);

    protected static final String DOCTOR_1 = "D1";
    protected static final String DOCTOR_2 = "D2";
    protected static final String LIMITED_DOCTOR = "D3";
    protected static final String PATIENT_1 = "12345678901";
    protected static final String PATIENT_2 = "23456789012";
    protected static final String PATIENT_3 = "34567890123";
    protected static final String PATIENT_4 = "45678901234";

    protected MutableClock clock;
    protected AppointmentService service;

    @BeforeEach
    void setUpService() {
        clock = new MutableClock(NOW);
        service = new AppointmentService(clock);
        service.registerDoctor(new Doctor(DOCTOR_1, "Ayşe Yılmaz", "Cardiology", 12));
        service.registerDoctor(new Doctor(DOCTOR_2, "Can Öztürk", "Dermatology", 12));
        service.registerDoctor(new Doctor(LIMITED_DOCTOR, "Elif Şahin", "Neurology", 2));
        service.registerPatient(new Patient(PATIENT_1, "Mehmet Kaya", 45));
        service.registerPatient(new Patient(PATIENT_2, "Zeynep Demir", 30));
        service.registerPatient(new Patient(PATIENT_3, "Ali Çelik", 60));
        service.registerPatient(new Patient(PATIENT_4, "Fatma Koç", 25));
    }

    protected static LocalDateTime tuesdayAt(int hour, int minute) {
        return TUESDAY.atTime(hour, minute);
    }
}
