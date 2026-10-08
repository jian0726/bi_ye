"""
扫描后端所有接口，与前端调用比对，找出「写了但没用」的接口。

关键点：前端写法形如 request.get<never, PageResult<Article>>('/path', {...})，
泛型参数可能嵌套 >>，用简单正则会失配。这里用尖括号计数跳过泛型，再取第一个引号串。
"""
import re
from pathlib import Path

ROOT = Path(r"D:\quanbudaima\bi_ye_she_ji")
BACK = ROOT / "blog-backend/src/main/java/com/jianyou/blog/controller"
FRONT_SRC = ROOT / "blog-web/src"

MAPPING = {
    "GetMapping": "GET",
    "PostMapping": "POST",
    "PutMapping": "PUT",
    "DeleteMapping": "DELETE",
    "PatchMapping": "PATCH",
}

# ---------- 1. 后端接口 ----------
endpoints = []
for f in sorted(BACK.glob("*.java")):
    text = f.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    base = ""
    for ln in lines:
        m = re.search(r'@RequestMapping\(\s*"([^"]+)"', ln)
        if m:
            base = m.group(1)
            break
    for i, ln in enumerate(lines):
        m = re.search(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"|\(\s*\))?', ln)
        if not m:
            continue
        http = MAPPING[m.group(1) + "Mapping"]
        sub = m.group(2) or ""
        jm = ""
        for j in range(i, min(i + 6, len(lines))):
            fm = re.search(r'public\s+[\w<>,\s\[\]\.]+\s+(\w+)\s*\(', lines[j])
            if fm:
                jm = fm.group(1)
                break
        endpoints.append((http, (base + sub) or "/", f.name, jm, i + 1))


# ---------- 2. 前端调用 ----------
def extract_calls(text):
    """提取 request.xxx('url') 的 url，正确跳过嵌套泛型"""
    out = []
    for m in re.finditer(r'request\s*\.\s*(get|post|put|delete|patch)\b', text):
        i, n = m.end(), len(text)
        while i < n and text[i].isspace():
            i += 1
        if i < n and text[i] == '<':            # 跳过泛型，支持嵌套
            depth = 0
            while i < n:
                if text[i] == '<':
                    depth += 1
                elif text[i] == '>':
                    depth -= 1
                    if depth == 0:
                        i += 1
                        break
                i += 1
            while i < n and text[i].isspace():
                i += 1
        if i >= n or text[i] != '(':
            continue
        i += 1
        while i < n and text[i].isspace():
            i += 1
        if i < n and text[i] in '"\'`':
            q = text[i]
            j = i + 1
            while j < n and text[j] != q:
                j += 2 if text[j] == '\\' else 1
            out.append(text[i + 1:j])
    return out


def norm(u):
    u = u.split("?")[0]
    if u.startswith("/api"):
        u = u[4:]
    return u.rstrip("/") or "/"


front_calls = []
for f in FRONT_SRC.rglob("*"):
    if f.suffix not in (".ts", ".vue") or "node_modules" in str(f):
        continue
    for u in extract_calls(f.read_text(encoding="utf-8", errors="replace")):
        front_calls.append((norm(u), str(f.relative_to(ROOT))))

# 前端 URL 模板 -> 正则（${x} 与 {x} 视为变量段）
front_rx = []
for u, f in front_calls:
    p = re.sub(r"\$\{[^}]+\}", "\x00", u)
    p = re.sub(r"\{[^}]+\}", "\x00", p)
    rx = re.compile("^" + re.escape(p).replace("\x00", "[^/]+") + "$")
    front_rx.append((rx, f, u))


def find_use(path):
    p = re.sub(r"\{[^}]+\}", "\x00", path)
    rx = re.compile("^" + re.escape(p).replace("\x00", "[^/]+") + "$")
    for frx, f, raw in front_rx:
        if frx.pattern == rx.pattern:
            return f, raw
    return None


# ---------- 3. 报告 ----------
print("=" * 80)
print(f"后端接口 {len(endpoints)} 个 ｜ 前端 request 调用点 {len(front_calls)} 个")
print("=" * 80)

unused = [e for e in endpoints if not find_use(e[1])]
print(f"\n【后端有、前端无调用】{len(unused)} 个\n")
if unused:
    by_file = {}
    for http, path, jf, jm, ln in unused:
        by_file.setdefault(jf, []).append((http, path, jm, ln))
    for jf in sorted(by_file):
        print(f"--- {jf} ---")
        for http, path, jm, ln in by_file[jf]:
            print(f"  {http:6} {path:48} {jm}()  L{ln}")
        print()
else:
    print("  （无）\n")

# 反向：前端调了但后端没有的（拼错/已删接口）
paths = set()
for http, path, *_ in endpoints:
    p = re.sub(r"\{[^}]+\}", "\x00", path)
    paths.add(re.escape(p).replace("\x00", "[^/]+"))
backend_rx = [re.compile("^" + p + "$") for p in paths]

orphan = []
for u, f in front_calls:
    p = re.sub(r"\$\{[^}]+\}", "\x00", u)
    if not any(rx.match(p) or rx.match(p.replace("\x00", "1")) for rx in backend_rx):
        orphan.append((u, f))
print(f"【前端调了、后端没有】{len(orphan)} 个\n")
seen = set()
for u, f in orphan:
    if (u, f) in seen:
        continue
    seen.add((u, f))
    print(f"  {u:48} {f}")
print()
