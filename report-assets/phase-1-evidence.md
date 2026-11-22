# Phase 1 evidence — 22 November 2026

- Java: Eclipse Temurin 21.0.12.1, checked with `java -version`.
- Fresh command: Maven Wrapper `clean verify surefire-report:report-only`, using a writable temporary local Maven repository and Windows-ROOT certificate store. Result: BUILD SUCCESS.
- Surefire XML totals: **216 tests, 0 failures, 0 errors, 0 skipped**. Designed cases: 115 according to the existing case catalogue.
- Fresh JaCoCo XML totals: instructions 1648/1648, branches 176/178, lines 317/317, methods 88/88. `Main` is excluded in pom.xml. These match the preserved final-verification numbers.
- Current reports: `../target/site/jacoco/index.html`, `../target/reports/surefire.html`; XML files under `../target/surefire-reports` and `../target/site/jacoco/jacoco.xml`.
- Freshly built JAR ran successfully; console output: `demo-output-2026-11-22.txt`.
- `03_def01_failing.txt` and `04_def01_fixed.txt` are historical runs, 27 October 2026; they are not current failures.
- For screenshot 01, run the command above in a terminal and capture the `Tests run: 216, Failures: 0, Errors: 0, Skipped: 0` and BUILD SUCCESS lines. This run's counts are independently recorded in the Surefire XML; terminal output of the run was not retained in a screenshot.
- For screenshot 02, open `../target/site/jacoco/index.html` and capture the total row; label it 22 November 2026.
