"""Manual mutation analysis: apply one mutant at a time, run the suite, record killing tests, restore."""
import io
import os
import re
import subprocess
import sys

ROOT = sys.argv[1]
OUT = os.path.join(ROOT, "docs", "test-results", "mutation-analysis.txt")
POLICY = "src/main/java/com/clinic/appointment/TimeSlotPolicy.java"
SERVICE = "src/main/java/com/clinic/appointment/AppointmentService.java"
NL = "\n"

MUTANTS = [
    ("M01", "Last start 16:30 treated as outside hours", POLICY,
     "|| time.isAfter(ClinicRules.LAST_SLOT_START)", "|| !time.isBefore(ClinicRules.LAST_SLOT_START)"),
    ("M02", "Opening 09:00 treated as outside hours", POLICY,
     "time.isBefore(ClinicRules.OPENING_TIME)", "!time.isAfter(ClinicRules.OPENING_TIME)"),
    ("M03", "Lunch starts after 12:00 (12:00 bookable)", POLICY,
     "!time.isBefore(ClinicRules.LUNCH_START) && time.isBefore(ClinicRules.LUNCH_END)",
     "time.isAfter(ClinicRules.LUNCH_START) && time.isBefore(ClinicRules.LUNCH_END)"),
    ("M04", "Exactly 1 hour lead time rejected", POLICY,
     "start.isBefore(now.plusHours(ClinicRules.MIN_LEAD_HOURS))",
     "!start.isAfter(now.plusHours(ClinicRules.MIN_LEAD_HOURS))"),
    ("M05", "Advance limit off by one (31 days allowed)", POLICY,
     "plusDays(ClinicRules.MAX_ADVANCE_DAYS)", "plusDays(ClinicRules.MAX_ADVANCE_DAYS + 1)"),
    ("M06", "Seconds not checked in alignment", POLICY,
     "start.getSecond() != 0 || ", ""),
    ("M07", "Doctor daily limit uses > instead of >=", SERVICE,
     ">= doctor.getDailyLimit()) {" + NL + "            throw", "> doctor.getDailyLimit()) {" + NL + "            throw"),
    ("M08", "Patient active limit uses > instead of >=", SERVICE,
     "active >= ClinicRules.MAX_ACTIVE_APPOINTMENTS_PER_PATIENT", "active > ClinicRules.MAX_ACTIVE_APPOINTMENTS_PER_PATIENT"),
    ("M09", "Exactly 24 hours treated as late", SERVICE,
     "now.plusHours(ClinicRules.FREE_CANCELLATION_HOURS).isAfter(appointment.getStart())",
     "!now.plusHours(ClinicRules.FREE_CANCELLATION_HOURS).isBefore(appointment.getStart())"),
    ("M10", "Patient conflict check removed", SERVICE,
     "if (patientBusy) {", "if (false) {"),
    ("M11", "Reschedule does not exclude the original appointment", SERVICE,
     "checkAvailability(original.getPatient(), original.getDoctor(), newStart, original);",
     "checkAvailability(original.getPatient(), original.getDoctor(), newStart, null);"),
    ("M12", "DEF-01 fix removed (past appointments count as active)", SERVICE,
     "                .filter(a -> a.getEnd().isAfter(now))" + NL, ""),
    ("M13", "Status check removed from change policy", SERVICE,
     "        requireScheduled(appointment);" + NL + "        LocalDateTime now = now();",
     "        LocalDateTime now = now();"),
]

ENV = dict(os.environ, MAVEN_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT")
SUMMARY = re.compile(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: \d+\s*$", re.M)
FAILING = re.compile(r"^\[ERROR\]\s{2,}(\w+(?:\$\w+)?\.\w+)", re.M)


def run_suite():
    r = subprocess.run(["cmd", "/c", os.path.join(ROOT, "mvnw.cmd"), "-o", "test"], cwd=ROOT, env=ENV,
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = r.stdout + r.stderr
    totals = SUMMARY.findall(out)
    return r.returncode, out, (totals[-1] if totals else ("?", "?", "?"))


lines = ["Manual mutation analysis",
         "One mutant applied at a time; full suite run (mvnw -o test); source restored with git checkout.", ""]

code, _, totals = run_suite()
lines.append(f"Baseline (no mutant): exit code {code}, tests run {totals[0]}, failures {totals[1]}, errors {totals[2]}")
lines.append("")
if code != 0:
    sys.exit("baseline failed")

killed = 0
for mid, desc, path, old, new in MUTANTS:
    full = os.path.join(ROOT, path)
    src = io.open(full, encoding="utf-8", newline="").read()
    if "\r\n" in src:
        old, new = old.replace(NL, "\r\n"), new.replace(NL, "\r\n")
    count = src.count(old)
    if count != 1:
        lines.append(f"{mid} SKIPPED  pattern found {count} times: {desc}")
        print(f"{mid} SKIPPED ({count})", flush=True)
        continue
    io.open(full, "w", encoding="utf-8", newline="").write(src.replace(old, new))
    try:
        code, out, totals = run_suite()
        failing = sorted(set(FAILING.findall(out)))
        if "COMPILATION ERROR" in out:
            status = "COMPILE-ERROR"
        elif code != 0:
            status = "KILLED"
            killed += 1
        else:
            status = "SURVIVED"
        lines.append(f"{mid} {status:8} {desc}")
        lines.append(f"      {path}")
        lines.append(f"      '{old.strip()[:90]}'")
        lines.append(f"   -> '{new.strip()[:90]}'")
        lines.append(f"      tests run {totals[0]}, failures {totals[1]}, errors {totals[2]}")
        for f in failing[:6]:
            lines.append(f"      killed by {f}")
        if len(failing) > 6:
            lines.append(f"      ... and {len(failing) - 6} more test methods")
        lines.append("")
    finally:
        subprocess.run(["git", "checkout", "--", path], cwd=ROOT)
    print(f"{mid} {status}", flush=True)

lines.append(f"Killed {killed} of {len(MUTANTS)} mutants.")
io.open(OUT, "w", encoding="utf-8").write("\n".join(lines) + "\n")
print(f"Killed {killed} of {len(MUTANTS)}")
