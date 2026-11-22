"""Preserve current build evidence and create the complete assignment ZIP.

Run `mvnw clean verify surefire-report:report-only`, then
`python scripts/test_case_report.py` and `python scripts/export_report.py` first.
This script uses only Python's standard library.
"""
from pathlib import Path
from datetime import datetime, timezone
import hashlib
import json
import shutil
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
final = ROOT / 'docs/test-results/final'
jar = ROOT / 'target/clinic-appointment-1.0-SNAPSHOT.jar'
required = [jar, ROOT/'target/site/jacoco/index.html', ROOT/'target/reports/surefire.html',
            ROOT/'docs/report.pdf', ROOT/'docs/report.html', ROOT/'docs/report.md']
for path in required:
    if not path.is_file(): raise SystemExit(f'Missing required artifact: {path}')
reports = sorted((ROOT/'target/surefire-reports').glob('TEST-*.xml'))
if not reports: raise SystemExit('No test XML found')
totals = dict(tests=0, failures=0, errors=0, skipped=0)
cases = []
for path in reports:
    suite = ET.parse(path).getroot()
    for key in totals: totals[key] += int(suite.get(key, '0'))
    cases.extend(suite.findall('testcase'))
if not totals['tests'] or any(totals[k] for k in ('failures', 'errors', 'skipped')):
    raise SystemExit(f'Cannot package an incomplete or failing run: {totals}')
for source, destination in [('target/site/jacoco', 'coverage'), ('target/surefire-reports', 'surefire'), ('target/reports', 'test-report')]:
    shutil.copytree(ROOT/source, final/destination, dirs_exist_ok=True)
invocations = ['# Final test invocation results', '',
               'Actual execution names and outcomes from the final Surefire XML. Parameter values',
               'appear where the test display pattern includes them; full input fixtures and assertions',
               'are in the accompanying Java tests and the business-rule catalogue.', '',
               '| Class | Test invocation (input / expected behaviour) | Actual |', '|---|---|---|']
for case in sorted(cases, key=lambda c:(c.get('classname',''), c.get('name',''))):
    name = case.get('name','').replace('|','/').replace('\n',' ')
    invocations.append(f"| {case.get('classname','')} | {name} | PASS |")
(final/'invocation-results.md').write_text('\n'.join(invocations)+'\n', encoding='utf-8')
coverage = ET.parse(ROOT/'target/site/jacoco/jacoco.xml').getroot()
metrics = {c.get('type'): {'covered':int(c.get('covered')), 'missed':int(c.get('missed'))} for c in coverage.findall('counter')}
metadata = {'generated_utc':datetime.now(timezone.utc).isoformat(), 'tests':totals,
            'coverage_excludes':['com/clinic/Main*'], 'coverage':metrics,
            'source_sha256':{str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in sorted((ROOT/'src').rglob('*.java'))}}
(final/'verification.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')
output = ROOT/'submission'
output.mkdir(exist_ok=True)
archive = output/'clinic-appointment-submission.zip'
files = [ROOT/'pom.xml', ROOT/'README.md', ROOT/'mvnw', ROOT/'mvnw.cmd', ROOT/'.gitignore', ROOT/'.gitattributes']
for directory in ['src', '.mvn', 'docs', 'scripts']:
    files.extend(p for p in (ROOT/directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
manifest=[]
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for path in sorted(set(files)):
        relative = str(path.relative_to(ROOT)).replace('\\','/')
        z.write(path,'clinic-appointment/'+relative)
        manifest.append(hashlib.sha256(path.read_bytes()).hexdigest()+'  '+relative)
    z.write(jar,'clinic-appointment/bin/'+jar.name)
    manifest.append(hashlib.sha256(jar.read_bytes()).hexdigest()+'  bin/'+jar.name)
    z.writestr('clinic-appointment/SHA256SUMS.txt','\n'.join(manifest)+'\n')
with zipfile.ZipFile(archive) as z:
    if z.testzip() is not None: raise SystemExit('ZIP validation failed')
print(f"Packaged {archive} ({archive.stat().st_size:,} bytes); {totals['tests']} passing executions")
