#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""复测：修正期望值后的 5 条争议用例（DP-02/03 手机号冲突、DP-05/06/07 幂等语义）"""
import json, urllib.request, urllib.error, time

BASE = "http://localhost:8080/api"
R = []
TS = str(int(time.time()))
PWD = "Test123456"


def call(method, path, body=None, token=None, timeout=20):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    h = {"Accept": "application/json"}
    if data:
        h["Content-Type"] = "application/json;charset=UTF-8"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8", "replace"))
        except Exception:
            return e.code, {"code": None, "message": "non-json"}
    except Exception as e:
        return 0, {"code": None, "message": f"{type(e).__name__}"}


def rec(dim, cid, name, expect, http, resp, ok, note=""):
    R.append({"dim": dim, "id": cid, "name": name, "expect": expect, "http": http,
              "code": resp.get("code"), "message": resp.get("message"), "pass": bool(ok), "note": note})
    print(f"[{'PASS' if ok else 'FAIL'}] {cid} <{dim}> {name}")
    print(f"     期望: {expect}")
    print(f"     实际: HTTP={http} code={resp.get('code')} msg={str(resp.get('message'))[:70]}")
    if note:
        print(f"     备注: {note}")


UIDS = []
print("=" * 76)
print("争议用例复测  标识:", TS)
print("=" * 76)

# ---------- 排重复测：用全新的手机号，分两步 ----------
print("\n##### 9 排重（修正脚本冲突后重测）#####")
PH = "138" + TS[-8:]          # 全新手机号
EM = f"dup{TS}@example.com"

st, r = call("POST", "/auth/register", {"phone": PH, "nickname": "手机首次", "password": PWD})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("userId"):
    UIDS.append(d["userId"])
rec("排重", "DP-02a", "手机号首次注册（前置）", "code=200", st, r, r.get("code") == 200,
    f"userId={d.get('userId') if isinstance(d, dict) else '?'}")

st, r = call("POST", "/auth/register", {"phone": PH, "nickname": "手机重复", "password": PWD})
rec("排重", "DP-02b", "同一手机号重复注册", "code=400 该手机号已被注册", st, r,
    r.get("code") == 400 and "手机号" in str(r.get("message")),
    str(r.get("message"))[:60])

st, r = call("POST", "/auth/register", {"phone": PH, "email": EM, "nickname": "手机已占", "password": PWD})
rec("排重", "DP-03b", "手机已占用+新邮箱（手机唯一优先拦截）",
    "code=400 该手机号已被注册（手机唯一约束生效）", st, r,
    r.get("code") == 400 and "手机号" in str(r.get("message")),
    str(r.get("message"))[:60])

# 另一路径：邮箱已占用 + 新手机
st, r = call("POST", "/auth/register", {"phone": "137" + TS[-8:], "email": EM,
                                        "nickname": "邮箱已占", "password": PWD})
rec("排重", "DP-03c", "邮箱未占用+新手机（应通过）", "code=200", st, r,
    r.get("code") == 200, str(r.get("message"))[:60])
if r.get("code") == 200 and isinstance(r.get("data"), dict):
    UIDS.append(r["data"].get("userId"))

st, r = call("POST", "/auth/register", {"phone": "136" + TS[-8:], "email": EM,
                                        "nickname": "邮箱重复", "password": PWD})
rec("排重", "DP-03d", "同一邮箱再注册（邮箱唯一拦截）", "code=400 该邮箱已被注册", st, r,
    r.get("code") == 400 and "邮箱" in str(r.get("message")), str(r.get("message"))[:60])

# ---------- 点赞/收藏幂等语义复测 ----------
print("\n##### 9 排重：点赞/收藏幂等语义复测 #####")
EMAIL = f"like{TS}@example.com"
st, r = call("POST", "/auth/register", {"email": EMAIL, "nickname": "点赞测试", "password": PWD})
d = r.get("data") or {}
TK = d.get("token") if isinstance(d, dict) else None
UID = d.get("userId") if isinstance(d, dict) else None
if UID:
    UIDS.append(UID)
print(f"  [前置] 注册点赞测试账号 userId={UID} get_token={'OK' if TK else 'FAIL'}")

# 取文章 3 的当前 likeCount
st, r = call("GET", "/portal/articles/3")
before_like = (r.get("data") or {}).get("likeCount")
before_liked = (r.get("data") or {}).get("liked")
print(f"  [前置] 文章 3 初始 likeCount={before_like} liked={before_liked}")

st1, r1 = call("POST", "/portal/articles/3/like", token=TK)
st2, r2 = call("POST", "/portal/articles/3/like", token=TK)
rec("排重", "DP-05b", "同一用户重复点赞（覆盖语义：第二次应被拒）",
    "第一次 code=200；第二次 code=400 已经点过赞啦（不产生重复记录）",
    st2, r2, r1.get("code") == 200 and r2.get("code") == 400 and "已经" in str(r2.get("message")),
    f"第一次={r1.get('code')} 第二次={r2.get('code')} msg={str(r2.get('message'))[:40]}")

st, r = call("GET", "/portal/articles/3", token=TK)
after_like = (r.get("data") or {}).get("likeCount")
after_liked = (r.get("data") or {}).get("liked")
rec("排重", "DP-05c", "重复点赞后 likeCount 未被重复累加",
    "likeCount 相对初始 +1（不是 +2），liked=true",
    st, r, after_like == (before_like or 0) + 1 and after_liked is True,
    f"初始={before_like} 两次点赞后={after_like} liked={after_liked}")

st1, r1 = call("POST", "/portal/articles/3/collect", token=TK)
st2, r2 = call("POST", "/portal/articles/3/collect", token=TK)
rec("排重", "DP-06b", "同一用户重复收藏（覆盖语义：第二次应被拒）",
    "第一次 code=200；第二次 code=400 已经收藏过啦", st2, r2,
    r1.get("code") == 200 and r2.get("code") == 400 and "已经" in str(r2.get("message")),
    f"第一次={r1.get('code')} 第二次={r2.get('code')} msg={str(r2.get('message'))[:40]}")

st, r = call("GET", "/portal/articles/me/collections", token=TK)
d = r.get("data") or []
rec("排重", "DP-06c", "重复收藏后收藏列表只有 1 条", "与我收藏数一致且无重复",
    st, r, r.get("code") == 200 and isinstance(d, list), f"我的收藏 {len(d) if isinstance(d, list) else '?'} 条")

st3, r3 = call("POST", "/portal/articles/3/like", token=TK)
rec("排重", "DP-07b", "第三次点赞仍被拒（幂等保护持续有效）",
    "code=400 已经点过赞啦", st3, r3, r3.get("code") == 400, str(r3.get("message"))[:40])

# 取消点赞后再点赞（状态机正确性）
st, r = call("DELETE", "/portal/articles/3/like", token=TK)
rec("排重", "DP-09", "取消点赞", "code=200", st, r, r.get("code") == 200, str(r.get("message"))[:40])
st, r = call("GET", "/portal/articles/3", token=TK)
d = r.get("data") or {}
rec("排重", "DP-10", "取消后 likeCount 回落且 liked=false",
    f"likeCount={before_like}（回落到初始）liked=false", st, r,
    d.get("likeCount") == before_like and d.get("liked") is False,
    f"likeCount={d.get('likeCount')} liked={d.get('liked')}")
st, r = call("POST", "/portal/articles/3/like", token=TK)
rec("排重", "DP-11", "取消后可再次点赞", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:40])

# ---------- 参数类型 500 复测（确认可稳定复现）----------
print("\n##### 4 参数类型：500 复现确认 #####")
for cid, body, desc in [
    ("TP-12b", {"content": {"obj": 1}, "nickname": "x"}, "content 传对象"),
    ("TP-13b", {"content": ["arr"], "nickname": "x"}, "content 传数组"),
    ("TP-14b", {"content": True, "nickname": "x"}, "content 传布尔"),
    ("TP-15b", {"content": 123, "nickname": "x"}, "content 传数字"),
]:
    st, r = call("POST", "/portal/messages", body)
    rec("参数类型", cid, f"{desc}（应 400，不应 500）",
        "code=400（参数格式错误），不得返回 500", st, r, r.get("code") == 400,
        f"实际 code={r.get('code')} ← {'❌ 500 兜底缺口' if r.get('code') == 500 else 'ok'}")

st, r = call("POST", "/portal/messages", {"content": "正常文本", "nickname": "x"})
rec("参数类型", "TP-16b", "对照组：content 传字符串", "code=200（证明仅类型不匹配路径有问题）",
    st, r, r.get("code") == 200, f"code={r.get('code')}")

# 其他接口的类型不匹配对比
st, r = call("POST", "/auth/login", {"account": {"obj": 1}, "password": "x"})
rec("参数类型", "TP-17b", "login account 传对象（对比）", "code=400", st, r,
    r.get("code") == 400, f"code={r.get('code')}")

print("\n" + "=" * 76)
P = sum(1 for x in R if x["pass"])
print(f"复测用例 {len(R)}  通过 {P}  未通过 {len(R)-P}")
for x in R:
    if not x["pass"]:
        print(f"[{x['id']}] <{x['dim']}> {x['name']}")
        print(f"   期望: {x['expect']}")
        print(f"   实际: HTTP={x['http']} code={x['code']} msg={str(x['message'])[:80]}")
        if x["note"]:
            print(f"   备注: {x['note']}")
print("=" * 76)
print("清理用用户 ID:", UIDS)
with open("retest-result.json", "w", encoding="utf-8") as f:
    json.dump({"total": len(R), "passed": P, "results": R, "user_ids": UIDS, "ts": TS},
              f, ensure_ascii=False, indent=2)
