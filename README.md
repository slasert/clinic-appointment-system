# Clinic Appointment Management System

Individual assignment for **Software Validation and Verification** – Sıla SERT, 220911761.

## 1. Project overview

A Java 21 console application that books, cancels and reschedules appointments for a small clinic
and enforces 26 explicit business rules. The project's focus is systematic verification: 115 JUnit 5
test cases (216 test invocations) designed with equivalence partitioning, boundary value analysis,
decision tables and negative testing, run as a regression suite after every change, measured with
JaCoCo and checked with a manual mutation analysis.

## 2. Problem definition

In small clinics appointments are often managed by hand. This leads to double bookings, scheduling
conflicts, appointments at invalid times and inconsistent handling of cancellations: one staff member
cancels, another cancels and rebooks, a third only writes a note. As the number of patients grows, so
does the error rate.

## 3. Project objectives

1. Enforce one consistent set of scheduling rules for every booking, cancellation and reschedule.
2. Prevent double booking of doctors and patients.
3. Reject invalid times (outside hours, lunch, weekends, too soon, too far ahead).
4. Apply one clear cancellation policy, including staff overrides and late-cancellation records.
5. Make rescheduling a single atomic operation.
6. Demonstrate the correctness of all of the above with automated, traceable tests.

## 4. Functional requirements

| ID | Requirement |
|---|---|
| FR-01 | Patient ID is 11 digits and does not start with 0; name is 2–50 characters (letters, spaces, `-`, `'`); age is 0–120. |
| FR-02 | A doctor has a non-empty ID, a valid name, a non-empty specialty and a daily appointment limit of 1–14. |
| FR-03 | An appointment lasts 30 minutes and starts on :00 or :30, on a weekday, between 09:00 and 16:30, not during the 12:00–13:00 lunch break, at least 1 hour from now and at most 30 days ahead. |
| FR-04 | A doctor cannot have two appointments at the same time; a patient cannot have two appointments at the same time; a doctor cannot exceed the daily limit; a patient can have at most 3 active appointments (SCHEDULED with an end after now, including an ongoing visit). |
| FR-05 | Patient and doctor IDs are unique. |
| FR-06 | Cancellation ≥ 24 h before the start is allowed for anyone. Under 24 h only staff may cancel and it is recorded as a late cancellation. Cancelling at or after the start is not allowed. |
| FR-07 | Rescheduling follows the cancellation policy and is atomic: the old appointment becomes RESCHEDULED and a new one is created, or nothing changes. |
| FR-08 | Only SCHEDULED appointments can change status. COMPLETED is allowed from the start time, NO_SHOW from the end time. |
| FR-09 | The system lists a doctor's daily schedule, a patient's appointment history and a doctor's free slots for a day. |
| FR-10 | Staff can add notes to an appointment in any status: 1–200 characters after trimming, at most 5 per appointment. |

## 5. Business rules

The 26 rules are defined in `com.clinic.rules.BusinessRule` (BR-01 … BR-26). Every rejection carries
its rule, for example `[BR-12] No appointments during the 12:00-13:00 lunch break`. The full catalogue,
with valid and invalid inputs, expected behaviour and tests, is in
[docs/test-plan.md §2](docs/test-plan.md). The numbers live in one place: `com.clinic.rules.ClinicRules`.

## 6. Technologies used

| Technology | Version | Purpose |
|---|---|---|
| Java (Eclipse Temurin JDK) | 21 | Language and runtime; only the standard library is used by the application |
| Apache Maven + Maven Wrapper | 3.9.9 / 3.3.2 | Build, dependency management, test execution |
| JUnit 5 (Jupiter) | 5.14.4 | Unit testing (the only external library; test scope) |
| Maven Surefire (+ report plugin) | 3.6.1 | Runs tests, XML/HTML reports |
| JaCoCo | 0.8.16 | Code coverage |
| exec-maven-plugin | 3.5.0 | Runs the console demo |
| Git | 2.56 | Version control; the history documents each defect fix |
| Python 3 (optional) | – | Helper scripts in `scripts/` for evidence, report export and submission packaging |

No database, GUI, web framework or network access is used. Data is kept in memory (`LinkedHashMap`).

## 7. Project structure

```
pom.xml, mvnw, mvnw.cmd, .mvn/        build configuration and Maven Wrapper
src/main/java/com/clinic/
  Main.java                           console demonstration
  appointment/                        AppointmentService, Appointment, AppointmentStatus, TimeSlotPolicy
  model/                              Patient, Doctor, Requester, NameValidator
  exception/                          ClinicException, ValidationException, SchedulingException
  rules/                              ClinicRules (constants), BusinessRule (BR catalogue)
src/test/java/com/clinic/
  SmokeTest.java                     TC-DEMO-01 checks actual console outcomes
  appointment/                        BookingTest, CancellationTest, RescheduleTest, StatusTransitionTest,
                                      SlotValidationTest, TimeSlotPolicyTest, ScheduleQueryTest, NoteTest,
                                      AppointmentTest
  model/                              PatientTest, DoctorTest
  EncapsulationTest.java              DEF-04 regression test (deliberately in another package)
  support/                            MutableClock, ServiceTestBase, RuleAssertions
docs/
  audit.md                            project audit (Phase 1)
  test-plan.md                        strategy, rule catalogue, techniques, decision tables, traceability matrix
  defect-log.md                       defects DEF-01…DEF-04, mutation analysis, tooling issues
  report.md, report.pdf, report.html   complete eight-section report
  proposal.md                         scope and honest approval-record status
  submission-checklist.md             assignment-to-deliverable mapping
  report-material.md                  historical report support material (run 15)
  test-results/                       saved console output of every test run, mutation analysis, demo output
scripts/
  test_case_report.py                 builds docs/test-results/test-case-results.md from Surefire XML
  mutation_analysis.py                manual mutation analysis (13 mutants)
```

## 8. Setup

Requirements: JDK 21 on the `PATH`. Maven does **not** need to be installed – the wrapper downloads it.

If Maven fails with `PKIX path building failed` (security software inspecting HTTPS on Windows),
let Java use the Windows certificate store, in the same terminal:

```powershell
$env:MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

On Linux/macOS use `./mvnw` instead of `.\mvnw.cmd`.

## 9. Running the application

```powershell
.\mvnw.cmd -q compile exec:java
```

The demo uses a fixed clock (Monday 2 Nov 2026 08:00), so the output is always the same – see
`docs/test-results/demo-output.txt`. It shows registration, booking, overlap rejection, invalid
time slots, rescheduling, cancellations with their outcomes and the doctors' schedules.

## 10. Running the JUnit tests

```powershell
.\mvnw.cmd clean test                 # full regression suite + coverage report
.\mvnw.cmd test -Dgroups=BVA          # only one technique: EP, BVA, DecisionTable, Negative
.\mvnw.cmd test -Dtest=CancellationTest
```

In VS Code: *Testing* panel (flask icon) → run all. Always use `clean` before producing reports,
otherwise results of deleted tests can remain in `target/`.

## 11. Coverage report

`mvnw clean test` writes the JaCoCo report to `target/site/jacoco/index.html`
(CSV: `target/site/jacoco/jacoco.csv`). HTML test report:

```powershell
.\mvnw.cmd surefire-report:report-only      # → target/reports/surefire.html
python scripts\test_case_report.py          # → docs/test-results/test-case-results.md
```

## 12. Testing techniques

| Technique | How it was applied | Tests tagged |
|---|---|---|
| Equivalence partitioning | Valid/invalid classes for IDs, names, days, minutes, requesters, statuses | 16 |
| Boundary value analysis | Below/at/above every time and numeric limit, using exact clock instants | 35 |
| Decision tables | Booking (5 rules), cancellation (6), rescheduling (7), status transitions (5×4) | 18 |
| Negative testing | Every rejection asserts the exception type, the exact `BusinessRule` and unchanged state | 55 |
| Regression testing | Full suite after every change; each run saved in `docs/test-results/` | whole suite |

Details and justification: [docs/test-plan.md](docs/test-plan.md).

## 13. Test results (2 November 2026, `mvnw clean verify`)

| Measure | Result |
|---|---|
| Test cases / JUnit invocations | 115 / 216 |
| Passed / failed / skipped | 216 / 0 / 0 |
| Line coverage (whole project / core `appointment` package) | 100 % (317/317) / 100 % (219/219) |
| Branch coverage (whole project / core) | 98.9 % (176/178) / 100 % (140/140) |
| Method coverage (whole project / core) | 100 % (88/88) / 100 % (58/58) |
| Manual mutation analysis | round 1: 12/13 killed → 2 tests added → round 2: 13/13 killed |
| Defects found and fixed | 4 (DEF-01 … DEF-04), each with a failing test before the fix |

Per-test results: [docs/test-results/test-case-results.md](docs/test-results/test-case-results.md).
Defects: [docs/defect-log.md](docs/defect-log.md).
`Main` is excluded from coverage because it is a demo without business logic. The two uncovered
branches are `equals(...)` called with an object of another class in `Patient` and `Doctor`.

## 14. Known limitations

- Data is in memory only and lost on exit; single user; not thread-safe.
- Fixed 30-minute slots; one working schedule for all doctors; no public holidays.
- Times are local date-times without time zones or daylight-saving handling.
- The national ID is checked for format only (no checksum).
- A late *reschedule* by staff is allowed but not flagged as late (the requirements do not say; see `docs/audit.md` E1).
- Tests were designed by the same author as the code (with AI assistance); there is no independent acceptance testing.
- The mutation analysis is manual (13 mutants), not exhaustive.

## 15. Future improvements

- Automated mutation testing (e.g. PIT) to measure test effectiveness more broadly.
- Public-holiday calendar and per-doctor working hours / leave.
- Persistence (file or database) and concurrent access.
- National-ID checksum validation; clarify and implement the late-reschedule flag.

## 16. Submission materials

The complete eight-section report is available as [PDF](docs/report.pdf),
[HTML](docs/report.html) and [Markdown](docs/report.md). The scope proposal is in
[docs/proposal.md](docs/proposal.md). See [docs/submission-checklist.md](docs/submission-checklist.md)
for the mapping to the assignment and the final evidence summary. The ZIP in `submission/`
contains source, tests, build configuration, the runnable JAR, reports and saved evidence.

```powershell
.\mvnw.cmd clean verify
java -jar target/clinic-appointment-1.0-SNAPSHOT.jar
```

Full coverage and test reports are also preserved under `docs/test-results/final/`,
so they remain available after cleaning `target/`. The current final run supersedes the
historical figures in `docs/report-material.md`.

## 17. AI assistance disclosure

This project was developed with the help of **Claude (Anthropic)**, used inside Visual Studio Code.
Claude generated the initial implementation, the JUnit 5 tests, the Maven configuration and the
documentation drafts from the approved proposal; later it audited and refactored the project,
reproduced and fixed defects DEF-01…DEF-04 with failing tests first, carried out the mutation
analysis and collected coverage results by running the tools. Every result in this repository comes
from a real execution recorded in `docs/test-results/` and in the git history (commits are marked
`Co-Authored-By: Claude`). OpenAI Codex subsequently completed final build configuration, the console regression test,
the eight-section report, reference verification and submission packaging. The student must
review and understand the work and is responsible for explaining the implementation and
testing decisions and complying with the instructor?s AI-use guidelines. The report is
AI-assisted; it is not represented as independently written student prose.
