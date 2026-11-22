"""Export the report as HTML and PDF. Optional dependencies: markdown, reportlab.

Install with: python -m pip install markdown reportlab
Run from any directory: python scripts/export_report.py
"""
from pathlib import Path
import os
import re
import xml.etree.ElementTree as ET

import markdown
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Preformatted

ROOT = Path(__file__).resolve().parents[1]
body = markdown.markdown((ROOT / 'docs/report.md').read_text(encoding='utf-8'), extensions=['tables', 'fenced_code'])
css = """body{font:11pt/1.55 Arial,sans-serif;max-width:950px;margin:40px auto;color:#172133;padding:0 24px}
h1,h2{color:#14345a}h2{border-bottom:1px solid #ccd5df;padding-top:18px}table{border-collapse:collapse;width:100%;font-size:10pt}
td,th{border:1px solid #ccd5df;padding:7px;text-align:left;vertical-align:top}th{background:#edf2f7}
pre{background:#edf2f7;padding:12px;white-space:pre-wrap}a{color:#19578a}@media print{body{margin:0;max-width:none}h2{page-break-after:avoid}tr{page-break-inside:avoid}}"""
(ROOT / 'docs/report.html').write_text('<!doctype html><html lang="en"><meta charset="utf-8"><title>SVV Clinic Report</title><style>'+css+'</style><body>'+body+'</body></html>', encoding='utf-8')
fontdir = Path(os.environ.get('WINDIR', 'C:/Windows')) / 'Fonts'
fallback = Path('/usr/share/fonts/truetype/dejavu')
for name, windows, linux in [('Body', 'arial.ttf', 'DejaVuSans.ttf'), ('Body-Bold', 'arialbd.ttf', 'DejaVuSans-Bold.ttf'), ('Body-Italic', 'ariali.ttf', 'DejaVuSans-Oblique.ttf'), ('Body-BoldItalic', 'arialbi.ttf', 'DejaVuSans-BoldOblique.ttf')]:
    font = fontdir / windows
    if not font.exists(): font = fallback / linux
    pdfmetrics.registerFont(TTFont(name, str(font)))
pdfmetrics.registerFontFamily('Body', normal='Body', bold='Body-Bold', italic='Body-Italic', boldItalic='Body-BoldItalic')
styles = getSampleStyleSheet()
for style in styles.byName.values():
    style.fontName = 'Body'
styles['BodyText'].fontSize = 9.5
styles['BodyText'].leading = 14
styles['BodyText'].spaceAfter = 7
cell_style = ParagraphStyle('Cell', parent=styles['BodyText'], fontSize=8, leading=11, spaceAfter=0, alignment=TA_LEFT)
story = []

def inner(el):
    result = (el.text or '') + ''.join(ET.tostring(c, encoding='unicode', method='html') for c in el)
    result = result.replace('<code>', '<font face="Courier">').replace('</code>', '</font>')
    result = re.sub(r'<br\s*/?>', '<br/>', result)
    return result

for el in ET.fromstring('<root>'+body.replace('<br>', '<br/>')+'</root>'):
    if el.tag in ('h1', 'h2', 'h3'):
        story.append(Paragraph(inner(el), styles[{'h1':'Title', 'h2':'Heading1', 'h3':'Heading2'}[el.tag]]))
    elif el.tag == 'p':
        story.append(Paragraph(inner(el), styles['BodyText']))
    elif el.tag == 'table':
        rows = [[Paragraph(inner(c), cell_style) for c in row] for row in el.findall('.//tr')]
        count = len(rows[0])
        widths = [80, 435] if count == 2 else ([45, 115, 100, 75, 180] if count == 5 else [515/count]*count)
        if count == 5 and len(rows[0]) == 5 and 'Scope' in ''.join(el.itertext()): widths = [155,90,90,90,90]
        table = Table(rows, colWidths=widths, repeatRows=1, hAlign='LEFT')
        table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#edf2f7')),('GRID',(0,0),(-1,-1),0.4,colors.HexColor('#ccd5df')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
        story.extend([table, Spacer(1,9)])
    elif el.tag == 'pre':
        story.append(Preformatted(''.join(el.itertext()), styles['Code']))
    elif el.tag in ('ol', 'ul'):
        for i,item in enumerate(el.findall('li'),1):
            story.append(Paragraph(f'{i}. '+inner(item), styles['BodyText']))

def footer(canvas, doc):
    canvas.setFont('Body', 8)
    canvas.drawString(40, 25, 'SVV — Clinic Appointment Management System')
    canvas.drawRightString(A4[0]-40, 25, str(doc.page))

SimpleDocTemplate(str(ROOT/'docs/report.pdf'), pagesize=A4, rightMargin=40, leftMargin=40, topMargin=42, bottomMargin=42, title='Clinic Appointment Management System — SVV Report', author='Sıla SERT (AI assistance disclosed)').build(story, onFirstPage=footer, onLaterPages=footer)
print('Exported docs/report.html and docs/report.pdf')
