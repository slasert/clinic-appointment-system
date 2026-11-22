# Clinic Appointment Management System

**Software Validation and Verification — Individual Research Assignment**  
**Student:** Sıla SERT — 220911761  
**Date:** 2 November 2026  
**Language:** Java 21; **testing:** JUnit 5; **build:** Maven

## 1. Introduction and Problem Definition

Small clinics need to coordinate patients, doctors and limited appointment times. When this is done manually, staff can assign the same doctor twice, overlook a patient's existing appointment, or apply cancellation policies inconsistently. A booking that looks correct in isolation can still violate a daily workload limit. These are suitable problems for a small application because they can be expressed as explicit rules with observable outcomes.

The implemented solution is an in-memory Java application with a console demonstration. It registers patients and doctors, books and changes appointments, records notes, and queries schedules. Its purpose is to make scheduling decisions consistent and to demonstrate systematic verification of those decisions. It does not attempt to replace a production clinical information system.

Testing contributes evidence about whether the implementation satisfies the specified rules. Quality assurance also includes preventive activities such as requirement review and clear development practices. Verification asks whether the implementation meets its specification; validation asks whether that specification solves the intended problem. Passing unit tests supports the former but does not establish that clinic staff approve the workflow. These distinctions follow the testing fundamentals in the ISTQB syllabus [1, §§1.1–1.3]. No independent clinic acceptance study was conducted.

The project therefore evaluates both successful operations and rejected operations. A rejected booking must leave stored appointments unchanged and identify the correct violated rule. This is stronger evidence than checking that some exception occurred.

## 2. Requirements Specification

The system is a single-user application. It uses standard Java collections and requires neither a database nor a graphical interface. The console is a repeatable demonstration rather than an interactive receptionist interface. The service API contains the operations being assessed.

| ID | Functional requirement and expected behaviour |
|---|---|
| FR-01 | Patient ID: 11 ASCII digits, first digit nonzero. Name: 2–50 characters after trimming and NFC normalization, allowed letters, spaces, hyphens and apostrophes, beginning with a letter. Age: 0–120. Invalid data is rejected. |
| FR-02 | Doctor ID and specialty must be nonblank; name follows the shared name policy; daily limit is 1–14. |
| FR-03 | Appointments last 30 minutes, start at :00 or :30 with no seconds or nanoseconds, and fall on weekdays at 09:00–16:30, excluding 12:00–13:00. Minimum lead time is one hour. The latest date is today plus 30 calendar days, inclusive. |
| FR-04 | Reject simultaneous scheduled bookings for the same doctor or patient, doctor daily-limit violations, and more than three active patient appointments. Active means SCHEDULED with an end time later than now; an ongoing appointment still counts. |
| FR-05 | Patient and doctor identifiers are unique within their respective registries. Rejection preserves the original record. |
| FR-06 | Anyone may cancel at least 24 hours before the start. Under 24 hours only staff may cancel, with a late flag. Cancellation at or after the start is rejected. |
| FR-07 | Rescheduling follows the change policy, requires a different valid slot, excludes the original booking from conflict and limit checks, and is atomic: both changes succeed or neither occurs. |
| FR-08 | Only SCHEDULED appointments may change status. Completion is permitted from the start; no-show is permitted from the end. Final statuses cannot be changed again. |
| FR-09 | Return a doctor's scheduled appointments for a date, a patient's complete appointment history, and the doctor's available slots. Results are ordered where appropriate. Available slots do not account for a particular patient's eligibility. |
| FR-10 | The staff-facing service records trimmed notes of 1–200 characters, with at most five per appointment, including appointments with final statuses. |

The detailed catalogue assigns BR-01–BR-26 to these rules in [the test plan](test-plan.md). Exceptions carry the relevant `BusinessRule`, providing stable traceability between a requirement, an implementation decision and its test oracle.

Assumptions are explicit. There is no national-ID checksum, public-holiday calendar, authentication or persistence. Staff operations are trusted API calls; this application does not prove the caller is staff. Late staff rescheduling is permitted, but the implementation does not set a late-cancellation flag on the old rescheduled record. That requirement ambiguity is retained and documented rather than treated as an approved policy change.

Instructor approval is a separate prerequisite in the assignment. Existing documentation describes an approved proposal, but no approval record was available for this final review. [The proposal](proposal.md) records the scope without inventing an approval date or decision.

## 3. Implementation

The application separates coordination, domain objects, time validation, rule definitions and errors.

| Component | Responsibility and main methods |
|---|---|
| `AppointmentService` | Registers entities; coordinates `book`, `cancel`, `reschedule`, `complete`, `markNoShow`, `addNote`; implements schedule and availability queries. |
| `TimeSlotPolicy` | Pure time rules through `findViolation`, `validate`, and `dailySlots`. Returns a violation before the caller chooses the exception behaviour. |
| `Appointment` / `AppointmentStatus` | Appointment data and lifecycle. Construction and mutation are package-private; consumers receive public read access. |
| `Patient`, `Doctor`, `NameValidator` | Immutable validated entity data; shared name normalization and validation. Equality is based on identifier. |
| `ClinicRules`, `BusinessRule` | Central numeric limits, opening times, and identifiable rejection reasons. |
| `ClinicException` and subclasses | Common rejection interface, specialized as validation or scheduling errors. |
| `Main` | Fixed-clock demonstration showing accepted operations, rejected inputs, cancellations and schedules. |

The service uses `LinkedHashMap` collections indexed by IDs. Search and conflict checks scan stored appointments, which is appropriate for the assignment's small data volume. This is not a performance claim for a large clinic.

Time is injected through `java.time.Clock`. Oracle describes alternate clock implementations as a way to simplify testing [2]. Here, tests can move a `MutableClock` to the exact cancellation or completion boundary instead of sleeping. The production API can receive a system clock, while the demo uses Monday 2 November 2026 at 08:00.

Rescheduling validates the original appointment, requester, target time, conflicts and limits before changing state. The old appointment is passed as an ignored record during availability checks. Consequently a patient at the active limit can move an appointment without incorrectly counting it twice. Only after all checks succeed does the old record become RESCHEDULED and the new record enter storage.

Encapsulation is also a correctness decision. Public appointment mutators originally allowed callers to bypass the service's rules. Package-private mutators now confine ordinary access to the coordinating package. A model-level status guard additionally protects final states.

Names are normalized to NFC before validation. Unicode normalization defines canonical forms for equivalent text [3]. This matters for copied Turkish names that can contain combining marks. Appointment IDs use `Locale.ROOT` to avoid locale-dependent digit formatting. These design choices are backed by regression tests rather than assumed to work because the usual environment uses Latin digits.

Build and execution:

```powershell
.\mvnw.cmd clean verify
java -jar target/clinic-appointment-1.0-SNAPSHOT.jar
python scripts/test_case_report.py
```

The Maven Wrapper selects Maven 3.9.9. The POM pins JUnit 5.14.4, Surefire 3.6.1 and JaCoCo 0.8.16. Java 21 is required. The runnable JAR contains application classes and needs no runtime library dependencies.

## 4. Test Plan and Test Design

Tests are derived from FR-01–FR-10 and BR-01–BR-26. Each service test receives fresh data. Valid operations assert returned values and stored state; invalid operations assert exception type, rule identity and relevant unchanged state. Test IDs appear in display names so evidence can be traced to requirements. The complete catalogue, decision tables and traceability matrix are in [test-plan.md](test-plan.md).

The selected techniques address different fault risks. Equivalence partitioning samples inputs expected to behave alike. Boundary analysis targets changes in acceptance. Decision tables organize interacting conditions. Regression checks protect existing behaviour after modification [1, §§2.2.3, 4.2]. Their concrete application is as follows.

| Technique | Application and justification |
|---|---|
| Equivalence partitioning | Patient IDs are partitioned into valid IDs, wrong length, leading zero, nondigits, and absent values. Names include ordinary and decomposed Unicode text. These classes exercise different validation paths without enumerating every possible string. |
| Boundary value analysis | Ages −1/0/1 and 119/120/121, note lengths around 1 and 200, opening and closing slots, exactly one hour of notice, and the 30-day date horizon. A cancellation at exactly 24 hours is distinguished from just below it. These cases target off-by-one errors. |
| Decision table testing | Booking combines doctor availability, patient availability, daily limit and patient limit. Cancellation combines status, start time, notice and requester. Rescheduling adds target validity and conflicts. The expected first rejection is specified. |
| Negative testing | Unknown IDs, null values, malformed slots, duplicate registrations, conflicting appointments and repeated changes to final states. A correct rejection must preserve data. |
| Regression testing | The complete suite is rerun after fixes and refactoring. Earlier failing runs are retained separately from the final successful run. |

A representative cancellation table makes the inclusive boundaries visible:

| Scheduled? | Now before start? | Notice ≥24 h? | Requester | Expected |
|---|---|---|---|---|
| No | Any | Any | Either | Reject BR-21 |
| Yes | No | Any | Either | Reject BR-20 |
| Yes | Yes | Yes | Either | Cancel; late=false |
| Yes | Yes | No | Patient | Reject BR-19 |
| Yes | Yes | No | Staff | Cancel; late=true |

This table assumes a nonnull requester and a known appointment; separate tests cover those invalid inputs. A dash or “Any” indicates a condition irrelevant after an earlier rejection, not an untested assumption.

Tests prioritize high-impact behaviour: conflicting reservations, atomic changes and state transitions. The suite additionally checks read-only results and package visibility. The console regression checks that the demo reaches its final schedule, reports both conflict rules, records a late cancellation and reports rescheduling.

## 5. Test Implementation and Results

JUnit 5 comprises Platform, Jupiter and Vintage. This project uses Jupiter; Vintage is unnecessary. `@Test` defines individual checks; `@ParameterizedTest` runs a rule across several inputs; `@BeforeEach` isolates fixtures; `@Nested` groups related decisions; `@DisplayName` records test IDs; and `@Tag` selects technique groups. Assertions combine value checks and expected exceptions [4].

For example, TC-B-01 books patient `12345678901` with D1 on Tuesday at 10:00. It expects ID A0001, SCHEDULED status, the correct participants, an end at 10:30, no late flag, and successful lookup of the same record. TC-B-04 first reserves that slot and then attempts another patient with D1 at 10:00; it expects BR-15 and no extra booking. Parameterized boundary cases represent separate executions of a single designed test case.

The final suite contains **115 designed test cases and 216 JUnit executions: 216 passed, zero failures, zero errors and zero skipped**. The extra executions come from parameterized inputs. Successful clean-build evidence is recorded in `run-25-final-verification.txt`; the preceding `run-24-final-verification.txt` also passed. The [final verification summary](test-results/final-verification.md) records scope and commands. The generated [per-case results](test-results/test-case-results.md) associate test inputs and expectations with actual outcomes. Surefire supplies raw XML and text execution evidence [5]. A submission copy of the XML reports and the full HTML coverage report is preserved under `docs/test-results/final/` so it survives deletion of Maven's `target/` directory.

Unsuccessful executions are also necessary evidence. The historical `run-2-DEF-01-failing.txt` records a booking rejection caused by three past appointments incorrectly counting as active. `run-5-DEF-02-03-04-failing.txt` records locale-sensitive IDs, rejected decomposed names, and bypassed appointment rules. These are development failures, not failures being concealed in the final suite. The later regression logs document the corrections.

Automated unit tests provide repeatable feedback at low execution cost. In this project, package refactoring and extraction of `TimeSlotPolicy` could be checked against the same behaviour. The benefit depends on assertion quality: an assertion that merely checks that an exception occurred may pass for the wrong reason. Rule-specific assertions and state checks make the feedback more useful. They still cannot establish that the requirements themselves are correct.

## 6. Code Coverage and Defect Analysis

JaCoCo measures executed bytecode instructions, branches, lines and methods. Branch coverage concerns conditional outcomes and does not count exception handling as branches. Line coverage requires debug information and records execution, not the correctness of assertions [6]. The final summary reports both whole application logic and the core appointment package with raw covered/total counts.

| Scope | Instruction coverage | Line coverage | Branch coverage | Method coverage |
|---|---|---|---|---|
| All assessed application logic | 1648/1648 (100%) | 317/317 (100%) | 176/178 (98.88%) | 88/88 (100%) |
| Core appointment package | 1093/1093 (100%) | 219/219 (100%) | 140/140 (100%) | 58/58 (100%) |

`Main` is explicitly excluded from the coverage denominator because it formats a demonstration. It is checked separately by TC-DEMO-01 and by running the packaged JAR. This exclusion is disclosed so a percentage is not mistaken for coverage of every application file.

| Defect | Evidence and root cause | Correction and verification |
|---|---|---|
| DEF-01: old bookings blocked new appointments | Advancing the clock exposed that every SCHEDULED record counted as active, even after its end. | Count only records whose end is in the future. TC-B-16 preserves this behaviour. |
| DEF-02: locale-dependent IDs | Under an Arabic locale, formatting produced non-ASCII digits. | Format with `Locale.ROOT`; TC-B-17 changes and restores the default locale. |
| DEF-03: decomposed names rejected | A valid Turkish name in NFD contained combining marks rejected by the original pattern. | Normalize to NFC before validation; TC-P-12 checks acceptance and stored form. |
| DEF-04: public mutations bypassed rules | External callers could complete early or add an oversized note directly. | Restrict construction and mutation visibility; TC-E-01/02 check the accessible API, with service-level rule tests. |

The [defect log](defect-log.md) includes reproduction steps, expected and actual behaviour, severity, corrective action and links to historical failing runs. These defects are distinguished from deliberately injected mutants.

The recorded manual mutation analysis temporarily introduced 13 faults. The first round killed 12; removing the early status check survived because a later check still threw an exception. New tests checked the correct rule and absence of consumed appointment IDs. The second recorded round killed all 13. This demonstrates that exercising a line does not mean its intended behaviour is adequately asserted. The result applies only to these selected mutants; it is not an exhaustive mutation score for every possible defect.

Coverage has further limits in this project. It does not detect omitted requirements, unrealistic policies, authentication gaps or concurrency problems. Locale and Unicode defects existed in lines already exercised with ordinary data. The remaining unexercised equality branches concern comparison with another object type. The report does not inflate the suite with assertions solely to produce a round percentage.

## 7. Discussion and Conclusion

The strongest evidence is the combination of traceable requirements, exact time boundaries, explicit rejection reasons, recorded real defects and repeatable regression results. No single metric supplies that evidence alone. The mutation survivor was particularly useful because it exposed a weakness in an apparently passing suite and led to assertions that distinguish the intended rejection from an accidental later rejection.

Several implementation challenges are visible in the repository. Java's access modifiers affect whether business rules can be bypassed; immutable entity data and package boundaries required deliberate design. Time comparisons require an explicit inclusive or exclusive interpretation. Parameterized tests require distinguishing designed test cases from individual invocations. Unicode character representation and locale-sensitive formatting required testing environmental assumptions. Maven setup introduced certificate-store and repository-permission issues, documented with the execution evidence. These are documented project challenges; this report does not invent a first-person account of the student's learning experience.

The Java concepts needed to explain this project include constructors and invariants, enums, exception inheritance, collections, stream filtering, equality by identifier, access control and `java.time`. The testing concepts include fixture isolation, arrange–act–assert, choosing input classes, checking boundaries and using independent expected outcomes. A student demonstration should explain why exactly 24 hours is allowed, why past bookings cease counting, and why a rejected reschedule changes neither appointment state nor ID sequence. The student must add any personal learning reflection in their own truthful words before presenting it as personal experience.

The system has practical limits. Records disappear at exit, concurrent access is unsupported, appointment length is fixed, holidays are omitted and local times do not model daylight-saving transitions. A staff-facing API is not an authorization mechanism. Free slots describe doctor availability rather than guaranteeing a particular patient's eligibility. The unresolved late-reschedule flag should be clarified before extending the policy.

Testing also has limits. Tests and implementation were developed with AI assistance and may share mistaken assumptions. There was no independent user acceptance, performance or concurrency study. Manual mutation analysis sampled a small set of faults, and the first pre-defect run was not preserved. These limitations constrain claims of correctness even when measured coverage is high.

Useful next steps are independent acceptance scenarios, clarified late-reschedule policy, persistence, doctor-specific availability and automated mutation analysis. Each extension should begin with a testable requirement. The present implementation provides a bounded scheduling solution and documented evidence for its stated rules.

**AI assistance disclosure.** Existing repository records attribute earlier implementation, tests, audits and documentation to Claude (Anthropic). OpenAI Codex assisted with final build configuration, a meaningful console regression test, the final report, source verification and packaging. AI-generated text is disclosed here; it is not represented as independently written student prose. The student remains responsible for understanding, reviewing and explaining the submitted work and following the instructor's AI-use rules. No approval or personal experience has been fabricated.

## 8. References

References were accessed on 2 November 2026. Numbered citations identify supporting sources; project-specific observations come from the accompanying source and execution evidence.

1. International Software Testing Qualifications Board. (2024). *Certified Tester Foundation Level Syllabus*, version 4.0.1. §§1.1–1.3, 2.2.3, 4.2. [Official syllabus](https://istqb.org/?download_id=3345&sdm_process_download=1).
2. Oracle. *Java Platform, Standard Edition 21 API: java.time.Clock*. [API documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/Clock.html).
3. Unicode Consortium. *Unicode Standard Annex #15: Unicode Normalization Forms*. [Technical standard](https://unicode.org/reports/tr15/).
4. JUnit Team. *JUnit 5.14.4 User Guide*. [Overview](https://docs.junit.org/5.14.4/overview.html) and the linked Jupiter writing-tests documentation.
5. Apache Software Foundation. *Maven Surefire Plugin: Introduction*. [Official documentation](https://maven.apache.org/surefire/maven-surefire-plugin/).
6. Mountainminds GmbH & Co. KG and Contributors. *JaCoCo: Coverage Counters*. [Official documentation](https://www.jacoco.org/jacoco/trunk/doc/counters.html).
