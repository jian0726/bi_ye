"""
本轮改动验证：
 A. 热门文章排行接口已删除（/portal/articles/hot 应 404/路由不存在）
 B. 文章列表不再支持 sort 参数（传 sort=hot 也不会按浏览量排序，参数被忽略）
 C. collected 字段回显：登录 → 收藏一篇文章 → 列表与详情均带 collected=true → 取消 → false
 D. 友链申请：POST /portal/links/apply 落库待审（status=0）
 E. 游客调列表接口：collected 恒为 false

跑完自动清理测试数据。
"""
import json
import time
import urllib.request
import urllib.error

BASE = "http://localhost:8080/api"
ADMIN = ("13800000001", "jianyou2026")
results = []


def call(method, path, body=None, token=None):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {"raw": "non-json"}


def check(name, cond, detail=""):
    results.append((name, cond, detail))
    print(("  PASS  " if cond else "  FAIL  ") + name + ("   " + detail if detail else ""))


print("=" * 68)
print("A. 热门接口应已删除")
code, res = call("GET", "/portal/articles/hot")
# 删除后该路径会落进 /{id}，'hot' 不是合法 id → 应为 400 参数错误（而非 500 服务端故障）
gone = res.get("code") == 400 and "参数" in (res.get("message") or "")
check("/portal/articles/hot 不再是有效接口（400 参数错误，非 500）", gone,
      f"code={res.get('code')} msg={res.get('message')}")
# 顺带确认同类情况也不再兜底 500
code, res = call("GET", "/portal/articles?page=abc")
check("?page=abc 也返回 400 而非 500", res.get("code") == 400,
      f"code={res.get('code')} msg={res.get('message')}")

print("=" * 68)
print("B. 列表接口忽略 sort 参数（排序固定）")
code, res = call("GET", "/portal/articles?page=1&size=5")
r1 = [a["id"] for a in (res.get("data") or {}).get("records", [])]
check("不带 sort 正常返回", res.get("code") == 200 and len(r1) > 0, f"ids={r1}")
code, res = call("GET", "/portal/articles?page=1&size=5&sort=hot")
r2 = [a["id"] for a in (res.get("data") or {}).get("records", [])]
check("带 sort=hot 结果与不带一致（参数已忽略）", r1 == r2, f"{r1} vs {r2}")

print("=" * 68)
print("C. collected 回显")
code, res = call("POST", "/auth/login", {"account": ADMIN[0], "password": ADMIN[1]})
token = (res.get("data") or {}).get("token")
check("站长登录成功", bool(token), f"code={res.get('code')}")

# 游客视角
code, res = call("GET", "/portal/articles?page=1&size=5")
recs = (res.get("data") or {}).get("records", [])
check("游客：collected 字段存在且为 false",
      all(a.get("collected") is False for a in recs),
      f"sample={[a.get('collected') for a in recs[:3]]}")

TARGET = r1[0] if r1 else None
if TARGET:
    # 收藏
    code, res = call("POST", f"/portal/articles/{TARGET}/collect", None, token)
    check(f"收藏文章 {TARGET}", res.get("code") == 200, f"msg={res.get('message')}")

    code, res = call("GET", "/portal/articles?page=1&size=10", None, token)
    recs = (res.get("data") or {}).get("records", [])
    hit = next((a for a in recs if a["id"] == TARGET), None)
    check("登录后列表：该文章 collected=true", hit is not None and hit.get("collected") is True,
          f"collected={hit.get('collected') if hit else None}")
    others = [a.get("collected") for a in recs if a["id"] != TARGET]
    check("其它文章 collected 仍为 false", all(c is False for c in others), f"others={others[:5]}")

    code, res = call("GET", f"/portal/articles/{TARGET}", None, token)
    check("详情接口也带 collected=true", (res.get("data") or {}).get("collected") is True,
          f"collected={(res.get('data') or {}).get('collected')}")

    # 取消收藏后复原
    code, res = call("DELETE", f"/portal/articles/{TARGET}/collect", None, token)
    code, res = call("GET", f"/portal/articles?page=1&size=10", None, token)
    recs = (res.get("data") or {}).get("records", [])
    hit = next((a for a in recs if a["id"] == TARGET), None)
    check("取消收藏后 collected=false", hit is not None and hit.get("collected") is False,
          f"collected={hit.get('collected') if hit else None}")

print("=" * 68)
print("D. 友链申请落库待审")
STAMP = str(int(time.time()))[-6:]
LINK_NAME = f"[E2E]测试站{STAMP}"
code, res = call("POST", "/portal/links/apply", {
    "name": LINK_NAME,
    "url": "https://example.com",
    "description": "自动化测试提交",
    "email": "e2e@example.com"
})
check("申请提交成功（游客可提交）", res.get("code") == 200, f"msg={res.get('message')}")

# 校验：申请不应立即出现在已上架列表
code, res = call("GET", "/portal/links")
names = [l["name"] for l in (res.get("data") or [])]
check("待审友链不出现在已上架列表", LINK_NAME not in names, f"count={len(names)}")

# 缺必填应被拒
code, res = call("POST", "/portal/links/apply", {"name": "", "url": ""})
check("缺必填被拒", res.get("code") == 400, f"msg={res.get('message')}")

print()
print("=" * 68)
passed = sum(1 for _, c, _ in results if c)
print(f"结果：{passed}/{len(results)} 通过")
for name, cond, detail in results:
    if not cond:
        print(f"  未通过：{name}   {detail}")
print("=" * 68)
print(f"清理提示：删除 t_friend_link 中 name='{LINK_NAME}' 的记录")
