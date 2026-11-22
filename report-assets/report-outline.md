# Report outline and material pack

**Student:** Sıla SERT, 220911761. **Required output:** `220911761_Sila_Sert.pdf`, maximum seven A4 pages, 11 pt text, 9 pt code, normal margins. Student writes and checks all final prose; the material below is a fact and evidence pack. Page budgets are targets, not a guarantee: export and count pages. GitHub: https://github.com/slasert/clinic-appointment-system

## 1. Introduction and Problem Definition — ¾ page
- Manual scheduling: conflicts, invalid times, inconsistent cancellations.
- Scope: Java 21 in-memory console demonstration; no database or GUI.
- Research: software testing's role in quality assurance; cite ISTQB [1].

## 2. Requirements Specification — ¾ page
| ID | Topic |
|---|---|
| FR-01 | Patient ID, name, age validation |
| FR-02 | Doctor identity, specialty and limit |
| FR-03 | 30-minute weekday slots, hours, lunch, lead, 30-day date window |
| FR-04 | Doctor/patient conflicts and limits |
| FR-05 | Unique patient and doctor registration |
| FR-06 | Notice-based cancellation and staff override |
| FR-07 | Validated atomic reschedule |
| FR-08 | Lifecycle and start/end constraints |
| FR-09 | Schedules, history and availability |
| FR-10 | Note length and count |
- Scope exclusions: persistence, GUI, authentication, concurrent users.

## 3. Implementation — 1 page
- Layers: service: operations; policy: time rules; model: entities; rules: constants and identifiers; exceptions: rejection categories.
- Clock injection: deterministic time-dependent tests.
- Exact excerpt: [AppointmentService.java lines 71–80](../src/main/java/com/clinic/appointment/AppointmentService.java#L71); copy the ten-line `book` method from the file, preserving attribution and formatting.

## 4. Test Plan and Test Design — 1½ pages
- EP: divide valid/invalid input classes; use for ID/name formats.
- BVA: test below/at/above boundaries; use for opening, lunch, notice and advance window.
- Decision tables: combine conditions and expected outcomes; use for cancellation.
- Negative: verify exception, correct rule and unchanged state.
- Regression: rerun suite after fixes/refactors.

| EP input | Valid class | Invalid class |
|---|---|---|
| ID | 11 ASCII digits; first nonzero | wrong length, non-digit or leading zero |
| Name | 2–50 units; starts with letter; supported characters | blank, invalid characters, too short/long |

| Boundary | Example | Expected, assuming other conditions valid |
|---|---|---|
| Opening/closing | 08:30 / 09:00 / 16:30 / 17:00 | reject / accept / accept / reject |
| Lunch | 11:30 / 12:00 / 13:00 | accept / reject / accept |
| Change notice | 23h59 / 24h / 24h01 | patient reject / accept / accept; staff allowed before start |
| Advance date | today + 30 / + 31 days | accept / reject; other date and time rules still apply |

| Scheduled | Started | Notice ≥24h | Requester | Cancellation result |
|---|---|---|---|---|
| No | any | any | either | reject BR-21 |
| Yes | yes | any | either | reject BR-20 |
| Yes | no | yes | either | allowed, late=false |
| Yes | no | no | patient | reject BR-19 |
| Yes | no | no | staff | allowed, late=true |

## 5. Test Implementation and Results — 1½ pages
- Features used: `@ParameterizedTest`, `@CsvSource`, `@Nested`, `@DisplayName`, `assertThrows`, `@BeforeEach`.
- Excerpts: choose concise complete snippets from [SlotValidationTest](../src/test/java/com/clinic/appointment/SlotValidationTest.java#L46), [CancellationTest](../src/test/java/com/clinic/appointment/CancellationTest.java#L47), and [BookingTest](../src/test/java/com/clinic/appointment/BookingTest.java#L223); maintain context and identify file/test ID. Target ≤10 lines each.
- Fresh verification on 22 November 2026: 216 invocations passed; 0 failures, errors or skips. Designed cases: 115 from the existing catalogue. Source [Phase 1 evidence](phase-1-evidence.md).
- Historical failure: [DEF-01 failure](03_def01_failing.txt): 154 run, 1 failure; [regression](04_def01_fixed.txt): 154 run, 0 failures.
- Figures: test summary, failure and regression excerpt; source and execution date on each.
- Research: automated unit-testing benefits, JUnit Jupiter features [2].

## 6. Code Coverage and Defect Analysis — 1 page
| Fresh 22 November measurement | Covered / total |
|---|---|
| Instructions | 1648 / 1648 |
| Lines | 317 / 317 |
| Branches | 176 / 178 |
| Methods | 88 / 88 |
- Source: [Phase 1 evidence](phase-1-evidence.md); `Main` excluded. Coverage reports execution, not correctness [3].

| Defect | Detecting test | Root cause | Fix |
|---|---|---|---|
| DEF-01 | TC-B-16 | past scheduled records counted active | count only end > now |
| DEF-02 | TC-B-17 | locale-dependent ID digits | Locale.ROOT |
| DEF-03 | TC-P-12 | decomposed Unicode rejected | NFC normalization |
| DEF-04 | TC-E-01/02 regression | public mutators bypassed service | package-private creation/mutations |

- Historical mutation: 12/13 killed; TC-C-09 and TC-RS-14 added; 13/13 killed. Only selected mutants, not exhaustive.

## 7. Discussion and Conclusion — ½ page
- Strengths: traceability, boundary tests, recorded failing and passing regression runs.
- Limits: in-memory, single-user, no auth/holidays/checksum; no independent acceptance testing; shared AI assistance.
- Possible improvements: persistence/concurrency, per-doctor schedules, PIT.
- Student supplies truthful personal Java/testing challenges and learning experience; do not invent it.

## 8. References and AI disclosure — ¼ page
- [1] ISTQB, Certified Tester Foundation Level Syllabus 4.0.1: https://istqb.org/?download_id=3345&sdm_process_download=1
- [2] JUnit Team, JUnit 5.14.4 User Guide: https://docs.junit.org/5.14.4/overview.html
- [3] JaCoCo, Coverage Counters: https://www.jacoco.org/jacoco/trunk/doc/counters.html
- Follow one consistent citation style and cite references in text. Use the complete source list in [report.md](../docs/report.md#8-references) as needed.
- AI-use facts from README: Claude assisted initial code/tests/docs and audit; Codex assisted final build configuration, smoke test, reporting and packaging. Student confirms actual use and instructor policy; no claim of independent student authorship for AI-generated code.

## Research and final checks
- [ ] Role of testing in QA; unit-testing benefits; JUnit features; technique applicability; coverage limits; student's own learning challenges.
- [ ] All eight sections, references cited in text, AI disclosure, success and historical failure evidence.
- [ ] Relevant excerpts only; all numbers sourced; name and number on page 1; actual PDF ≤7 pages and correctly named.
- [ ] Instructor approval and AI-policy questions resolved; upload receipt obtained by student.

