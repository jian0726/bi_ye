# 读取 ngrok 当前的公网域名，写入 vite.config.ts 的 allowedHosts
# 适用于把 allowedHosts 从 true 收紧为白名单的场景（例如交付评审要求最小放行）
#
# 用法：
#   python scripts\sync-ngrok-host.py           # 只打印建议写法
#   python scripts\sync-ngrok-host.py --write   # 直接改写 vite.config.ts
#
# 前提：ngrok 已在运行（读取 http://127.0.0.1:4040/api/tunnels）

import json
import re
import sys
import urllib.request
from pathlib import Path

VITE = Path(__file__).resolve().parent.parent / "blog-web" / "vite.config.ts"
API = "http://127.0.0.1:4040/api/tunnels"


def fetch_hosts():
    try:
        with urllib.request.urlopen(API, timeout=5) as r:
            data = json.load(r)
    except Exception as e:
        print(f"[X] 读取 ngrok API 失败：{e}")
        print("    确认 ngrok 正在运行（Web 控制台 http://127.0.0.1:4040）")
        sys.exit(1)
    hosts = []
    for t in data.get("tunnels", []):
        url = t.get("public_url", "")
        m = re.match(r"^https?://(.+)$", url)
        if m:
            hosts.append(m.group(1))
    return hosts


def main():
    hosts = fetch_hosts()
    if not hosts:
        print("[X] 没有活跃隧道，先执行 tunnel-dev.ps1 或 tunnel-docker.ps1")
        sys.exit(1)

    print("[OK] 当前 ngrok 域名：")
    for h in hosts:
        print("     ", h)

    block = "    allowedHosts: [\n" + "".join(f"      '{h}',\n" for h in hosts) + "    ],\n"
    print("\n建议写入 vite.config.ts 的内容：")
    print(block)

    if "--write" not in sys.argv:
        print("加 --write 参数可直接改写文件。")
        return

    src = VITE.read_text(encoding="utf-8")
    new, n = re.subn(
        r"[ \t]*allowedHosts:\s*(true|\[[^\]]*\]),?\n",
        block,
        src,
    )
    if n == 0:
        print("[!] 未在 vite.config.ts 中找到 allowedHosts，请手动添加")
        sys.exit(1)
    VITE.write_text(new, encoding="utf-8")
    print(f"[OK] 已写入 {VITE}（{n} 处），重启 dev server 生效")


if __name__ == "__main__":
    main()
