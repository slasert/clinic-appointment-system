# Phase 1 – Project Audit

Audit of the project as of commit `a9069ba` (before any refactoring). Every "confirmed" item
was reproduced by running code; nothing here is assumed.

## Strengths

- Clear functional scope (FR-01 … FR-10) that matches the approved proposal.
- Time-dependent rules use an injected `java.time.Clock`, so every time boundary is testable exactly.
- Business rule numbers live in one place (`ClinicRules`).
- Rescheduling is atomic: all checks run before any state change.
- 168 automated tests, all passing, with explicit EP / BVA / decision-table / negative tags.
- One genuine defect (DEF-01) was already found and fixed with a recorded failing run and a regression run.

## A. Genuine defects (behaviour is wrong)

| ID | Issue | Location | Severity | Recommended improvement | Reason / evidence |
|---|---|---|---|---|---|
| A1 → DEF-02 | Appointment IDs depend on the JVM default locale. With `ar-EG` the first ID is `A٠٠٠١` (U+0660 digits), so `findAppointment("A0001")` fails and IDs differ between machines. | `AppointmentService.nextId()` | Medium | Format with `Locale.ROOT`. | Confirmed with a probe program: `String.format(Locale.forLanguageTag("ar-EG"), "A%04d", 1)` → `[U+0041, U+0660, U+0660, U+0660, U+0661]`. |
| A2 → DEF-03 | Valid Turkish names are rejected when the text arrives in decomposed Unicode form (NFD), e.g. `Şükrü` = `S` + U+0327 + … . Combining marks are not letters (`\p{L}`), and NFD also inflates the length (5 → 8). | `NameValidator.validate()` | Medium | Normalise the name to NFC before validating and storing it. | Confirmed: NFC `Şükrü` matches the pattern, NFD form does not. Copy-pasted text (PDF, macOS) is often NFD. |
| A3 → DEF-04 | Rules can be bypassed. `Appointment.cancel/complete/markNoShow/markRescheduled/addNote` are `public`, and the service hands out `Appointment` objects (`book`, `findAppointment`, `getPatientAppointments`). Any caller can cancel without the 24-hour policy, complete before the start, or add a 500-character note. | `model.Appointment` | High | Make state-changing methods package-private and place `Appointment` in the same package as the service that enforces the rules. | Confirmed by a test in another package (see defect log). |

## B. Design and code-quality weaknesses (behaviour correct, maintainability affected)

| ID | Issue | Location | Severity | Recommended improvement | Reason |
|---|---|---|---|---|---|
| B1 | Package cycle: `model` imports `service.ClinicRules`, `service` imports `model`. | `Doctor`, `Appointment` | Medium | Move rule constants into their own `rules` package with no dependencies. | Cycles make classes harder to understand and test in isolation. |
| B2 | `AppointmentService` mixes time-slot rules with booking coordination; `isBookableTime` uses exceptions for normal control flow. | `AppointmentService.validateSlot / isBookableTime` | Medium | Extract a small `TimeSlotPolicy` that *returns* the violated rule; the service throws. | Single responsibility; the slot rules become directly unit-testable; no try/catch-as-if. |
| B3 | Exceptions carry no rule identity, so tests can only check the exception *type*. Fifteen different rules throw `SchedulingException`, so a test can pass for the wrong reason. | `exception` package, all tests | High (test quality) | Add a `BusinessRule` enum (BR-01 …) to every exception and assert the rule in tests. | Makes tests precise and gives the traceability matrix stable rule IDs. |
| B4 | No common exception base class; callers must catch two types. | `exception` package | Low | `ClinicException` as a common base. | Simpler, consistent error handling (`Main`, future callers). |
| B5 | `SLOTS_PER_DAY = 14` is hard-coded although it follows from the opening, lunch and closing times. | `ClinicRules` | Low | Compute it from the time constants. | Changing working hours would silently break the doctor-limit range. |
| B6 | The full national ID is printed in `toString()` and therefore in every error message. | `Patient.toString()` | Low (privacy) | Mask the ID (`123******01`). | Personal data should not leak into logs or console output (KVKK/GDPR principle of data minimisation). |
| B7 | `Appointment` constructor accepts `null` patient/doctor/start. | `Appointment` | Low | Fail fast with `Objects.requireNonNull`. | Invariant protection. |
| B8 | Status is checked twice (`requireScheduled` in the service and `changeStatus` in `Appointment`). | both | Low | Keep both on purpose: the service check gives the right *reason* before time checks; the model check protects the invariant. Document it. | Not duplication of logic but defence in depth. |

## C. Test-suite weaknesses

| ID | Issue | Severity | Improvement |
|---|---|---|---|
| C1 | Rejection tests only assert the exception type (see B3). | High | Assert the violated `BusinessRule`. |
| C2 | Several rejection tests do not verify that the system state is unchanged (slot rules, patient/doctor limits). | Medium | Assert "no appointment was stored" after each rejection. |
| C3 | Missing cases: back-to-back slots (adjacent boundary of the overlap rule), reschedule after the start, reschedule of a completed appointment, free slots on a past date, returned lists are read-only, locale-independent IDs, decomposed Unicode names. | Medium | Add targeted tests. |
| C4 | `TC-ST-08` calls model methods directly, coupling the test to internal API. | Low | Replace with an API-level encapsulation test. |
| C5 | Time-slot rules are only tested through `book()`. | Low | Add direct `TimeSlotPolicy` tests after extraction. |

## D. Documentation and build

| ID | Issue | Severity | Improvement |
|---|---|---|---|
| D1 | README lacks several sections required for the submission (objectives, business rules, limitations, AI disclosure …). | Medium | Rewrite README. |
| D2 | No traceability matrix, business-rule catalogue or structured defect log. | Medium | Add `docs/test-plan.md`, `docs/defect-log.md`. |
| D3 | The initial test run (before DEF-01) was not saved as a log; only later runs are in `docs/test-results/`. | Low | State this honestly; keep saving every run from now on. |
| D4 | Build from a clean checkout has not been verified. | Medium | Clone into an empty folder and run `mvnw clean verify`. |

## E. Requirement ambiguities (not changed – documented instead)

| ID | Ambiguity | Decision |
|---|---|---|
| E1 | FR-06 records a *late cancellation* for staff; FR-07 says rescheduling "follows the cancellation policy" but does not say whether a late staff reschedule is recorded. | Behaviour kept (not recorded); listed as an open question. |
| E2 | Public holidays are not in the approved requirements. | Not implemented; listed as future work. |
| E3 | Cancellation fees and VIP policies are not in the approved requirements. | Not implemented (no invented requirements). |
| E4 | FR-01 requires 11 digits but not the official national-ID checksum. | Format-only validation kept; checksum listed as future work. |
| E5 | Appointment duration is fixed at 30 minutes; there is no variable duration, so min/max duration boundaries do not apply. | Documented. |

## F. Resolution status (after Phases 3–5)

| Audit item | Outcome | Commit |
|---|---|---|
| A1 / DEF-02 | Fixed (`Locale.ROOT`) | `0977400` |
| A2 / DEF-03 | Fixed (NFC normalisation) | `27d1ab7` |
| A3 / DEF-04 | Fixed (package-private mutators in `com.clinic.appointment`) | `bf6a171` |
| B1 package cycle | Fixed (`com.clinic.rules`) | `9493097` |
| B2 mixed responsibilities | Fixed (`TimeSlotPolicy` extracted) | `33e335c` |
| B3 / C1 rule identity | Fixed (`BusinessRule` on every exception; `RuleAssertions`) | `9493097`, `18e20e9` |
| B4 common base exception | Fixed (`ClinicException`) | `9493097` |
| B5 hard-coded slot count | Fixed (derived) | `9493097` |
| B6 national ID in messages | Fixed (masked) | `1c8bfa0` |
| B7 null constructor arguments | Fixed (`Objects.requireNonNull`) | `bf6a171` |
| B8 double status check | Kept intentionally; mutation M13 showed it determines the reported reason – now tested (TC-C-09, TC-RS-14) | `de8c64a` |
| C2–C5 test gaps | Fixed | `18e20e9`, `de8c64a` |
| D1, D2 documentation | Fixed (README, test-plan, defect-log, report-material) | this phase |
| D3 missing first log | Cannot be recovered; stated in the test plan | – |
| D4 clean checkout | Verified in Phase 7 | – |
| E1–E5 ambiguities | Documented, not implemented | – |
