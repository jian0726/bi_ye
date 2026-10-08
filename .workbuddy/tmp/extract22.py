# -*- coding: utf-8 -*-
"""Extract the '22级_毕业设计正文要求' doc only."""
import os
import time

SRC = r"D:\quanbudaima\bi_ye_she_ji\23级毕业设计文档-要求-参考案例\暂定参考-22级_毕业设计正文要求.doc"
OUT = r"D:\quanbudaima\bi_ye_she_ji\.workbuddy\tmp\out\22级_正文要求.txt"

import win32com.client as wc  # noqa

word = wc.Dispatch("Word.Application")
time.sleep(1)
try:
    word.Visible = False
except Exception:
    pass
try:
    word.DisplayAlerts = 0
except Exception:
    pass

doc = word.Documents.Open(SRC)
time.sleep(0.5)
try:
    # copy all then read from a clipboard-free route: select-all paragraphs
    n = doc.Paragraphs.Count
    lines = []
    for i in range(1, n + 1):
        lines.append(doc.Paragraphs(i).Range.Text)
    txt = "".join(lines)
except Exception as e:
    txt = f"[paragraph loop failed] {e}"

with open(OUT, "w", encoding="utf-8") as f:
    f.write(txt)
print("len:", len(txt))
doc.Close(False)
try:
    word.Quit()
except Exception:
    pass
