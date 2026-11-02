# Assignment Deliverables and Evidence

Assignment basis: the four-page **SVV Research Assignment: Software Development and Automated Testing with JUnit** supplied as `h (2).pdf`.

| Required item | Completed artifact / evidence |
|---|---|
| Real-world problem and feasible individual scope | Report §1; [proposal](proposal.md) |
| Explicit testable functional requirements | Report §2; README FR-01–FR-10; test plan BR-01–BR-26 |
| Java implementation | `src/main/java`; Java 21; runnable JAR in ZIP `bin/` |
| JUnit 5 automated tests | `src/test/java`; 115 cases / 216 executions |
| Test plan based on requirements | [test-plan.md](test-plan.md), including complete FR/BR traceability |
| Equivalence partitioning | Test plan §3.1, EP tags and representative valid/invalid classes |
| Boundary value analysis | Test plan §3.2, BVA tags and inclusive/exclusive time and value limits |
| Decision table testing | Test plan §3.3; booking, cancellation, rescheduling and lifecycle tables |
| Negative testing | Rule-specific rejection and unchanged-state assertions; Negative tags |
| Regression testing | Historical runs 2–15; current clean builds 24–25 and isolated copy verification |
| Inputs, expected and actual test results | Test plan rule catalogue; annotated Java tests; [case results](test-results/test-case-results.md); [invocation results](test-results/final/invocation-results.md) |
| Successful and unsuccessful executions | Current successful build logs; original failing runs 2 and 5 |
| Defects identified and analysed | [defect-log.md](defect-log.md), DEF-01–DEF-04, historical reproduction and fix evidence |
| Measured code coverage report | [preserved JaCoCo HTML](test-results/final/coverage/index.html), XML and CSV; report §6 |
| Eight-section individual report | [PDF](report.pdf), [HTML](report.html), [Markdown](report.md) |
| Testing and QA research | Report §1 and §7 |
| Automated unit-testing benefits | Report §5 |
| JUnit 5 features | Report §5 |
| Test techniques and applicability | Report §4 and full test plan |
| Code coverage importance and limitations | Report §6 |
| Java and test implementation challenges | Report §7, grounded in recorded project issues |
| At least three credible, cited references | Six official technical/standards sources, cited in report text and listed in §8 |
| Build and configuration files | POM; Maven Wrapper scripts and `.mvn/wrapper/` |
| Acknowledgement of AI and external sources | Report §7 disclosure; README; citations |
| Complete supporting-material bundle | `submission/clinic-appointment-submission.zip`, with file hashes |

## Matters only the student or instructor can establish

The project files do not prove instructor approval before implementation. A scope proposal is included; attach the actual approval record if requested. AI-generated prose is disclosed, including the final report. The student must verify that this use meets the instructor's guidelines and be able to explain the work. The discussion covers evidenced project challenges without inventing a personal learning history; any first-person reflection must come from the student.

These are administrative and authorship responsibilities, not hidden software features or fabricated evidence. No assignment has been submitted to the instructor by this tool.

## Reproduction

```powershell
.\mvnw.cmd clean verify surefire-report:report-only
python scripts/test_case_report.py
java -jar target/clinic-appointment-1.0-SNAPSHOT.jar
```

For optional report exports, install `markdown` and `reportlab`, then run `python scripts/export_report.py`. Run `python scripts/package_submission.py` to preserve reports and recreate the ZIP. Core Java build and tests require no Python packages.
