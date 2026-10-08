# -*- coding: utf-8 -*-
"""Extract text from legacy .doc via Word COM (Windows) - robustness pass."""
import os
import time

SRC = r"D:\quanbudaima\bi_ye_she_ji\23级毕业设计文档-要求-参考案例"
OUT = r"D:\quanbudaima\bi_ye_she_ji\.workbuddy\tmp\out"
os.makedirs(OUT, exist_ok=True)

import win32com.client as wc  # noqa

try:
    word = wc.gencache.EnsureDispatch("Word.Application")
except Exception:
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

for fn in os.listdir(SRC):
    if not fn.lower().endswith(".doc"):
        continue
    src = os.path.join(SRC, fn)
    dst = os.path.join(OUT, os.path.splitext(fn)[0] + ".doc.txt")
    doc = word.Documents.Open(src)
    # get text via Content.Text, avoid SaveAs
    txt = doc.Content.Text
    with open(dst, "w", encoding="utf-8") as f:
        f.write(txt)
    doc.Close(False)
    print("saved", dst, len(txt))

try:
    word.Quit()
except Exception:
    pass
