"""把正文「图3.3 系统数据库 ER 图」替换为最新渲染的 shots/db-01.png。

前置：先渲染 ER 图（普通终端执行，沙箱下 spawn 会 EBUSY）：
    node scripts/shot.mjs "file:///D:/quanbudaima/bi_ye_she_ji/shots/_figs/html/db-01.html" db-01 --w 2000 --full
用法：
    python scripts/replace-fig33.py          # 检查定位（不写盘）
    python scripts/replace-fig33.py --apply  # 执行替换
"""
import pathlib
import sys

from docx import Document
from docx.oxml.ns import qn

ROOT = pathlib.Path(r'D:/quanbudaima/bi_ye_she_ji')
DOCX = ROOT / 'docs/03-正文/毕业设计正文-简柚个人博客系统的设计与实现.docx'
PNG = ROOT / 'shots/db-01.png'
CAPTION = '图3.3'

d = Document(str(DOCX))

# 1) 定位题注段
cap_idx = [i for i, p in enumerate(d.paragraphs) if p.text.strip().startswith(CAPTION)]
assert len(cap_idx) == 1, f'题注 {CAPTION} 匹配 {len(cap_idx)} 次'
cap_idx = cap_idx[0]
print(f'题注段 P{cap_idx}: {d.paragraphs[cap_idx].text.strip()}')

# 2) 向前找最近的含图片段落
img_idx, rids = None, []
for i in range(cap_idx, max(cap_idx - 5, -1), -1):
    blips = d.paragraphs[i]._p.findall('.//' + qn('a:blip'))
    if blips:
        img_idx = i
        rids = [b.get(qn('r:embed')) for b in blips]
        break
assert img_idx is not None, '题注上方未找到图片段落'
print(f'图片段 P{img_idx}，rId = {rids}')

parts = d.part.related_parts
for rid in rids:
    part = parts[rid]
    print(f'  {rid}: {part.content_type}, 原大小 {len(part.blob)} 字节')

if '--apply' not in sys.argv:
    print('\n（未写盘；确认无误后加 --apply 执行）')
    sys.exit(0)

assert PNG.exists(), f'未找到 {PNG}，请先渲染 ER 图'
new_bytes = PNG.read_bytes()
for rid in rids:
    parts[rid]._blob = new_bytes
d.save(str(DOCX))
print(f'\n已替换：图{cap_idx} 使用 {PNG.name}（{len(new_bytes)} 字节）')
