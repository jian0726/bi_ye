# -*- coding: utf-8 -*-
"""Extract text from .docx / .doc files under the reference folder."""
import os
import sys
import zipfile
import re

SRC = r"D:\quanbudaima\bi_ye_she_ji\23级毕业设计文档-要求-参考案例"
OUT = r"D:\quanbudaima\bi_ye_she_ji\.workbuddy\tmp\out"
os.makedirs(OUT, exist_ok=True)


def docx_text(path):
    with zipfile.ZipFile(path) as z:
        names = [n for n in z.namelist() if n.startswith("word/") and n.endswith(".xml")]
        parts = []
        if "word/document.xml" in names:
            xml = z.read("word/document.xml").decode("utf-8", "ignore")
            # paragraph split
            xml = xml.replace("</w:p>", "\n</w:p>")
            xml = xml.replace("<w:br/>", "\n")
            xml = re.sub(r"<w:tab[^>]*/>", "\t", xml)
            xml = re.sub(r"<[^>]+>", "", xml)
            parts.append(xml)
        return "\n".join(parts)


for fn in sorted(os.listdir(SRC)):
    p = os.path.join(SRC, fn)
    stem, ext = os.path.splitext(fn)
    ext = ext.lower()
    print("=" * 70)
    print("FILE:", fn)
    print("=" * 70)
    if ext == ".docx":
        try:
            t = docx_text(p)
        except Exception as e:
            t = f"[docx extract error] {e}"
    else:
        t = "[.doc binary: needs antiword/word com]"
    dst = os.path.join(OUT, stem + ".txt")
    with open(dst, "w", encoding="utf-8") as f:
        f.write(t)
    print(t[:1500])
    print("... [saved]", dst)
