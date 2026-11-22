# Study notes — Clinic Appointment System

Use these notes to learn the code; explain concepts in your own words. Links point to the source. Historical figures refer to recorded runs, not necessarily the latest execution.

1. **Limits.** [ClinicRules.java](../src/main/java/com/clinic/rules/ClinicRules.java#L13): 09:00–17:00 with 12:00–13:00 lunch; 30-minute slots, last start 16:30; 14 daily slots; age 0–120; name length 2–50; doctor limit 1–14; one-hour lead; 30 calendar days ahead; 24-hour free-change threshold; three active appointments per patient; notes 200 characters and five per appointment. Patient ID is 11 ASCII digits, first nonzero, defined in [Patient.java](../src/main/java/com/clinic/model/Patient.java#L16).
2. **Time-check order.** [TimeSlotPolicy.java](../src/main/java/com/clinic/appointment/TimeSlotPolicy.java#L31): null; slot alignment including seconds/nanoseconds; weekend; opening hours; lunch; one-hour lead; advance-date limit. It returns the first violation, so order determines the reported reason when several conditions fail.
3. **Booking.** [AppointmentService.java](../src/main/java/com/clinic/appointment/AppointmentService.java#L71): look up patient and doctor; validate start; check doctor/patient conflicts and capacity; create an appointment with the next ID; save and return it. Failure during checks does not consume an ID or store a booking.
4. **Changes.** [AppointmentService.java](../src/main/java/com/clinic/appointment/AppointmentService.java#L263): require requester and scheduled status; reject at or after start; at least 24h ahead either requester may change; less than 24h only staff may. Cancellation records the late flag; rescheduling validates the replacement and then changes the old record. Late staff rescheduling is not separately flagged.
5. **Lifecycle.** [AppointmentStatus.java](../src/main/java/com/clinic/appointment/AppointmentStatus.java#L10): `CANCELLED`, `RESCHEDULED`, `COMPLETED`, and `NO_SHOW` are final. `SCHEDULED`, `COMPLETED`, and `NO_SHOW` count against the doctor's daily limit.
6. **Clock.** [AppointmentService.java](../src/main/java/com/clinic/appointment/AppointmentService.java#L31) receives a `Clock` so tests can reproduce time boundaries. [MutableClock.java](../src/test/java/com/clinic/support/MutableClock.java#L13) lets tests advance the current time without waiting.
7. **Exceptions.** [ValidationException](../src/main/java/com/clinic/exception/ValidationException.java) represents invalid inputs and references; [SchedulingException](../src/main/java/com/clinic/exception/SchedulingException.java) represents a scheduling rule that prevents an operation. Both carry a `BusinessRule` through `ClinicException`.
8. **Defects.** DEF-01: ended but still scheduled visits blocked booking; TC-B-16; count only those ending after now. DEF-02: IDs depended on locale; TC-B-17; use `Locale.ROOT`. DEF-03: decomposed Unicode names rejected; TC-P-12; normalize NFC. DEF-04: callers bypassed rules via public mutators; TC-E-01/02; restrict constructor and mutators to package visibility. [Defect log](../docs/defect-log.md).
9. **Mutation.** Historical round 1 killed 12/13 selected mutants. The surviving status-check removal could cause the wrong rejection or consume an ID. TC-C-09 and TC-RS-14 strengthened assertions; round 2 killed 13/13. These 13 selected mutations are not exhaustive.
10. **Coverage.** Preserved report: two branches missed when `Patient.equals` and `Doctor.equals` compare a different class; `Main` excluded by POM because it is a demonstration, and separately tested by TC-DEMO-01. Coverage measures execution, not correctness. [Evidence](../docs/test-results/final-verification.md).

## Self-quiz — answer without looking

1. Why is exactly 24 hours treated differently from 23 hours 59 minutes?
2. Which checks run before a booking receives an ID?
3. Why can a past appointment still marked scheduled stop counting against the patient limit?
4. What did the surviving mutant reveal about checking only that an exception occurs?
5. Why are high coverage and 13 killed mutants not proof that the system is defect-free?
