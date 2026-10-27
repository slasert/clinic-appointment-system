# Report Support Material

Technical material for the eight report sections required by the assignment. It is **source
material to review and rewrite in your own words**, not the report itself. Every number here was
measured on 2026-10-28 (run 15, `mvnw clean test`, and the JaCoCo report of that run).

---

## 1. Introduction and problem definition

- **Problem (from the approved proposal):** small clinics schedule appointments manually; this
  causes double bookings, conflicts, wrong time slots, inconsistent cancellation handling and
  inconsistent decisions between staff (one cancels, another cancels and rebooks, another only adds a note).
- **Solution:** a Java console application that enforces one consistent set of scheduling rules:
  bookings are checked against 26 explicit business rules; rescheduling is a single atomic operation;
  cancellations follow one decision table; notes record exceptions.
- **Scope limits (assignment constraints):** no GUI, no database, single user, in-memory data.

## 2. Requirements specification

Use the FR table in `README.md` and the BR catalogue in `docs/test-plan.md` §2.
Points worth stating explicitly:

- Requirements were made **testable** by giving every rule a number (30 min, 09:00–16:30, 1 h, 30 days, 24 h, 3, 5, 200).
- Ambiguities found and **not** resolved by inventing rules (see `docs/audit.md` §E): late staff
  reschedule flag, public holidays, cancellation fees, national-ID checksum.

## 3. Implementation

### Package structure (dependencies point downwards only, no cycles)

```
com.clinic                 Main (console demo)
com.clinic.appointment     AppointmentService, Appointment, AppointmentStatus, TimeSlotPolicy
com.clinic.model           Patient, Doctor, Requester, NameValidator
com.clinic.exception       ClinicException ← ValidationException, SchedulingException
com.clinic.rules           ClinicRules (constants), BusinessRule (BR-01…BR-26)
```

### Main classes and methods

| Class | Responsibility | Key methods |
|---|---|---|
| `AppointmentService` | Coordinates registration, booking, cancellation, rescheduling, status changes, notes and queries; checks conflicts and limits across appointments | `book`, `cancel`, `reschedule`, `complete`, `markNoShow`, `addNote`, `getAvailableSlots`, `getDoctorSchedule`, `getPatientAppointments` |
| `TimeSlotPolicy` | Pure time rules for one start time (BR-08…BR-14) | `findViolation(start, now)`, `validate`, `dailySlots(date)` |
| `Appointment` | One 30-min appointment; guards its status (only SCHEDULED can change) | read methods public; `cancel`, `complete`, … package-private |
| `Patient`, `Doctor` | Immutable, always-valid value objects | constructors validate; `equals` by ID |
| `NameValidator` | Shared name rule incl. Unicode normalisation | `validate` |
| `BusinessRule` | Rule catalogue with IDs and descriptions | `getId`, `getDescription` |
| `ClinicException` | Base of all rejections; carries the violated rule | `getRule` |

### Design decisions worth explaining

1. **Injected `java.time.Clock`** – the service never calls `LocalDateTime.now()` directly; tests move a
   `MutableClock` to exact boundary instants (e.g. exactly 24 h before the start).
2. **Rule IDs on exceptions** – tests assert *why* something was rejected; the console shows `[BR-12] …`.
3. **Encapsulation by package** – only `AppointmentService` (same package) can change an `Appointment`.
   This fixed DEF-04.
4. **Atomic reschedule** – all checks run before the first state change; the original appointment is
   excluded from its own conflict/limit checks.
5. **Single source of truth for numbers** – `ClinicRules`; `SLOTS_PER_DAY` is derived, not typed.
6. **Data minimisation** – the national ID is masked in `toString()` and therefore in all messages.

## 4. Test plan and test design

Use `docs/test-plan.md` (strategy, technique explanations with justification, decision tables,
BVA table, traceability matrix). Key figures:

| Technique | Test cases tagged |
|---|---|
| Equivalence partitioning | 16 |
| Boundary value analysis | 35 |
| Decision table | 18 (4 tables: booking, cancellation, reschedule, 5×4 status transitions) |
| Negative | 55 |
| Untagged functional | 27 |

## 5. Test implementation and results

- **Final result (run 15, `mvnw clean test`):** 114 test cases, **215 JUnit invocations, 215 passed,
  0 failed, 0 skipped**. Per-test results: `docs/test-results/test-case-results.md`.
- JUnit 5 features used: `@Test`, `@ParameterizedTest` with `@CsvSource`, `@ValueSource`,
  `@EnumSource`, `@NullAndEmptySource`; `@Nested`; `@DisplayName`; `@Tag`; `@BeforeEach`;
  `assertAll`, `assertThrows`, `assertEquals`; implicit argument conversion of `LocalTime`,
  `LocalDate`, `LocalDateTime`, `Duration` and enums from CSV strings.
- **Example of a successful run** – `docs/test-results/run-15-appointment-invariants.txt`
  (`Tests run: 215, Failures: 0, Errors: 0, Skipped: 0 – BUILD SUCCESS`).
- **Examples of unsuccessful runs** (real, from defect reproduction):
  - `run-2-DEF-01-failing.txt`: `BookingTest.pastScheduledAppointmentsAreNotActive: Unexpected exception thrown: SchedulingException: Mehmet Kaya (12345678901) already has 3 active appointments`
  - `run-5-DEF-02-03-04-failing.txt`: `BookingTest.idsAreLocaleIndependent: expected: <A0001> but was: <A????>`
    and `EncapsulationTest.noteRuleCannotBeBypassed: Expected ValidationException to be thrown, but nothing was thrown.`
- Screenshot suggestion: VS Code *Testing* panel (green/red ticks) and `target/reports/surefire.html`.

## 6. Code coverage and defect analysis

### Coverage (JaCoCo 0.8.12, run 15; `Main` excluded as a demo without logic)

| Scope | Instruction | Branch | Line | Method |
|---|---|---|---|---|
| Whole project | 1648/1648 = 100 % | 176/178 = 98.9 % | 317/317 = 100 % | 88/88 = 100 % |
| Core logic `com.clinic.appointment` | 1093/1093 = 100 % | 140/140 = 100 % | 219/219 = 100 % | 58/58 = 100 % |

Uncovered: only the `equals(...)` branch for an object of another class in `Patient` and `Doctor`.
No test was added just to cover it: such a test would not check a requirement.

**What the coverage report taught us (worth discussing):** before `AppointmentTest` was added, the
core package had one uncovered branch – the status guard inside `Appointment.changeStatus`. It cannot be
reached through the public API because `AppointmentService` checks the status first. That is the same
check whose removal mutant M13 simulated. The guard and the null checks in the `Appointment`
constructor are invariants of the class, so they are now tested directly (TC-A-01, TC-A-02).

**A measurement mistake that was caught:** the first coverage figures written into these documents were
read from `target/` right after the mutation script, i.e. from a build of *mutated* code. A clean checkout
of the committed code gave different numbers, and the documents were corrected. Lesson: coverage is a
property of one specific build; always measure from `mvnw clean test` on the committed code.

### Why high coverage did not mean "no defects" – evidence from this project

- When DEF-02 and DEF-03 were present, line coverage was already ~99 %. Each faulty line *was executed*
  by tests, but never with the input that triggers the fault (a non-Latin locale, NFD text).
  Coverage measures what was **run**, not what was **checked**.
- Mutation round 1: mutant M13 survived although the status check it removes was executed by many
  tests – no assertion depended on *which* check rejected the request.
- Coverage cannot reveal **missing** code (DEF-01 was a missing time condition) or **wrong requirements**.
- See Inozemtseva & Holmes [6]: coverage is only weakly correlated with fault-detection ability once
  suite size is controlled for; mutation analysis [8] is a stronger effectiveness measure.

### Defects

Four genuine defects (DEF-01…DEF-04), each reproduced by a failing test before the fix, then closed
with a full regression run – see `docs/defect-log.md`. How they were found:

| Defect | Found through |
|---|---|
| DEF-01 | Boundary thinking about *time passing* (moving the clock a day forward) |
| DEF-02 | Code review of locale-sensitive API (`String.format`), confirmed by a probe program |
| DEF-03 | Equivalence class "letters" examined more carefully (Unicode normalisation forms) |
| DEF-04 | Design audit (encapsulation), confirmed by a test in another package |

## 7. Discussion and conclusion – points to evaluate critically

**What worked**
- Clock injection made every time boundary exactly testable and the suite deterministic (215 test invocations in about 2 seconds of test time).
- Asserting the rule ID removed a whole class of "passes for the wrong reason" tests; mutation
  analysis confirmed the BVA tests catch every injected off-by-one fault (M01–M09).
- Writing the failing test first for each defect produced clear before/after evidence.

**Limitations of the system**
- In-memory, single-user, not thread-safe; data lost on exit.
- Fixed 30-minute slots, no holidays, no doctor-specific working hours or leave.
- Times are `LocalDateTime` (no time zones/DST handling).
- National-ID format only, no checksum.
- Late *staff reschedule* is allowed but not flagged (requirement ambiguity E1).

**Limitations of the testing**
- Tests and code were written by the same author (with AI assistance): shared assumptions can hide
  requirement misunderstandings; no independent tester or user acceptance test.
- Mutation analysis was manual (13 hand-picked mutants), not a tool like PIT; results show the suite
  is strong for those fault types only.
- No property-based or randomised testing; no performance or concurrency testing.
- The first test run before DEF-01 was not saved.

**Possible improvements**
- Automated mutation testing (PIT), holidays calendar, persistence (file/DB), per-doctor hours,
  national-ID checksum, flag late staff reschedules (after clarifying E1).

## 8. References

Checked on 2026-10-28: the web pages [1]–[4], [9] were opened and their titles confirmed; the
metadata of [6] and [8] was confirmed through Crossref (authors, venue, volume, pages). [5] and [7]
are standard printed/PDF sources that were **not** checked online – verify the edition you cite.
Note: the "current" JUnit guide now documents JUnit 6, so [1] deliberately links the 5.11.4 guide
that matches this project.

1. JUnit Team. *JUnit 5 User Guide* (version 5.11.4). https://docs.junit.org/5.11.4/user-guide/
2. Mountainminds GmbH & Co. KG and Contributors. *JaCoCo Documentation – Coverage Counters*. https://www.jacoco.org/jacoco/trunk/doc/counters.html
3. Oracle. *Java SE 21 API Specification – `java.time.Clock`*. https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html
4. Apache Software Foundation. *Maven Surefire Plugin*. https://maven.apache.org/surefire/maven-surefire-plugin/
5. Myers, G. J., Sandler, C., & Badgett, T. (2011). *The Art of Software Testing* (3rd ed.). John Wiley & Sons.
6. Inozemtseva, L., & Holmes, R. (2014). Coverage is not strongly correlated with test suite effectiveness. In *Proceedings of the 36th International Conference on Software Engineering (ICSE 2014)* (pp. 435–445). ACM. https://doi.org/10.1145/2568225.2568271
7. ISTQB. (2023). *Certified Tester Foundation Level Syllabus, Version 4.0*. International Software Testing Qualifications Board.
8. Jia, Y., & Harman, M. (2011). An analysis and survey of the development of mutation testing. *IEEE Transactions on Software Engineering, 37*(5), 649–678. https://doi.org/10.1109/TSE.2010.62
9. The Unicode Consortium. *Unicode Standard Annex #15: Unicode Normalization Forms*. https://unicode.org/reports/tr15/

## Research component – talking points (write these sections yourself)

| Topic | Points you can develop | Sources |
|---|---|---|
| Role of testing in QA | Verification vs validation; testing shows presence, not absence, of defects; QA is process-wide, testing is one activity | [5], [7] |
| Automated unit testing | Fast feedback, repeatable regression, documents behaviour, enables refactoring (this project refactored packages twice with zero regressions) | [1], [5] |
| JUnit 5 features | Platform/Jupiter/Vintage architecture; parameterized tests; nested tests; display names; tags; extension model | [1] |
| Test design techniques | EP, BVA, decision tables, negative testing – how each fit this domain (`docs/test-plan.md` §3) | [5], [7] |
| Code coverage and its limits | Counters (instruction, branch, line, method); coverage ≠ effectiveness; examples DEF-02/03 and M13 from this project | [2], [6], [8] |
| Your own challenges | Write from your own experience: Java syntax, `java.time`, Maven setup on Windows (PKIX error), understanding parameterized tests, deciding boundaries | – |

## AI assistance disclosure (adapt and include in the report)

> This project was developed with the assistance of Claude (Anthropic), an AI assistant, used inside
> Visual Studio Code. Claude generated the initial Java implementation, the JUnit 5 test suite, the
> Maven configuration and the documentation drafts from my approved proposal and requirements, and
> later audited and refactored the code, identified the defects DEF-01–DEF-04, wrote the tests that
> reproduce them, performed the mutation analysis and produced the coverage figures by running the
> tools. All test results and coverage numbers come from actual executions recorded in
> `docs/test-results/` and the git history. I reviewed the code, tests and analysis, and the report
> text is written by me. [Adjust this paragraph to describe accurately what you did yourself.]
