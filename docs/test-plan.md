# Test Plan, Test Design and Traceability

## 1. Test strategy

| Item | Decision |
|---|---|
| Test level | Unit and component tests of the Java classes (no UI, no database, no network). |
| Framework | JUnit 5 (Jupiter) 5.11.4, run by Maven Surefire 3.5.2. |
| Test basis | Functional requirements FR-01…FR-10 and business rules BR-01…BR-26 (`com.clinic.rules.BusinessRule`). |
| Time | All time-dependent rules use an injected `Clock`. Tests use `MutableClock`, fixed at Monday 2026-11-02 08:00 and moved explicitly. No test reads the real clock, sleeps or uses random data. |
| Independence | `ServiceTestBase` creates a new service, clock, doctors and patients before **every** test (`@BeforeEach`). Tests can run in any order. |
| Oracle | Accepted operations: the returned object and the stored state. Rejected operations: the exception **type** and the violated **`BusinessRule`** (`RuleAssertions.assertRejected`), and that the state is unchanged. |
| Structure | Arrange–Act–Assert, separated by blank lines. Test case IDs (`TC-x-nn`) in every `@DisplayName`. Techniques in `@Tag` (`EP`, `BVA`, `DecisionTable`, `Negative`). |
| Regression | The complete suite is run after every change (`mvnw test`); every run is saved in `docs/test-results/`. |
| Effectiveness check | JaCoCo coverage and a manual mutation analysis (`scripts/mutation_analysis.py`). |
| Exit criteria | All tests pass; every FR and BR has at least one automated test; no known open defect. |

Fixture data: doctors `D1` (limit 12), `D2` (limit 12), `D3` (limit 2, used for limit boundaries);
patients `12345678901`, `23456789012`, `34567890123`, `45678901234`.

## 2. Business rule catalogue

| ID | Rule | Valid inputs | Invalid inputs | Expected behaviour | Tests |
|---|---|---|---|---|---|
| BR-01 | Patient ID: 11 digits, first digit 1–9 | `12345678901` | 10/12 digits, leading `0`, letter, space, sign, null, empty | `ValidationException(BR-01)` | TC-P-03, TC-P-04 |
| BR-02 | Name: 2–50 chars after trimming (NFC), letters/space/`-`/`'`, starts with a letter | `Ali`, `Şükrü Çağlar-Öz O'Neil`, NFD text | null, blank, 1 or 51 chars, digits, `@`, leading `-`/`'` | `ValidationException(BR-02)` | TC-P-02, 07–10, 12; TC-D-06 |
| BR-03 | Patient age 0–120 | 0, 1, 119, 120 | -1, 121, `Integer.MIN/MAX_VALUE` | `ValidationException(BR-03)` | TC-P-05, TC-P-06 |
| BR-04 | Doctor ID and specialty not empty | `D1`, `Cardiology` | null, `""`, blank | `ValidationException(BR-04)` | TC-D-04, TC-D-05 |
| BR-05 | Doctor daily limit 1–14 | 1, 2, 13, 14 | -1, 0, 15 | `ValidationException(BR-05)` | TC-D-02, TC-D-03 |
| BR-06 | Patient/doctor IDs unique | new ID | already registered ID | `ValidationException(BR-06)`, first record kept | TC-R-01, TC-R-02 |
| BR-07 | Referenced patient/doctor/appointment must exist | registered ID | unknown, null, empty, wrong case (`d1`) | `ValidationException(BR-07)` | TC-B-14, 15; TC-C-06; TC-N-08; TC-Q-09 |
| BR-08 | Required values not null | non-null | null start/date/requester/patient/doctor | `ValidationException(BR-08)` | TC-S-06, TC-R-03, TC-C-05, TC-RS-06, TC-Q-09, TC-TP-03 |
| BR-09 | Start on :00 or :30, no seconds | 10:00, 10:30 | 09:01, 09:15, 09:29, 09:31, 16:59, +1 s, +1 ns | `ValidationException(BR-09)` | TC-S-04, TC-S-05 |
| BR-10 | Weekdays only | Mon–Fri | Sat, Sun | `SchedulingException(BR-10)` | TC-S-07, TC-S-08 |
| BR-11 | Start between 09:00 and 16:30 | 09:00 … 16:30 | 00:00, 08:00, 08:30, 17:00, 17:30, 23:30 | `SchedulingException(BR-11)` | TC-S-01, TC-S-02 |
| BR-12 | No start in 12:00–13:00 | 11:30, 13:00 | 12:00, 12:30 | `SchedulingException(BR-12)` | TC-S-01, TC-S-03, TC-RS-04 |
| BR-13 | At least 1 hour ahead | now ≤ start − 1 h | now 08:01…10:00 for 09:00; past date | `SchedulingException(BR-13)` | TC-S-09–11, TC-Q-04 |
| BR-14 | At most 30 days ahead (calendar date) | today + 29, + 30 | today + 31, + 365 | `SchedulingException(BR-14)` | TC-S-12, TC-S-13, TC-Q-03 |
| BR-15 | One appointment per doctor per slot | different slot, adjacent slot | same doctor, same start | `SchedulingException(BR-15)` | TC-B-04, 08, 09, 18; TC-RS-03 |
| BR-16 | One appointment per patient per slot | adjacent slot | same patient, same start, other doctor | `SchedulingException(BR-16)` | TC-B-05, 19; TC-RS-12 |
| BR-17 | Doctor's daily limit (SCHEDULED + COMPLETED + NO_SHOW) | below limit, cancelled not counted | at limit | `SchedulingException(BR-17)` | TC-B-06, 10, 12; TC-ST-07; TC-RS-08, 13 |
| BR-18 | Max 3 upcoming SCHEDULED appointments per patient | 0–2 upcoming; past unclosed ones not counted | 3 upcoming | `SchedulingException(BR-18)` | TC-B-07, 11, 16; TC-RS-07 |
| BR-19 | < 24 h before start only staff may cancel/reschedule; recorded as late | ≥ 24 h (anyone); < 24 h (staff) | < 24 h (patient) | `SchedulingException(BR-19)`; staff → `lateCancellation=true` | TC-C-01, 02, 07; TC-RS-09 |
| BR-20 | No cancel/reschedule at or after start | before start | at start, after start | `SchedulingException(BR-20)` | TC-C-03, TC-RS-11 |
| BR-21 | Only SCHEDULED can change; status reported before time rules | SCHEDULED | CANCELLED, RESCHEDULED, COMPLETED, NO_SHOW | `SchedulingException(BR-21)` | TC-C-04, 09; TC-RS-10, 14; TC-ST-09 |
| BR-22 | Complete only from the start time | at start, later | 1 min before start | `SchedulingException(BR-22)` | TC-ST-01, 02, 10 |
| BR-23 | No-show only from the end time | at end | 1 min before end | `SchedulingException(BR-23)` | TC-ST-03, 04 |
| BR-24 | Reschedule target differs from current time | other slot | same slot | `SchedulingException(BR-24)` | TC-RS-05 |
| BR-25 | Note 1–200 characters after trimming | 1, 2, 199, 200 | null, empty, blank, 201 | `ValidationException(BR-25)` | TC-N-01–04 |
| BR-26 | At most 5 notes per appointment | 5th note | 6th note | `SchedulingException(BR-26)` | TC-N-05 |

## 3. How each technique was applied

### 3.1 Equivalence partitioning (EP)

Each input was split into classes that the code should treat the same way; one or two
representatives per class were chosen. Examples:

| Input | Valid classes | Invalid classes | Representatives |
|---|---|---|---|
| Patient ID | 11 digits starting 1–9 | too short, too long, leading zero, non-digit, null/empty | `12345678901`; `1234567890`, `01234567890`, `1234567890A`, null |
| Name | letters incl. Turkish, hyphen, apostrophe; NFC or NFD | digits, symbols, wrong first char, blank | `Şükrü Çağlar-Öz O'Neil`; `Mehmet3`, `Ali@Veli`, `-Ali` |
| Day of week | Mon–Fri | Sat, Sun | Fri 06.11, Mon 09.11; Sat 07.11, Sun 08.11 |
| Minute of start | 0, 30 | any other | 09:01, 09:15, 09:29, 09:31 |
| Requester | PATIENT, STAFF | null | `@EnumSource(Requester.class)`, null |
| Status | SCHEDULED | four final statuses | all five (TC-ST-09) |

*Justification:* one value per class is enough if the implementation treats the class uniformly;
extra values are added only at class edges (that is BVA).

### 3.2 Boundary value analysis (BVA)

Every numeric or time limit was tested **below, at and above** the boundary. Time boundaries use exact
timestamps set on `MutableClock`, never the real clock.

| Boundary | Below | At | Above | Tests |
|---|---|---|---|---|
| Opening 09:00 | 08:30 ✗ | 09:00 ✓ | 09:30 ✓ | TC-S-01/02 |
| Last start 16:30 | 16:00 ✓ | 16:30 ✓ | 17:00 ✗ | TC-S-01/02 |
| Lunch 12:00–13:00 | 11:30 ✓ | 12:00 ✗, 12:30 ✗ | 13:00 ✓ | TC-S-01/03 |
| Lead time 1 h (09:00 slot) | now 07:59 ✓ | now 08:00 ✓ | now 08:01 ✗ | TC-S-09/10 |
| Advance 30 days | +29 ✓ | +30 ✓ | +31 ✗ | TC-S-12/13 |
| Free cancellation 24 h | 23 h 59 min: patient ✗, staff ✓ late | 24 h: ✓ not late | 26 h: ✓ not late | TC-C-01/02 |
| Start of appointment | 1 min before ✓ | at start ✗ cancel, ✓ complete | 1 min after ✗ | TC-C-03, TC-ST-01/02 |
| End of slot (no-show) | 10:29 ✗ | 10:30 ✓ | — | TC-ST-03/04 |
| Doctor limit (2) | 1 ✓ | 2nd ✓ | 3rd ✗ | TC-B-06 |
| Patient active limit (3) | 2 ✓ | 3rd ✓ | 4th ✗ | TC-B-07 |
| Doctor limit range 1–14 | 0 ✗ | 1 ✓, 14 ✓ | 15 ✗ | TC-D-02/03 |
| Age 0–120 | -1 ✗ | 0 ✓, 120 ✓ | 121 ✗ | TC-P-05/06 |
| Name 2–50 | 1 ✗ | 2 ✓, 50 ✓ | 51 ✗ | TC-P-07/08/09 |
| Note 1–200 | 0 ✗ | 1 ✓, 200 ✓ | 201 ✗ | TC-N-02/03/04 |
| Notes per appointment 5 | 4 ✓ | 5 ✓ | 6 ✗ | TC-N-05 |
| Overlap (adjacent slot) | 09:30 ✓ | 10:00 ✗ | 10:30 ✓ | TC-B-04/18/19 |

*Justification:* off-by-one mistakes (`<` vs `<=`, `isBefore` vs `!isAfter`) are the most common
defects in scheduling code; the mutation analysis (M01–M09) confirms these tests catch them.

### 3.3 Decision table testing

**Booking (BookingTest)** – conditions in checking order:

| | R1 | R2 | R3 | R4 | R5 |
|---|---|---|---|---|---|
| Doctor free | Y | N | Y | Y | Y |
| Patient free | Y | – | N | Y | Y |
| Doctor under limit | Y | – | – | N | Y |
| Patient under limit | Y | – | – | – | N |
| **Result** | book | BR-15 | BR-16 | BR-17 | BR-18 |
| **Test** | TC-B-03 | TC-B-04 | TC-B-05 | TC-B-06 | TC-B-07 |

**Cancellation (CancellationTest)**:

| | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---|---|---|---|---|---|
| Status SCHEDULED | Y | Y | Y | Y | Y | N |
| Time left | ≥ 24 h | ≥ 24 h | < 24 h | < 24 h | ≤ 0 | – |
| Requester | Patient | Staff | Patient | Staff | – | – |
| **Result** | cancel | cancel | BR-19 | cancel, late | BR-20 | BR-21 |
| **Test** | TC-C-01 | TC-C-01 | TC-C-02 | TC-C-01 | TC-C-03 | TC-C-04, 09 |

**Rescheduling (RescheduleTest)**:

| | R1 | R2 | R3 | R4 | R5 | R6 | R7 |
|---|---|---|---|---|---|---|---|
| Original SCHEDULED | Y | Y | Y | Y | Y | Y | N |
| Before start | Y | Y | Y | Y | Y | N | – |
| ≥ 24 h or staff | Y | Y | Y | Y | N | – | – |
| Target slot valid | Y | Y | N | Y | – | – | – |
| Target slot free | Y | N | – | Y | – | – | – |
| Target differs | Y | Y | Y | N | – | – | – |
| **Result** | move | BR-15 | BR-12 | BR-24 | BR-19 | BR-20 | BR-21 |
| **Test** | TC-RS-01 | TC-RS-03 | TC-RS-04 | TC-RS-05 | TC-RS-09 | TC-RS-11 | TC-RS-10 |

**Status transitions (StatusTransitionTest, TC-ST-09)** – 5 statuses × 4 operations = 20 rules,
all automated in one `@ParameterizedTest`. For each rule the clock is set to a time at which the
operation is allowed for a SCHEDULED appointment, so the status is the only condition that varies.

| From \ Operation | cancel | reschedule | complete | no-show |
|---|---|---|---|---|
| SCHEDULED | CANCELLED | RESCHEDULED | COMPLETED | NO_SHOW |
| CANCELLED | BR-21 | BR-21 | BR-21 | BR-21 |
| RESCHEDULED | BR-21 | BR-21 | BR-21 | BR-21 |
| COMPLETED | BR-21 | BR-21 | BR-21 | BR-21 |
| NO_SHOW | BR-21 | BR-21 | BR-21 | BR-21 |

### 3.4 Negative testing

55 test cases carry the `Negative` tag. Every one asserts (a) the exception type, (b) the exact
`BusinessRule`, and where state could change (c) that nothing changed: no appointment stored,
status still SCHEDULED, no note added, no appointment ID consumed (TC-B-13, TC-RS-14).
Covered: unknown/null IDs, null inputs, malformed times, overlaps, limit violations, invalid
status transitions, double cancellation, unauthorised (patient) late changes, and attempts to
modify returned lists or `Appointment` objects from outside the package (TC-N-07, TC-Q-11, TC-E-01/02).

### 3.5 Regression testing

The full suite was rerun after every change and every run was saved (`docs/test-results/run-*.txt`):

| Run | Change | Result |
|---|---|---|
| 2 | DEF-01 reproduction test added | 154 run, 1 failure (expected) |
| 3 | DEF-01 fixed | 154/154 pass |
| 4 | Notes feature (FR-10) | 168/168 pass |
| 5 | DEF-02, DEF-03, DEF-04 reproduction tests added | 172 run, 3 failures + 1 error (expected) |
| 6 | DEF-02 fixed | 172 run, 2 failures + 1 error (DEF-03, DEF-04 open) |
| 7 | DEF-03 fixed | 172 run, 2 failures (DEF-04 open) |
| 8 | Refactor: rules package, rule IDs | 172 run, same 2 failures only – no regression |
| 9 | DEF-04 fixed (package-private mutators) | 173/173 pass |
| 10 | Refactor: TimeSlotPolicy extracted | 173/173 pass |
| 11 | National ID masked | 174/174 pass |
| 12 | Tests strengthened | 209/209 pass |
| 13 | Tests added after mutation round 1 | 212/212 pass |
| 14 | Surefire reports with display names (`clean test`) | 212/212 pass |
| 15 | `AppointmentTest` added for invariants unreachable through the API (`clean test`) | 215/215 pass |

The initial suite run (before DEF-01) was not saved; this is a gap in the evidence.

## 4. Testing technique matrix

| Technique | Test classes | Test cases (tag count) |
|---|---|---|
| Equivalence partitioning | PatientTest, DoctorTest, SlotValidationTest, BookingTest, ScheduleQueryTest, StatusTransitionTest | 16 |
| Boundary value analysis | all time/limit tests (see 3.2) | 35 |
| Decision table | BookingTest, CancellationTest, RescheduleTest, StatusTransitionTest | 18 |
| Negative testing | every class | 55 |
| Regression | the whole suite, every run | 114 test cases / 215 invocations |
| Other functional tests (untagged) | queries, ordering, ID generation, TimeSlotPolicy, encapsulation | 27 |

A test case can carry several tags, so the counts overlap. Counts are produced by
`scripts/test_case_report.py`.

## 5. Requirements traceability matrix

Actual result: all listed test cases passed in run 15 (`docs/test-results/test-case-results.md`).

| Req. | Business rules | Implementation | Test cases | Techniques | Expected result | Actual | Status |
|---|---|---|---|---|---|---|---|
| FR-01 Patient data | BR-01, BR-02, BR-03 | `Patient` (constructor, `validateId`, `validateAge`), `NameValidator.validate` | TC-P-01 … TC-P-13 | EP, BVA, Negative | Valid data accepted and normalised; invalid rejected with the rule | as expected | Pass |
| FR-02 Doctor data | BR-02, BR-04, BR-05 | `Doctor` constructor | TC-D-01 … TC-D-07 | EP, BVA, Negative | Same | as expected | Pass |
| FR-03 Time slots | BR-08 … BR-14 | `TimeSlotPolicy.findViolation / validate / dailySlots` | TC-S-01 … TC-S-13, TC-TP-01 … TC-TP-04 | EP, BVA, Negative | Only aligned weekday slots in hours, outside lunch, 1 h – 30 days ahead | as expected | Pass |
| FR-04 Booking | BR-07, BR-15 … BR-18 | `AppointmentService.book`, `checkAvailability` | TC-B-01 … TC-B-19 | DT, BVA, EP, Negative | Conflicts and limits rejected, state unchanged | as expected | Pass |
| FR-05 Registration | BR-06, BR-08 | `registerPatient`, `registerDoctor` | TC-R-01 … TC-R-04 | Negative | Duplicates/null rejected, first record kept | as expected | Pass |
| FR-06 Cancellation | BR-07, BR-08, BR-19, BR-20, BR-21 | `cancel`, `checkChangePolicy`, `Appointment.cancel` | TC-C-01 … TC-C-09 | DT, BVA, Negative | Decision table outcomes, late flag | as expected | Pass |
| FR-07 Rescheduling | BR-08, BR-12, BR-15 … BR-21, BR-24 | `reschedule` | TC-RS-01 … TC-RS-14 | DT, BVA, Negative | Atomic move or no change | as expected | Pass |
| FR-08 Status lifecycle | BR-21, BR-22, BR-23 | `complete`, `markNoShow`, `Appointment.changeStatus`; package-private mutators | TC-ST-01 … 04, 07, 09, 10; TC-E-01, TC-E-02; TC-A-01 … TC-A-03 | DT, BVA, Negative | Only SCHEDULED changes; time limits | as expected | Pass |
| FR-09 Queries | BR-07, BR-08, BR-13, BR-14, BR-17 | `getAvailableSlots`, `getDoctorSchedule`, `getPatientAppointments`, `findAppointment` | TC-Q-01 … TC-Q-11 | EP, BVA, Negative | Correct, sorted, read-only results | as expected | Pass |
| FR-10 Notes | BR-07, BR-25, BR-26 | `addNote`, `Appointment.getNotes` | TC-N-01 … TC-N-08 | EP, BVA, Negative | Valid notes stored trimmed; limits enforced | as expected | Pass |

**Retired test IDs** (kept out of use so old logs stay unambiguous): TC-ST-05 and TC-ST-06
(final-status checks, superseded by the full table TC-ST-09), TC-ST-08 (called model methods
directly; superseded by TC-E-01 after DEF-04), TC-E-03 (duplicate of TC-N-07).

**Requirements without adequate automated verification:** none of FR-01…FR-10.
Not covered by automated tests: the console demo (`Main`, checked by running it – output in
`docs/test-results/demo-output.txt`), thread safety, performance and persistence, none of which
are in the requirements.
