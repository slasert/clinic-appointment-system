# Final test invocation results

Actual execution names and outcomes from the final Surefire XML. Parameter values
appear where the test display pattern includes them; full input fixtures and assertions
are in the accompanying Java tests and the business-rule catalogue.

| Class | Test invocation (input / expected behaviour) | Actual |
|---|---|---|
| com.clinic.EncapsulationTest | TC-E-01 Appointment exposes only read methods publicly | PASS |
| com.clinic.EncapsulationTest | TC-E-02 Appointment has no public constructor | PASS |
| com.clinic.SmokeTest | TC-DEMO-01 console demo completes and reports conflicts and cancellation outcomes | PASS |
| com.clinic.appointment.AppointmentTest | TC-A-01 once a status is final, no further status change is possible | PASS |
| com.clinic.appointment.AppointmentTest | TC-A-02 an appointment cannot be created with a missing id, patient, doctor or start | PASS |
| com.clinic.appointment.AppointmentTest | TC-A-03 end time and text form are derived from the start | PASS |
| com.clinic.appointment.BookingTest | TC-B-01 valid request creates a SCHEDULED appointment | PASS |
| com.clinic.appointment.BookingTest | TC-B-02 appointment IDs are unique and sequential | PASS |
| com.clinic.appointment.BookingTest | TC-B-08 two different doctors can see two patients at the same time | PASS |
| com.clinic.appointment.BookingTest | TC-B-09 a cancelled slot becomes free again | PASS |
| com.clinic.appointment.BookingTest | TC-B-10 cancelled appointments do not count towards the doctor's daily limit | PASS |
| com.clinic.appointment.BookingTest | TC-B-11 cancelled appointments do not count towards the patient's active limit | PASS |
| com.clinic.appointment.BookingTest | TC-B-12 the doctor limit is per day: a full day does not block the next day | PASS |
| com.clinic.appointment.BookingTest | TC-B-13 a failed booking does not change schedules, history or the next ID | PASS |
| com.clinic.appointment.BookingTest | TC-B-16 [DEF-01] past appointments that were never closed do not count as active | PASS |
| com.clinic.appointment.BookingTest | TC-B-17 [DEF-02] appointment IDs do not depend on the default locale | PASS |
| com.clinic.appointment.BookingTest | TC-B-18 doctor back-to-back: slots directly before and after a booked slot are free | PASS |
| com.clinic.appointment.BookingTest | TC-B-19 patient back-to-back with two doctors is allowed | PASS |
| com.clinic.appointment.BookingTest | unknownDoctorRejected(String) TC-B-15 unknown doctor ID "" is rejected | PASS |
| com.clinic.appointment.BookingTest | unknownDoctorRejected(String) TC-B-15 unknown doctor ID "D99" is rejected | PASS |
| com.clinic.appointment.BookingTest | unknownDoctorRejected(String) TC-B-15 unknown doctor ID "d1" is rejected | PASS |
| com.clinic.appointment.BookingTest | unknownDoctorRejected(String) TC-B-15 unknown doctor ID "null" is rejected | PASS |
| com.clinic.appointment.BookingTest | unknownPatientRejected(String) TC-B-14 unknown patient ID "" is rejected | PASS |
| com.clinic.appointment.BookingTest | unknownPatientRejected(String) TC-B-14 unknown patient ID "99999999999" is rejected | PASS |
| com.clinic.appointment.BookingTest | unknownPatientRejected(String) TC-B-14 unknown patient ID "null" is rejected | PASS |
| com.clinic.appointment.BookingTest$DecisionTable | TC-B-03 R1 all conditions true -> booked | PASS |
| com.clinic.appointment.BookingTest$DecisionTable | TC-B-04 R2 doctor already booked at that time -> rejected (double booking) | PASS |
| com.clinic.appointment.BookingTest$DecisionTable | TC-B-05 R3 patient already has another doctor at that time -> rejected | PASS |
| com.clinic.appointment.BookingTest$DecisionTable | TC-B-06 R4 doctor daily limit (2): 2nd booking accepted, 3rd rejected | PASS |
| com.clinic.appointment.BookingTest$DecisionTable | TC-B-07 R5 patient active limit (3): 3rd booking accepted, 4th rejected | PASS |
| com.clinic.appointment.BookingTest$Registration | TC-R-01 registering the same patient ID twice is rejected; the first patient is kept | PASS |
| com.clinic.appointment.BookingTest$Registration | TC-R-02 registering the same doctor ID twice is rejected; the first doctor is kept | PASS |
| com.clinic.appointment.BookingTest$Registration | TC-R-03 null patient/doctor is rejected | PASS |
| com.clinic.appointment.BookingTest$Registration | TC-R-04 service cannot be created without a clock | PASS |
| com.clinic.appointment.CancellationTest | TC-C-04 R6 an already cancelled appointment cannot be cancelled again | PASS |
| com.clinic.appointment.CancellationTest | TC-C-05 null requester is rejected and the appointment stays SCHEDULED | PASS |
| com.clinic.appointment.CancellationTest | TC-C-06 unknown or null appointment ID is rejected | PASS |
| com.clinic.appointment.CancellationTest | TC-C-07 a normal cancellation is not flagged as late | PASS |
| com.clinic.appointment.CancellationTest | TC-C-08 a cancelled appointment leaves the doctor's schedule but stays in the patient history | PASS |
| com.clinic.appointment.CancellationTest | allowedCancellations(Requester, Duration, boolean) TC-C-01 PATIENT cancels PT24H before start -> CANCELLED, late=false | PASS |
| com.clinic.appointment.CancellationTest | allowedCancellations(Requester, Duration, boolean) TC-C-01 PATIENT cancels PT26H before start -> CANCELLED, late=false | PASS |
| com.clinic.appointment.CancellationTest | allowedCancellations(Requester, Duration, boolean) TC-C-01 STAFF cancels PT1M before start -> CANCELLED, late=true | PASS |
| com.clinic.appointment.CancellationTest | allowedCancellations(Requester, Duration, boolean) TC-C-01 STAFF cancels PT23H59M before start -> CANCELLED, late=true | PASS |
| com.clinic.appointment.CancellationTest | allowedCancellations(Requester, Duration, boolean) TC-C-01 STAFF cancels PT24H before start -> CANCELLED, late=false | PASS |
| com.clinic.appointment.CancellationTest | cancelAfterStartRejected(Requester) TC-C-03 PATIENT cancels at/after start -> rejected | PASS |
| com.clinic.appointment.CancellationTest | cancelAfterStartRejected(Requester) TC-C-03 STAFF cancels at/after start -> rejected | PASS |
| com.clinic.appointment.CancellationTest | patientLateCancellationRejected(Duration) TC-C-02 PATIENT cancels PT1M before start -> rejected | PASS |
| com.clinic.appointment.CancellationTest | patientLateCancellationRejected(Duration) TC-C-02 PATIENT cancels PT23H59M before start -> rejected | PASS |
| com.clinic.appointment.CancellationTest | patientLateCancellationRejected(Duration) TC-C-02 PATIENT cancels PT2H before start -> rejected | PASS |
| com.clinic.appointment.CancellationTest | statusIsReportedBeforeTimeRules(Requester, Duration) TC-C-09 cancelled appointment, PATIENT retries PT2H before start -> reason is BR-21, not a time rule | PASS |
| com.clinic.appointment.CancellationTest | statusIsReportedBeforeTimeRules(Requester, Duration) TC-C-09 cancelled appointment, STAFF retries PT-1H before start -> reason is BR-21, not a time rule | PASS |
| com.clinic.appointment.NoteTest | TC-N-01 a valid note is stored trimmed | PASS |
| com.clinic.appointment.NoteTest | TC-N-03 note of 201 characters is rejected | PASS |
| com.clinic.appointment.NoteTest | TC-N-05 5th note is accepted, 6th is rejected | PASS |
| com.clinic.appointment.NoteTest | TC-N-06 a note can be added to a cancelled appointment to record the reason | PASS |
| com.clinic.appointment.NoteTest | TC-N-07 the notes list cannot be modified from outside | PASS |
| com.clinic.appointment.NoteTest | TC-N-08 note for an unknown or null appointment is rejected | PASS |
| com.clinic.appointment.NoteTest | emptyNoteRejected(String) TC-N-04 empty note "  " is rejected | PASS |
| com.clinic.appointment.NoteTest | emptyNoteRejected(String) TC-N-04 empty note "   " is rejected | PASS |
| com.clinic.appointment.NoteTest | emptyNoteRejected(String) TC-N-04 empty note "" is rejected | PASS |
| com.clinic.appointment.NoteTest | emptyNoteRejected(String) TC-N-04 empty note "null" is rejected | PASS |
| com.clinic.appointment.NoteTest | noteLengthBoundariesAccepted(int) TC-N-02 note of length 1 is accepted | PASS |
| com.clinic.appointment.NoteTest | noteLengthBoundariesAccepted(int) TC-N-02 note of length 199 is accepted | PASS |
| com.clinic.appointment.NoteTest | noteLengthBoundariesAccepted(int) TC-N-02 note of length 2 is accepted | PASS |
| com.clinic.appointment.NoteTest | noteLengthBoundariesAccepted(int) TC-N-02 note of length 200 is accepted | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-01 R1 successful reschedule: old RESCHEDULED, new SCHEDULED with same patient/doctor | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-02 the old slot becomes free after rescheduling | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-03 R2 target slot busy -> rejected and original stays SCHEDULED (atomicity) | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-04 R3 invalid target time (lunch) -> rejected and original unchanged | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-05 R4 rescheduling to the same time is rejected | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-06 null target time or null requester is rejected | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-07 patient at the active limit (3) can still move one of them | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-08 doctor at the daily limit can move an appointment within the same day | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-09 R5 patient cannot reschedule less than 24h before, staff can | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-10 R7 a rescheduled appointment cannot be rescheduled again | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-12 target slot where the patient already sees another doctor -> rejected | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-13 doctor at the daily limit cannot receive an appointment moved from another day | PASS |
| com.clinic.appointment.RescheduleTest | TC-RS-14 rescheduling a cancelled appointment is rejected and uses no appointment ID | PASS |
| com.clinic.appointment.RescheduleTest | rescheduleAfterStartRejected(Requester) TC-RS-11 R6 PATIENT cannot reschedule after the start | PASS |
| com.clinic.appointment.RescheduleTest | rescheduleAfterStartRejected(Requester) TC-RS-11 R6 STAFF cannot reschedule after the start | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-01 an empty working day has 14 free slots: 09:00..11:30 and 13:00..16:30 | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-02 booked slots are not offered, cancelled ones are offered again | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-03 no free slots on a weekend or beyond 30 days | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-04 today only offers slots at least 1 hour ahead (now 10:00 -> first slot 11:00) | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-05 a doctor at the daily limit has no free slots | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-06 every offered slot can actually be booked | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-07 doctor schedule is sorted by time and excludes cancelled appointments | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-08 patient history contains every status, sorted by time | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-09 queries with unknown IDs or a null date are rejected | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-10 a past date has no free slots | PASS |
| com.clinic.appointment.ScheduleQueryTest | TC-Q-11 returned lists are read-only, so callers cannot corrupt the schedule | PASS |
| com.clinic.appointment.SlotValidationTest | TC-S-05 start time with one second or one nanosecond is rejected | PASS |
| com.clinic.appointment.SlotValidationTest | TC-S-06 null start time is rejected | PASS |
| com.clinic.appointment.SlotValidationTest | TC-S-11 a date in the past is rejected | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryAccepted(LocalTime) TC-S-09 booking 09:00 when now is 07:00 -> accepted | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryAccepted(LocalTime) TC-S-09 booking 09:00 when now is 07:59 -> accepted | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryAccepted(LocalTime) TC-S-09 booking 09:00 when now is 08:00 -> accepted | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryRejected(LocalTime) TC-S-10 booking 09:00 when now is 08:01 -> rejected | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryRejected(LocalTime) TC-S-10 booking 09:00 when now is 08:59 -> rejected | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryRejected(LocalTime) TC-S-10 booking 09:00 when now is 09:00 -> rejected | PASS |
| com.clinic.appointment.SlotValidationTest | leadTimeBoundaryRejected(LocalTime) TC-S-10 booking 09:00 when now is 10:00 -> rejected | PASS |
| com.clinic.appointment.SlotValidationTest | lunchBreakRejected(LocalTime) TC-S-03 start 12:00 falls in the lunch break | PASS |
| com.clinic.appointment.SlotValidationTest | lunchBreakRejected(LocalTime) TC-S-03 start 12:30 falls in the lunch break | PASS |
| com.clinic.appointment.SlotValidationTest | maxAdvanceBoundaryAccepted(LocalDate, int) TC-S-12 booking on 2026-12-01 (today + 29 days) -> accepted | PASS |
| com.clinic.appointment.SlotValidationTest | maxAdvanceBoundaryAccepted(LocalDate, int) TC-S-12 booking on 2026-12-02 (today + 30 days) -> accepted | PASS |
| com.clinic.appointment.SlotValidationTest | maxAdvanceBoundaryRejected(LocalDate, int) TC-S-13 booking on 2026-12-03 (today + 31 days) -> rejected | PASS |
| com.clinic.appointment.SlotValidationTest | maxAdvanceBoundaryRejected(LocalDate, int) TC-S-13 booking on 2027-11-02 (today + 365 days) -> rejected | PASS |
| com.clinic.appointment.SlotValidationTest | misalignedMinutesRejected(LocalTime) TC-S-04 start 09:01 is not aligned to a 30 minute slot | PASS |
| com.clinic.appointment.SlotValidationTest | misalignedMinutesRejected(LocalTime) TC-S-04 start 09:15 is not aligned to a 30 minute slot | PASS |
| com.clinic.appointment.SlotValidationTest | misalignedMinutesRejected(LocalTime) TC-S-04 start 09:29 is not aligned to a 30 minute slot | PASS |
| com.clinic.appointment.SlotValidationTest | misalignedMinutesRejected(LocalTime) TC-S-04 start 09:31 is not aligned to a 30 minute slot | PASS |
| com.clinic.appointment.SlotValidationTest | misalignedMinutesRejected(LocalTime) TC-S-04 start 16:59 is not aligned to a 30 minute slot | PASS |
| com.clinic.appointment.SlotValidationTest | outsideWorkingHoursRejected(LocalTime) TC-S-02 start 00:00 is outside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | outsideWorkingHoursRejected(LocalTime) TC-S-02 start 08:00 is outside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | outsideWorkingHoursRejected(LocalTime) TC-S-02 start 08:30 is outside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | outsideWorkingHoursRejected(LocalTime) TC-S-02 start 17:00 is outside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | outsideWorkingHoursRejected(LocalTime) TC-S-02 start 17:30 is outside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | outsideWorkingHoursRejected(LocalTime) TC-S-02 start 23:30 is outside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | validWorkingHours(LocalTime) TC-S-01 start 09:00 is inside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | validWorkingHours(LocalTime) TC-S-01 start 09:30 is inside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | validWorkingHours(LocalTime) TC-S-01 start 11:30 is inside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | validWorkingHours(LocalTime) TC-S-01 start 13:00 is inside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | validWorkingHours(LocalTime) TC-S-01 start 16:00 is inside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | validWorkingHours(LocalTime) TC-S-01 start 16:30 is inside working hours | PASS |
| com.clinic.appointment.SlotValidationTest | weekdayAccepted(LocalDate, String) TC-S-07 2026-11-06 (Friday) is a working day | PASS |
| com.clinic.appointment.SlotValidationTest | weekdayAccepted(LocalDate, String) TC-S-07 2026-11-09 (Monday) is a working day | PASS |
| com.clinic.appointment.SlotValidationTest | weekendRejected(LocalDate, String) TC-S-08 2026-11-07 (Saturday) is a weekend | PASS |
| com.clinic.appointment.SlotValidationTest | weekendRejected(LocalDate, String) TC-S-08 2026-11-08 (Sunday) is a weekend | PASS |
| com.clinic.appointment.StatusTransitionTest | TC-ST-01 complete one minute before start -> rejected | PASS |
| com.clinic.appointment.StatusTransitionTest | TC-ST-02 complete exactly at start -> COMPLETED | PASS |
| com.clinic.appointment.StatusTransitionTest | TC-ST-03 no-show one minute before the slot ends -> rejected | PASS |
| com.clinic.appointment.StatusTransitionTest | TC-ST-04 no-show exactly when the slot ends -> NO_SHOW | PASS |
| com.clinic.appointment.StatusTransitionTest | TC-ST-07 completed and no-show appointments still count towards the doctor's daily limit | PASS |
| com.clinic.appointment.StatusTransitionTest | TC-ST-10 completing long after the slot (next day) is still allowed | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from CANCELLED: CANCEL -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from CANCELLED: COMPLETE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from CANCELLED: NO_SHOW -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from CANCELLED: RESCHEDULE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from COMPLETED: CANCEL -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from COMPLETED: COMPLETE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from COMPLETED: NO_SHOW -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from COMPLETED: RESCHEDULE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from NO_SHOW: CANCEL -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from NO_SHOW: COMPLETE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from NO_SHOW: NO_SHOW -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from NO_SHOW: RESCHEDULE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from RESCHEDULED: CANCEL -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from RESCHEDULED: COMPLETE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from RESCHEDULED: NO_SHOW -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from RESCHEDULED: RESCHEDULE -> REJECTED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from SCHEDULED: CANCEL -> CANCELLED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from SCHEDULED: COMPLETE -> COMPLETED | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from SCHEDULED: NO_SHOW -> NO_SHOW | PASS |
| com.clinic.appointment.StatusTransitionTest | transitionTable(AppointmentStatus, String, String) TC-ST-09 from SCHEDULED: RESCHEDULE -> RESCHEDULED | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | TC-TP-02 a valid start has no violation | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | TC-TP-03 malformed input -> ValidationException, rule violation -> SchedulingException | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | TC-TP-04 the daily grid has 14 slots, 09:00-11:30 and 13:00-16:30, none in lunch | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | rulesAreCheckedInFixedOrder(LocalDateTime, BusinessRule) TC-TP-01 2026-11-02T08:30 breaks several rules -> first reported is WORKING_HOURS | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | rulesAreCheckedInFixedOrder(LocalDateTime, BusinessRule) TC-TP-01 2026-11-07T12:00 breaks several rules -> first reported is WORKING_DAY | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | rulesAreCheckedInFixedOrder(LocalDateTime, BusinessRule) TC-TP-01 2026-11-07T12:15 breaks several rules -> first reported is SLOT_ALIGNMENT | PASS |
| com.clinic.appointment.TimeSlotPolicyTest | rulesAreCheckedInFixedOrder(LocalDateTime, BusinessRule) TC-TP-01 2026-12-31T12:00 breaks several rules -> first reported is LUNCH_BREAK | PASS |
| com.clinic.model.DoctorTest | TC-D-01 valid doctor is created with trimmed ID and specialty | PASS |
| com.clinic.model.DoctorTest | TC-D-07 doctors with the same ID are equal | PASS |
| com.clinic.model.DoctorTest | dailyLimitBoundariesAccepted(int) TC-D-02 daily limit 1 is accepted | PASS |
| com.clinic.model.DoctorTest | dailyLimitBoundariesAccepted(int) TC-D-02 daily limit 13 is accepted | PASS |
| com.clinic.model.DoctorTest | dailyLimitBoundariesAccepted(int) TC-D-02 daily limit 14 is accepted | PASS |
| com.clinic.model.DoctorTest | dailyLimitBoundariesAccepted(int) TC-D-02 daily limit 2 is accepted | PASS |
| com.clinic.model.DoctorTest | dailyLimitOutsideBoundariesRejected(int) TC-D-03 daily limit -1 is rejected | PASS |
| com.clinic.model.DoctorTest | dailyLimitOutsideBoundariesRejected(int) TC-D-03 daily limit 0 is rejected | PASS |
| com.clinic.model.DoctorTest | dailyLimitOutsideBoundariesRejected(int) TC-D-03 daily limit 15 is rejected | PASS |
| com.clinic.model.DoctorTest | emptyIdRejected(String) TC-D-04 empty doctor ID "  " is rejected | PASS |
| com.clinic.model.DoctorTest | emptyIdRejected(String) TC-D-04 empty doctor ID "" is rejected | PASS |
| com.clinic.model.DoctorTest | emptyIdRejected(String) TC-D-04 empty doctor ID "null" is rejected | PASS |
| com.clinic.model.DoctorTest | emptySpecialtyRejected(String) TC-D-05 empty specialty "  " is rejected | PASS |
| com.clinic.model.DoctorTest | emptySpecialtyRejected(String) TC-D-05 empty specialty "" is rejected | PASS |
| com.clinic.model.DoctorTest | emptySpecialtyRejected(String) TC-D-05 empty specialty "null" is rejected | PASS |
| com.clinic.model.DoctorTest | invalidNameRejected(String) TC-D-06 invalid doctor name "" is rejected | PASS |
| com.clinic.model.DoctorTest | invalidNameRejected(String) TC-D-06 invalid doctor name "A" is rejected | PASS |
| com.clinic.model.DoctorTest | invalidNameRejected(String) TC-D-06 invalid doctor name "Ayşe2" is rejected | PASS |
| com.clinic.model.DoctorTest | invalidNameRejected(String) TC-D-06 invalid doctor name "Dr. Ayşe" is rejected | PASS |
| com.clinic.model.DoctorTest | invalidNameRejected(String) TC-D-06 invalid doctor name "null" is rejected | PASS |
| com.clinic.model.PatientTest | TC-P-01 valid patient is created and the name is trimmed | PASS |
| com.clinic.model.PatientTest | TC-P-02 Turkish characters, hyphen and apostrophe are accepted in names | PASS |
| com.clinic.model.PatientTest | TC-P-09 surrounding spaces do not count towards the 50 character limit | PASS |
| com.clinic.model.PatientTest | TC-P-11 patients with the same ID are equal | PASS |
| com.clinic.model.PatientTest | TC-P-12 [DEF-03] a name in decomposed Unicode (NFD) is accepted and stored composed (NFC) | PASS |
| com.clinic.model.PatientTest | TC-P-13 the national ID is masked in text output | PASS |
| com.clinic.model.PatientTest | ageBoundariesAccepted(int) TC-P-05 age 0 is accepted | PASS |
| com.clinic.model.PatientTest | ageBoundariesAccepted(int) TC-P-05 age 1 is accepted | PASS |
| com.clinic.model.PatientTest | ageBoundariesAccepted(int) TC-P-05 age 119 is accepted | PASS |
| com.clinic.model.PatientTest | ageBoundariesAccepted(int) TC-P-05 age 120 is accepted | PASS |
| com.clinic.model.PatientTest | ageOutsideBoundariesRejected(int) TC-P-06 age -1 is rejected | PASS |
| com.clinic.model.PatientTest | ageOutsideBoundariesRejected(int) TC-P-06 age -2147483648 is rejected | PASS |
| com.clinic.model.PatientTest | ageOutsideBoundariesRejected(int) TC-P-06 age 121 is rejected | PASS |
| com.clinic.model.PatientTest | ageOutsideBoundariesRejected(int) TC-P-06 age 2147483647 is rejected | PASS |
| com.clinic.model.PatientTest | invalidIdRejected(String) TC-P-03 invalid ID "-1234567890" is rejected | PASS |
| com.clinic.model.PatientTest | invalidIdRejected(String) TC-P-03 invalid ID "01234567890" is rejected | PASS |
| com.clinic.model.PatientTest | invalidIdRejected(String) TC-P-03 invalid ID "12345 78901" is rejected | PASS |
| com.clinic.model.PatientTest | invalidIdRejected(String) TC-P-03 invalid ID "1234567890" is rejected | PASS |
| com.clinic.model.PatientTest | invalidIdRejected(String) TC-P-03 invalid ID "123456789012" is rejected | PASS |
| com.clinic.model.PatientTest | invalidIdRejected(String) TC-P-03 invalid ID "1234567890A" is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "   " is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "" is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "'Ali" is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "-Ali" is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "Ali@Veli" is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "Mehmet3" is rejected | PASS |
| com.clinic.model.PatientTest | invalidNameRejected(String) TC-P-10 invalid name "null" is rejected | PASS |
| com.clinic.model.PatientTest | nameLengthBoundariesAccepted(int) TC-P-07 name of length 2 is accepted | PASS |
| com.clinic.model.PatientTest | nameLengthBoundariesAccepted(int) TC-P-07 name of length 3 is accepted | PASS |
| com.clinic.model.PatientTest | nameLengthBoundariesAccepted(int) TC-P-07 name of length 49 is accepted | PASS |
| com.clinic.model.PatientTest | nameLengthBoundariesAccepted(int) TC-P-07 name of length 50 is accepted | PASS |
| com.clinic.model.PatientTest | nameLengthOutsideBoundariesRejected(int) TC-P-08 name of length 1 is rejected | PASS |
| com.clinic.model.PatientTest | nameLengthOutsideBoundariesRejected(int) TC-P-08 name of length 51 is rejected | PASS |
| com.clinic.model.PatientTest | nullOrEmptyIdRejected(String) TC-P-04 null/empty ID "" is rejected | PASS |
| com.clinic.model.PatientTest | nullOrEmptyIdRejected(String) TC-P-04 null/empty ID "null" is rejected | PASS |
