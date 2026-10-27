# Defect Log

All defects below were found in this project's own code during development and testing, were
reproduced by a failing automated test **before** being fixed, and are visible in the git history.
No defect was injected on purpose. (The mutants in section 3 are deliberate, temporary changes used
to measure test effectiveness. They are **not** product defects and were never committed.)

## 1. Summary

| ID | Title | Severity | Component | Found by | Status |
|---|---|---|---|---|---|
| DEF-01 | Past unclosed appointments block the patient forever | High | `AppointmentService` (BR-18) | TC-B-16 (BVA + moving the clock) | Fixed, verified |
| DEF-02 | Appointment IDs depend on the JVM locale | Medium | `AppointmentService.nextId` | Code review, confirmed by probe, TC-B-17 | Fixed, verified |
| DEF-03 | Valid names in decomposed Unicode (NFD) rejected | Medium | `NameValidator` (BR-02) | Code review, confirmed by probe, TC-P-12 | Fixed, verified |
| DEF-04 | Business rules can be bypassed through returned `Appointment` objects | High | `Appointment` (encapsulation) | Audit, TC-E-01/02 (originally a failing call test) | Fixed, verified |

Severity scale: **High** – a business rule is not enforced or a user is blocked; **Medium** – wrong
result in a specific environment or for specific input; **Low** – cosmetic.

## 2. Details

### DEF-01 Past unclosed appointments block the patient forever

| Field | Value |
|---|---|
| Date | 2026-10-27 |
| Severity | High – a patient can be locked out of booking permanently |
| Affected component | `AppointmentService.checkAvailability` – active-appointment limit (BR-18, FR-04) |
| Reproduction steps | 1. Monday 07:00: book patient P1 at 09:00, 09:30, 10:00. 2. Move the clock to Tuesday 08:00 (all three visits are over, nobody marked them COMPLETED). 3. Book P1 for Tuesday 14:00. |
| Expected | Booking accepted – past appointments are not "active". |
| Actual | `SchedulingException: ... already has 3 active appointments` |
| Root cause | "Active" was implemented as `status == SCHEDULED`, ignoring time. Staff forgetting to close a visit is realistic. |
| Corrective action | Count only SCHEDULED appointments whose **end is in the future** (`a.getEnd().isAfter(now)`). |
| Regression test | TC-B-16 `pastScheduledAppointmentsAreNotActive` |
| Evidence | Failing: `docs/test-results/run-2-DEF-01-failing.txt` (commit `7ea4fe4`). Fix: commit `4b20a70`; full suite `run-3-regression-after-fix.txt` 154/154. |
| Final status | Closed |

### DEF-02 Appointment IDs depend on the JVM locale

| Field | Value |
|---|---|
| Date | 2026-10-28 |
| Severity | Medium – IDs differ between machines; lookups by the printed ID can fail |
| Affected component | `AppointmentService.nextId()` |
| Reproduction steps | 1. `Locale.setDefault(Locale.forLanguageTag("ar-EG"))`. 2. Book an appointment. 3. Read its ID. |
| Expected | `A0001` |
| Actual | `A٠٠٠١` (code points U+0041 U+0660 U+0660 U+0660 U+0661) – test output: `expected: <A0001> but was: <A????>` |
| Root cause | `String.format("A%04d", n)` uses the default locale, which may define non-ASCII digits. |
| Corrective action | `String.format(Locale.ROOT, "A%04d", n)` |
| Regression test | TC-B-17 `idsAreLocaleIndependent` (restores the original locale in `finally`) |
| Evidence | Probe program output in `docs/audit.md`; failing `run-5-DEF-02-03-04-failing.txt` (commit `708aaa6`); fix commit `0977400`; `run-6-after-DEF-02-fix.txt` |
| Final status | Closed |

### DEF-03 Valid names in decomposed Unicode (NFD) rejected

| Field | Value |
|---|---|
| Date | 2026-10-28 |
| Severity | Medium – Turkish patients/doctors can be refused depending on where the text was copied from |
| Affected component | `NameValidator.validate` (BR-02) |
| Reproduction steps | Create `new Patient("12345678901", Normalizer.normalize("Şükrü Çelik", NFD), 30)`. |
| Expected | Patient created, name stored as `Şükrü Çelik`. |
| Actual | `ValidationException: Patient name may only contain letters, spaces, hyphens and apostrophes` |
| Root cause | In NFD, `Ş` is `S` + U+0327 (combining cedilla). Combining marks are not in `\p{L}`, and the string is longer (5 → 8 code units for `Şükrü`). |
| Corrective action | Normalise to NFC (`Normalizer.normalize(name.trim(), NFC)`) before validating and storing. |
| Regression test | TC-P-12 `decomposedNameAccepted` |
| Evidence | Failing `run-5-…` (error), fix commit `27d1ab7`, `run-7-after-DEF-03-fix.txt` |
| Final status | Closed |

### DEF-04 Business rules can be bypassed through returned `Appointment` objects

| Field | Value |
|---|---|
| Date | 2026-10-28 |
| Severity | High – FR-06, FR-08 and FR-10 could be violated by any caller |
| Affected component | `Appointment` (was in `com.clinic.model` with public state-changing methods) |
| Reproduction steps | From a class outside the service package: `Appointment a = service.book(...); a.addNote("x".repeat(500)); a.complete();` |
| Expected | Both calls rejected (note > 200 chars; completion before start). |
| Actual | Both succeeded: `Expected ValidationException to be thrown, but nothing was thrown` / `Expected SchedulingException …` |
| Root cause | The service enforced the rules, but handed out objects whose `cancel/complete/markNoShow/markRescheduled/addNote` were `public`. |
| Corrective action | `Appointment`, `AppointmentStatus` and `AppointmentService` moved into `com.clinic.appointment`; the constructor and all state-changing methods became package-private; constructor rejects nulls. |
| Regression test | After the fix the reproduction code no longer compiles (that *is* the fix), so TC-E-01 checks that `Appointment` has only read methods publicly and TC-E-02 that it has no public constructor. TC-N-07 and TC-Q-11 check that returned lists are read-only. |
| Evidence | Failing `run-5-…`; still failing in `run-8-…` (only these 2 failures, proving the rules refactor caused no regression); fix commit `bf6a171`; `run-9-after-DEF-04-fix.txt` 173/173 |
| Final status | Closed |

## 3. Weaknesses found in the tests themselves (mutation analysis)

To check whether the passing tests would actually catch mistakes, 13 realistic one-line faults
(mutants) were applied one at a time, the full suite was run, and the code was restored
(`scripts/mutation_analysis.py`).

| Round | Result | Evidence |
|---|---|---|
| 1 | 12 / 13 killed. **M13 survived**: removing the status check from the cancellation/reschedule policy still produced a rejection (the model guard), so no test noticed. But the *reason* changed (e.g. BR-19 instead of BR-21 for a patient retrying a cancelled appointment), and a rejected reschedule consumed an appointment ID. | `docs/test-results/mutation-analysis-1.txt` |
| 2 | After adding TC-C-09 (reason must be BR-21) and TC-RS-14 (no ID consumed): **13 / 13 killed**. | `docs/test-results/mutation-analysis-2.txt` |

This was a gap in the test suite, not a product defect: the behaviour of the unmutated code was correct.

## 4. Problems in the tooling (not product defects)

| Problem | Effect | Resolution |
|---|---|---|
| Maven could not download artifacts: `PKIX path building failed` | Build failed on this PC (HTTPS inspection by security software). | `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (documented in README). |
| PowerShell 5.1 `Set-Content -Encoding utf8` wrote a UTF-8 BOM into a Java file | Compilation error "class, interface, enum, or record expected". | BOM removed; files are edited without PowerShell. |
| First mutation-analysis attempt: `cmd` could not find `mvnw.cmd` by relative name | Every mutant appeared "killed" because the build never ran. Detected by adding a **baseline run** that must pass; results discarded. | Absolute path to `mvnw.cmd`; baseline check kept in the script. |
| Stale Surefire reports from the old `com.clinic.service` package remained in `target/` | Reports would show tests that no longer exist. | Reports are always generated with `mvnw clean …`. |
