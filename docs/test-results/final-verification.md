# Final Verification — 2 November 2026

## Build and execution

The current source was built using Maven Wrapper 3.9.9 and Temurin JDK 21.0.12.1.

```powershell
.\mvnw.cmd clean verify surefire-report:report-only
java -jar target/clinic-appointment-1.0-SNAPSHOT.jar
python scripts/test_case_report.py
```

The agent's environment needed a writable temporary Maven repository (`-Dmaven.repo.local=<TEMP>/svv-maven-repository`) and normal Java filesystem access outside its sandbox. Windows certificate trust was selected with `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Dfile.encoding=UTF-8`. Those are execution-environment settings, not disabled TLS verification or application requirements.

Successful evidence: `run-24-final-verification.txt` and `run-25-final-verification.txt`. Run 25 also generates the HTML test report. `run-26-clean-copy-verification.txt` confirms a second clean build of only the final source, POM and Wrapper copied to an empty temporary directory: 216 executions passed, offline using cached dependencies. Earlier finalization attempts exposed repository permissions, TLS trust and a sandbox `AccessDeniedException` when Java closed ZIP files; these are tooling failures, not failing application assertions. They are retained under `tooling/`.

## Measured results

| Measure | Final result |
|---|---|
| Designed test cases | 115 |
| JUnit executions | 216 |
| Passed / failures / errors / skipped | 216 / 0 / 0 / 0 |
| Instructions | 1648/1648 = 100% |
| Lines | 317/317 = 100% |
| Branches | 176/178 = 98.88% |
| Methods | 88/88 = 100% |
| Core appointment instructions / lines / branches / methods | 1093/1093; 219/219; 140/140; 58/58 — all 100% |
| Exclusion | `com/clinic/Main*`, explicitly configured; tested separately by TC-DEMO-01 |
| Remaining uncovered branches | Comparison with a different object type in `Patient.equals` and `Doctor.equals` |
| Packaged application | Runnable JAR completes the demo; `demo-output-final.txt` |
| Historical mutation analysis | 13 selected mutants; round 1: 12 killed; round 2: 13 killed; not rerun as part of this finalization |

Current versions: JUnit 5.14.4, Surefire/report plugin 3.6.1, JaCoCo 0.8.16, compiler plugin 3.13.0, JAR plugin 3.4.1, exec plugin 3.5.0. The report generator refuses missing test IDs, skipped executions and mismatches between the source catalogue and XML results.

## Preserved artifacts

- `final/coverage/index.html`, complete report assets, XML and CSV.
- `final/test-report/surefire.html`, with supporting assets.
- `final/surefire/TEST-*.xml` and text reports.
- `test-case-results.md`: all 115 case outcomes.
- `final/invocation-results.md`: all 216 execution names and outcomes.
- `final/verification.json`: totals, coverage counters and SHA-256 hashes of Java sources.
- `submission/clinic-appointment-submission.zip`: source, build files, report, evidence and runnable JAR, with `SHA256SUMS.txt`.

The final PDF/HTML/Markdown report replaces historical report-support material. Instructor approval and personal learning claims are not fabricated; see the submission checklist.
