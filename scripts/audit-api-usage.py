"""
第二层审计：api 层函数是否真的被组件调用。

Script A 只验证了「后端接口 ← api 函数」这一跳。
但 api 函数本身可能从未被任何组件 import —— 那接口在运行时依然不会被触发。
本脚本提取 api/*.ts 的导出函数名，再在 views/components/layouts/stores 里查是否被使用。
"""
import re
from pathlib import Path

ROOT = Path(r"D:\quanbudaima\bi_ye_she_ji\blog-web\src")
API_DIR = ROOT / "api"
CONSUMERS = [
    ROOT / "views", ROOT / "components", ROOT / "layouts",
    ROOT / "stores", ROOT / "router", ROOT / "composables", ROOT / "utils",
]

# ---------- 1. 提取 api 导出函数 ----------
api_funcs = {}  # 函数名 -> (文件名, 该函数调用的 url 列表)
for f in sorted(API_DIR.glob("*.ts")):
    if f.name == "request.ts":
        continue
    text = f.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    # 找 export function xxx / export const xxx =
    for i, ln in enumerate(lines):
        m = re.match(r"\s*export\s+(?:async\s+)?function\s+(\w+)", ln)
        if not m:
            m = re.match(r"\s*export\s+const\s+(\w+)\s*=", ln)
        if not m:
            continue
        name = m.group(1)
        # 收集该函数体内（到下一个 export 或文件尾）的 url
        body = []
        depth = 0
        started = False
        for j in range(i, min(i + 60, len(lines))):
            if j > i and re.match(r"\s*export\s+", lines[j]):
                break
            body.append(lines[j])
            if "{" in lines[j]:
                started = True
            depth += lines[j].count("{") - lines[j].count("}")
            if started and depth <= 0 and j > i:
                break
        blob = "\n".join(body)
        urls = re.findall(r"""['"`](/[^'"`]+)['"`]""", blob)
        api_funcs[name] = (f.name, urls)

# ---------- 2. 收集消费方代码（src 下除 api 目录外的全部代码） ----------
consumer_text = {}
for f in ROOT.rglob("*"):
    if f.suffix not in (".ts", ".vue") or "node_modules" in str(f):
        continue
    if API_DIR in f.parents or f.parent == API_DIR:   # 排除 api 自身，避免互调误判
        continue
    consumer_text[str(f.relative_to(ROOT))] = f.read_text(encoding="utf-8", errors="replace")

# ---------- 3. 判定 ----------
unused_funcs = []   # api 函数定义了但没人调用
for name, (api_file, urls) in sorted(api_funcs.items()):
    used_in = []
    for path, text in consumer_text.items():
        # 恰当成词匹配，避免 getArticle 命中 getArticles
        if re.search(r"\b" + re.escape(name) + r"\b", text):
            used_in.append(path)
    if not used_in:
        unused_funcs.append((name, api_file, urls))

print("=" * 80)
print(f"api 导出函数 {len(api_funcs)} 个")
print("=" * 80)

print(f"\n【api 函数已定义、但无任何组件调用】{len(unused_funcs)} 个\n")
if unused_funcs:
    for name, api_file, urls in unused_funcs:
        print(f"  {name}()   [{api_file}]")
        for u in urls:
            print(f"        -> {u}")
else:
    print("  （无）")
