# -*- coding: utf-8 -*-
"""Extract text from legacy .doc via Word COM (Windows)."""
import os

SRC = r"D:\quanbudaima\bi_ye_she_ji\23级毕业设计文档-要求-参考案例"
OUT = r"D:\quanbudaima\bi_ye_she_ji\.workbuddy\tmp\out"
os.makedirs(OUT, exist_ok=True)

import win32com.client  # noqa

word = win32com.client.Dispatch("Word.Application")
word.Visible = False
word.DisplayAlerts = 0
try:
    for fn in os.listdir(SRC):
        if not fn.lower().endswith(".doc"):
            continue
        src = os.path.join(SRC, fn)
        dst = os.path.join(OUT, os.path.splitext(fn)[0] + ".txt")
        doc = word.Documents.Open(src, ReadOnly=True, AddToRecentFiles=False)
        doc.SaveAs2(dst, FileFormat=2)  # wdFormatText
        doc.Close(False)
        print("saved", dst)
finally:
    word.Quit()
