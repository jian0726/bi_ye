"""
第三层审计：Service 层的 public 方法是否有调用方（Controller / 其他 Service / Task）。

目的：发现「为某功能写了 Service 方法，但接口或功能整体没上线」的死代码。
"""
import re
from pathlib import Path

ROOT = Path(r"D:\quanbudaima\bi_ye_she_ji\blog-backend/src/main/java/com/jianyou/blog")

# 收集所有 java 文件
all_java = {p: p.read_text(encoding="utf-8", errors="replace") for p in ROOT.rglob("*.java")}

# ---------- 1. 提取 Service 的 public 方法 ----------
service_methods = {}  # (类名, 方法名) -> 文件
for p, text in all_java.items():
    if p.parent.name != "service" or p.name.endswith("Sender.java"):
        continue
    cls = p.stem
    if cls.endswith("Impl"):
        continue
    # public xxx 方法名( ...  —— 排除构造器与 getter/setter
    for m in re.finditer(r'public\s+(?:static\s+)?(?:final\s+)?[\w<>,\s\[\]\.]+\s+(\w+)\s*\(', text):
        name = m.group(1)
        if name in ("main",):
            continue
        service_methods[(cls, name)] = p.name

# ---------- 2. 找调用方 ----------
def is_called(owner_cls, method):
    """在其他文件中查找 <任意>.method( 或 直接 method( 的调用"""
    pat = re.compile(r'(?:\w+\s*\.\s*)?' + re.escape(method) + r'\s*\(')
    hits = []
    for p, text in all_java.items():
        if p.name == owner_cls + ".java":
            continue
        for m in pat.finditer(text):
            line_no = text[: m.start()].count("\n") + 1
            # 排除定义处（public xxx method(）
            ln = text.splitlines()[line_no - 1]
            if re.search(r'\bpublic\s', ln) and owner_cls in ln:
                continue
            hits.append(str(p.relative_to(ROOT)))
            break
    return hits


dead = []
for (cls, method), fname in sorted(service_methods.items()):
    if method.startswith(("get", "set", "is", "toString", "hashCode", "equals")):
        continue
    hits = is_called(cls, method)
    if not hits:
        dead.append((cls, method, fname))

print("=" * 78)
print(f"Service public 方法 {len(service_methods)} 个")
print("=" * 78)
print(f"\n【Service 方法无任何调用方】{len(dead)} 个\n")
for cls, method, fname in dead:
    print(f"  {cls}.{method}()   [{fname}]")
if not dead:
    print("  （无）")

# ---------- 3. 反射调用豁免说明 ----------
print()
print("=" * 78)
print("注：Mapper 方法由 MyBatis-Plus/XML 动态调用，不参与本检查；")
print("    Controller 接口的调用方在前端，已由 audit-endpoints.py 单独审计。")
