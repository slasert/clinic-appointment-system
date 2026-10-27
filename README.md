# Clinic Appointment Management System

SVV (Software Validation and Verification) research assignment. Sıla SERT, 220911761.

A Java 21 console application that books, cancels and reschedules clinic appointments while
enforcing the clinic's business rules. Tested with JUnit 5; coverage measured with JaCoCo.

## Build and run

```bash
./mvnw test            # run all tests (regression run) + coverage report
./mvnw compile exec:java   # run the console demo
./mvnw surefire-report:report-only   # HTML test report
```

On Windows use `mvnw.cmd`. Maven does not need to be installed; the wrapper downloads it.

If Maven fails with `PKIX path building failed` (antivirus/proxy HTTPS inspection on Windows),
tell Java to use the Windows certificate store:

```powershell
$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

| Output | Location |
|---|---|
| Test results (HTML) | `target/reports/surefire.html` |
| Test results (XML/TXT) | `target/surefire-reports/` |
| Coverage report | `target/site/jacoco/index.html` |
| Saved console logs of test runs | `docs/test-results/` |

## Functional requirements

| ID | Requirement |
|---|---|
| FR-01 | Patient ID is 11 digits and does not start with 0; name is 2–50 characters (letters, spaces, `-`, `'`); age is 0–120. |
| FR-02 | Doctor has a non-empty ID, valid name, non-empty specialty and a daily appointment limit of 1–14. |
| FR-03 | An appointment lasts 30 minutes and starts on :00 or :30, on a weekday, between 09:00 and 16:30, not during the 12:00–13:00 lunch break, at least 1 hour from now and at most 30 days ahead. |
| FR-04 | A doctor cannot have two appointments at the same time; a patient cannot have two appointments at the same time; a doctor cannot exceed the daily limit; a patient can have at most 3 upcoming appointments. |
| FR-05 | Patient and doctor IDs are unique. |
| FR-06 | Cancellation ≥ 24 h before the start is allowed for anyone. Under 24 h only staff may cancel and it is recorded as a late cancellation. Cancelling after the start is not allowed. |
| FR-07 | Rescheduling follows the cancellation policy and is atomic: the old appointment becomes RESCHEDULED and a new one is created, or nothing changes. |
| FR-08 | Only SCHEDULED appointments can change status. COMPLETED is allowed from the start time, NO_SHOW from the end time. |
| FR-09 | The system lists a doctor's daily schedule, a patient's appointment history and a doctor's free slots for a day. |

## Project structure

```
src/main/java/com/clinic/
  model/      Patient, Doctor, Appointment, AppointmentStatus, Requester
  service/    AppointmentService (business logic), ClinicRules (rule constants)
  exception/  ValidationException (bad input), SchedulingException (rule violation)
  Main.java   console demo
src/test/java/com/clinic/
  model/      PatientTest, DoctorTest
  service/    SlotValidationTest, BookingTest, CancellationTest, RescheduleTest,
              StatusTransitionTest, ScheduleQueryTest
  support/    MutableClock (controllable time), ServiceTestBase (shared fixture)
```

Test methods are tagged with the technique they apply (`EP`, `BVA`, `DecisionTable`, `Negative`)
and named with a test case ID (e.g. `TC-C-01`) that matches the test case table in the report.
Run only one technique with e.g. `./mvnw test -Dgroups=BVA`.

## Defect log

| ID | Found by | Description | Fix | Status |
|---|---|---|---|---|
| DEF-01 | TC-B-16 (BVA on the 3-appointment limit, with time moved forward) | Past appointments that were never marked COMPLETED/NO_SHOW were still counted as "active", so a patient could be blocked from booking forever. | Only appointments whose end time is in the future count as active. | Fixed; full suite re-run (154/154 pass) |
