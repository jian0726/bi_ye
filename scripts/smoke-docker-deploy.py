"""Docker 部署冒烟：针对「移除留言回复 + 浏览量按访客去重 + 点赞按账号落库」三条改动。

用法：python scripts/smoke-docker-deploy.py [api_base] [web_base]
默认：http://127.0.0.1:8081/api 与 http://127.0.0.1:5174

判定原则：只断言「与本次改动直接相关」的行为，不重复造 smoke-auth.mjs 的登录/注册全家桶。
"""
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

API = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8081/api").rstrip("/")
WEB = (sys.argv[2] if len(sys.argv) > 2 else "http://127.0.0.1:5174").rstrip("/")
ADMIN = ("13800000001", "jianyou2026")

passed = failed = 0


def call(method, path, token=None, body=None, base=API):
    url = path if path.startswith("http") else base + path
    # query 里可能有中文（如按 IP 属地筛选），http.client 只接受 ascii，必须先做百分号编码
    url = urllib.parse.quote(url, safe=":/?&=%")
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            raw = r.read().decode("utf-8", "replace")
            code = r.status
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        code = e.code
    try:
        return code, json.loads(raw)
    except json.JSONDecodeError:
        # HTML 响应：多留一些字符，SPA 挂载点在 <body> 里，截太短会误判
        return code, {"_raw": raw[:3000]}


def check(name, ok, detail=""):
    global passed, failed
    if ok:
        passed += 1
        print(f"  [通过] {name}" + (f"　{detail}" if detail else ""))
    else:
        failed += 1
        print(f"  [失败] {name}　{detail}")


print("=" * 78)
print(f"API = {API}　WEB = {WEB}")
print("=" * 78)

# ---------- 1. 站点统计可达 ----------
print("\n1. 服务可达性")
code, j = call("GET", "/portal/site/stats")
check("GET /portal/site/stats 返回 200", code == 200 and j.get("code") == 200, f"http={code} code={j.get('code')}")

# ---------- 2. 浏览量：同一访客只计一次 ----------
print("\n2. 浏览量按独立访客去重（本次改造点）")
code, before = call("GET", "/portal/articles/1")
v1 = before.get("data", {}).get("viewCount")
code, again = call("GET", "/portal/articles/1")
v2 = again.get("data", {}).get("viewCount")
check("首次访问读到文章详情", code == 200 and isinstance(v1, int), f"viewCount={v1}")
check("同一访客立即再访问，浏览量不再增加", v1 == v2, f"{v1} -> {v2}")

# ---------- 3. 管理员登录 ----------
print("\n3. 管理员登录")
code, j = call("POST", "/auth/login", body={"account": ADMIN[0], "password": ADMIN[1]})
token = j.get("data", {}).get("token")
check("站长账号可登录并拿到 Token", code == 200 and bool(token), f"code={j.get('code')} msg={j.get('message')}")

# ---------- 4. 留言管理：无回复字段 / 按 IP 属地筛选 ----------
print("\n4. 后台留言管理（回复能力已移除）")
code, j = call("GET", "/admin/messages?page=1&size=10", token)
recs = j.get("data", {}).get("records", [])
check("留言列表可读取", code == 200 and j.get("code") == 200, f"共 {len(recs)} 条")
check(
    "返回记录不含 reply / replyTime 字段",
    all("reply" not in r and "replyTime" not in r for r in recs),
    f"字段={sorted(recs[0].keys()) if recs else '—'}",
)
locs = [r.get("ipLocation") for r in recs]
check("记录带 ipLocation 字段", any(locs), f"样例={locs[:3]}")

code, j2 = call("GET", "/admin/messages?page=1&size=10&ipLocation=湖南", token)
hit = j2.get("data", {}).get("records", [])
check(
    "按 IP 属地「湖南」筛选命中且全部匹配",
    j2.get("code") == 200 and len(hit) > 0 and all("湖南" in (r.get("ipLocation") or "") for r in hit),
    f"命中 {len(hit)}/{len(recs)} 条",
)

code, j3 = call("GET", "/admin/messages?page=1&size=10&ipLocation=不存在的属地XYZ", token)
check("筛选不存在的属地返回空列表而非报错", j3.get("code") == 200 and not j3.get("data", {}).get("records"), f"code={j3.get('code')}")

# ---------- 5. 已移除的回复接口确实不存在 ----------
print("\n5. 回复接口已下线")
code, j = call("PUT", "/admin/messages/1/reply", token, {"content": "不该存在"})
check(
    "PUT /admin/messages/{id}/reply 不再可用（404/405）",
    code in (404, 405),
    f"http={code}",
)

# ---------- 6. 前台留言 + 页面可达 ----------
print("\n6. 前台接口与页面")
code, j = call("GET", "/portal/messages?page=1&size=10")
check("GET /portal/messages 可读", code == 200 and j.get("code") == 200, f"共 {len(j.get('data', {}).get('records', []))} 条")

code, j = call("GET", WEB + "/", base=WEB)
raw = j.get("_raw", "")
check("前端首页可访问（nginx 200）", code == 200, f"http={code}")
check("返回的是 SPA 页面", "<div id=\"app\">" in raw or "id=app" in raw, f"片段={raw[:60]!r}")

# ---------- 7. 前端经 nginx 反代访问后端 ----------
print("\n7. nginx 反向代理 /api/ → backend:8080")
code, j = call("GET", WEB + "/api/portal/site/stats", base=WEB)
check("经 nginx 反代的 API 可用", code == 200 and j.get("code") == 200, f"http={code} code={j.get('code')}")

# ---------- 8. 上传链路（MinIO）----------
print("\n8. MinIO 上传链路连通性")
code, j = call("GET", "/admin/files?page=1&size=5", token)
check("资源列表接口可用（说明 MinIO 配置已装载）", code == 200 and j.get("code") == 200, f"code={j.get('code')}")

print("\n" + "=" * 78)
print(f"结果：通过 {passed} 项，失败 {failed} 项")
print("=" * 78)
sys.exit(0 if failed == 0 else 1)
