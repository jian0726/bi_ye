# -*- coding: utf-8 -*-
"""替换 docx 中 6 张过期截图，并按新图宽高比修正 wpspic extent 防变形。"""
import shutil, zipfile, struct, re
from PIL import Image

DOCX = r"D:\quanbudaima\bi_ye_she_ji\docs\03-正文\毕业设计正文-简柚博客网站系统的设计与实现.docx"
PNG = r"D:\quanbudaima\bi_ye_she_ji\shots\_figs\png"
MAP = {
    "word/media/image9.png":  PNG + r"\reshoot-album.png",       # 图3.9 相册瀑布流
    "word/media/image13.png": PNG + r"\reshoot-register.png",    # 图3.13 注册
    "word/media/image14.png": PNG + r"\reshoot-login.png",       # 图3.14 登录
    "word/media/image15.png": PNG + r"\reshoot-collections.png", # 图3.15 收藏
    "word/media/image16.png": PNG + r"\reshoot-dashboard.png",   # 图3.16 仪表盘
    "word/media/image20.png": PNG + r"\reshoot-admin-album.png", # 图3.20 相册管理
}

# 1) 生成替换后的 docx（zip 层面换 blob）
tmp = DOCX + ".tmp"
with zipfile.ZipFile(DOCX) as zin, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        data = zin.read(item.filename)
        if item.filename in MAP:
            data = open(MAP[item.filename], "rb").read()
            print("replaced", item.filename, len(data), "bytes")
        zout.writestr(item, data)
shutil.move(tmp, DOCX)

# 2) 修正每张图的显示 extent（保持宽度 cx，按新宽高比重算 cy）
from docx import Document
from docx.oxml.ns import qn
doc = Document(DOCX)
part_rels = {}
for name, _ in MAP.items():
    rid_prefix = name.split("/")[-1]
    part_rels[rid_prefix] = None

def img_size(p):
    with Image.open(p) as im:
        return im.size

sizes = {k.split("/")[-1]: img_size(v) for k, v in MAP.items()}

# 建立 rId -> media 文件名 映射
rels = doc.part.rels
rid2media = {rid: rel.target_ref.split("/")[-1] for rid, rel in rels.items() if "image" in rel.reltype}

n_fix = 0
for pic in doc.element.body.iter(qn("pic:pic")):
    blip = pic.find(qn("pic:blipFill") + "/" + qn("a:blip"))
    if blip is None:
        continue
    rid = blip.get(qn("r:embed"))
    media = rid2media.get(rid)
    if media not in sizes:
        continue
    w, h = sizes[media]
    for ext in pic.iter(qn("a:ext")):
        cx = int(ext.get("cx"))
        cy = int(cx * h / w)
        if abs(int(ext.get("cy")) - cy) > 1000:
            ext.set("cy", str(cy))
            n_fix += 1
    for ext in pic.iter(qn("wp:extent")):
        cx = int(ext.get("cx"))
        ext.set("cy", str(int(cx * h / w)))
print("extent fixed:", n_fix)
doc.save(DOCX)
print("saved")
