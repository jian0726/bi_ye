# -*- coding: utf-8 -*-
"""按学院排版规范把设计文档 md 原生渲染为 docx。

规范来源：docs/03-正文/格式规范.md（22 级正文要求）
  文档大标题：宋体小三加粗居中；一级标题：宋体小四加粗；二三级标题：宋体小四
  正文：宋体五号两端对齐；表格内容：宋体五号居中；图题：黑体五号居中
  页边距：上下 2.5cm、左 3.0cm、右 2.6cm；行距 1.5 倍
mermaid 代码块替换为已渲染 PNG（shots/_figs/png/）并自动加「图N 标题」。

用法：python md2docx.py <md路径> <docx路径> <fig-prefix>
"""
import pathlib
import re
import struct
import sys

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

SONG, HEI, MONO = '宋体', '黑体', 'Consolas'
GRAY = (0x59, 0x59, 0x59)

INLINE = re.compile(r'(\*\*.+?\*\*|`[^`]+`|\[[^\]]+\]\([^)]+\))')


def set_run(run, font=SONG, size=10.5, bold=False, color=None):
    run.font.name = font
    run.font.size = Pt(size)
    run.bold = bold
    rpr = run._element.get_or_add_rPr()
    rf = rpr.find(qn('w:rFonts'))
    if rf is None:
        rf = OxmlElement('w:rFonts')
        rpr.append(rf)
    rf.set(qn('w:ascii'), font)
    rf.set(qn('w:hAnsi'), font)
    rf.set(qn('w:eastAsia'), font)
    if color:
        run.font.color.rgb = RGBColor(*color)


def add_inline(p, text, size=10.5, font=SONG, bold=False):
    for part in INLINE.split(text):
        if not part:
            continue
        if part.startswith('**') and part.endswith('**') and len(part) > 4:
            r = p.add_run(part[2:-2])
            set_run(r, font, size, True)
        elif part.startswith('`') and part.endswith('`') and len(part) > 2:
            r = p.add_run(part[1:-1])
            set_run(r, MONO, size - 1.5)
        elif part.startswith('[') and '](' in part:
            m = re.match(r'\[([^\]]+)\]\([^)]+\)', part)
            r = p.add_run(m.group(1) if m else part)
            set_run(r, font, size, bold)
        else:
            r = p.add_run(part)
            set_run(r, font, size, bold)


def base_para(doc, size=10.5, align=None, indent=True, before=0, after=0, line=1.5):
    p = doc.add_paragraph()
    if align is not None:
        p.alignment = align
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing = line
    if indent:
        pf.first_line_indent = Pt(size * 2)
    return p


def shade(p, fill='F3F3F3'):
    ppr = p._p.get_or_add_pPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:fill'), fill)
    ppr.append(shd)


def png_size(path):
    d = pathlib.Path(path).read_bytes()[:24]
    w, h = struct.unpack('>II', d[16:24])
    return w, h


def parse_blocks(lines):
    blocks = []
    i, n = 0, len(lines)
    while i < n:
        s = lines[i].strip()
        if s.startswith('```'):
            lang = s[3:].strip()
            j = i + 1
            buf = []
            while j < n and not lines[j].strip().startswith('```'):
                buf.append(lines[j])
                j += 1
            blocks.append(('mermaid' if lang == 'mermaid' else 'code', buf))
            i = j + 1
            continue
        if s.startswith('|') and i + 1 < n and re.match(r'^\|[\s:\-|]+\|?$', lines[i + 1].strip()):
            rows = [s]
            j = i + 1
            while j < n and lines[j].strip().startswith('|'):
                rows.append(lines[j].strip())
                j += 1
            blocks.append(('table', rows))
            i = j
            continue
        m = re.match(r'^(#{1,6})\s+(.*)$', s)
        if m:
            blocks.append(('h%d' % len(m.group(1)), m.group(2)))
            i += 1
            continue
        if s == '':
            i += 1
            continue
        if s == '---':
            blocks.append(('hr', None))
            i += 1
            continue
        if s.startswith('>'):
            buf = []
            while i < n and lines[i].strip().startswith('>'):
                buf.append(lines[i].strip().lstrip('>').strip())
                i += 1
            blocks.append(('quote', buf))
            continue
        if re.match(r'^([-*]|\d+\.)\s+', s):
            buf = []
            while i < n and re.match(r'^\s*([-*]|\d+\.)\s+', lines[i]):
                indent = len(lines[i]) - len(lines[i].lstrip())
                buf.append((indent, lines[i].strip()))
                i += 1
            blocks.append(('list', buf))
            continue
        buf = [s]
        i += 1
        while i < n:
            t = lines[i].strip()
            if (t == '' or t.startswith(('```', '|', '#', '>'))
                    or t == '---' or re.match(r'^([-*]|\d+\.)\s+', t)):
                break
            buf.append(t)
            i += 1
        blocks.append(('p', ' '.join(buf)))
    return blocks


def split_cells(row):
    return [c.strip() for c in row.strip().strip('|').split('|')]


def render(md_path, out_path, fig_prefix, fig_dir):
    lines = pathlib.Path(md_path).read_text(encoding='utf-8').splitlines()
    blocks = parse_blocks(lines)

    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21), Cm(29.7)
    sec.top_margin = sec.bottom_margin = Cm(2.5)
    sec.left_margin, sec.right_margin = Cm(3.0), Cm(2.6)
    normal = doc.styles['Normal']
    normal.font.name = SONG
    normal.font.size = Pt(10.5)
    normal._element.rPr.rFonts.set(qn('w:eastAsia'), SONG)

    fig_no = 0
    fig_counter = 0
    in_refs = False

    for kind, payload in blocks:
        if kind == 'h1':
            p = base_para(doc, 15, WD_ALIGN_PARAGRAPH.CENTER, indent=False, after=10)
            add_inline(p, payload, 15, SONG, bold=True)
        elif kind == 'h2':
            if '参考文献' in payload:
                in_refs = True
            p = base_para(doc, 12, indent=False, before=10, after=4)
            add_inline(p, payload, 12, SONG, bold=True)
        elif kind in ('h3', 'h4', 'h5', 'h6'):
            p = base_para(doc, 12, indent=False, before=6, after=3)
            add_inline(p, payload, 12, SONG, bold=False)
        elif kind == 'p':
            p = base_para(doc, indent=not in_refs)
            if in_refs:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                p.paragraph_format.first_line_indent = Pt(0)
            add_inline(p, payload)
        elif kind == 'quote':
            for q in payload:
                if not q:
                    continue
                p = base_para(doc, indent=False, after=2)
                p.paragraph_format.left_indent = Pt(18)
                add_inline(p, q)
                for r in p.runs:
                    r.font.color.rgb = RGBColor(*GRAY)
        elif kind == 'list':
            for indent, item in payload:
                ordered = re.match(r'^(\d+\.)\s+(.*)$', item)
                if ordered:
                    text = ordered.group(1) + ' ' + ordered.group(2)
                else:
                    text = '• ' + re.sub(r'^[-*]\s+', '', item)
                p = base_para(doc, indent=False, after=2)
                p.paragraph_format.left_indent = Pt(14 + min(indent, 6) * 12)
                add_inline(p, text)
        elif kind == 'hr':
            continue
        elif kind == 'code':
            for ln in payload:
                p = base_para(doc, indent=False, after=0, line=1.15)
                r = p.add_run(ln if ln else ' ')
                set_run(r, MONO, 9)
                shade(p)
            base_para(doc, indent=False, after=4, line=1.0)
        elif kind == 'mermaid':
            fig_counter += 1
            png = pathlib.Path(fig_dir) / ('%s-%02d.png' % (fig_prefix, fig_counter))
            if png.exists():
                fig_no += 1
                w_px, h_px = png_size(png)
                max_w, max_h = 15.2, 19.0
                w_cm = max_w
                h_cm = w_cm * h_px / w_px
                if h_cm > max_h:
                    h_cm = max_h
                    w_cm = h_cm * w_px / h_px
                p = base_para(doc, align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, before=6)
                p.add_run().add_picture(str(png), width=Cm(w_cm))
                cap = base_para(doc, align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, after=8)
                add_inline(cap, '图%d' % fig_no, 10.5, HEI)
            else:
                p = base_para(doc, indent=False)
                add_inline(p, '【图位：%s-%02d】' % (fig_prefix, fig_counter))
        elif kind == 'table':
            rows = [split_cells(r) for r in payload]
            rows = [r for k, r in enumerate(rows) if k != 1]
            cols = max(len(r) for r in rows)
            t = doc.add_table(rows=0, cols=cols)
            t.style = 'Table Grid'
            t.alignment = WD_TABLE_ALIGNMENT.CENTER
            for ri, row in enumerate(rows):
                cells = t.add_row().cells
                for ci in range(cols):
                    txt = row[ci] if ci < len(row) else ''
                    para = cells[ci].paragraphs[0]
                    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    para.paragraph_format.space_after = Pt(0)
                    para.paragraph_format.line_spacing = 1.15
                    add_inline(para, txt, 10.5, SONG, bold=(ri == 0))
            base_para(doc, indent=False, after=4, line=1.0)

    doc.save(out_path)
    print('saved:', out_path, '| figs:', fig_no, '/', fig_counter)


if __name__ == '__main__':
    md, out, prefix = sys.argv[1], sys.argv[2], sys.argv[3]
    fig_dir = r'D:\quanbudaima\bi_ye_she_ji\shots\_figs\png'
    render(md, out, prefix, fig_dir)
