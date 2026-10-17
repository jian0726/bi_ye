#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""DEF-01 修复验证：确认 JSON 字段类型不匹配由 500 转为 400
用于后端重启后执行：python verify-def01.py
"""
import json, urllib.request, urllib.error, sys

BASE = "http://localhost:8080/api"
R = []


def call(method, path, body=None, timeout=15):
    url = BASE + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    h = {"Accept": "application/json"}
    if data:
        h["Content-Type"] = "application/json;charset=UTF-8"
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


def check(cid, name, method, path, body, want_code):
    st, r = call(method, path, body)
    ok = r.get("code") == want_code
    R.append(ok)
    print(f"[{'PASS' if ok else 'FAIL'}] {cid} {name}")
    print(f"     期望 code={want_code}  实际 code={r.get('code')}  msg={str(r.get('message'))[:50]}")
    return ok


print("=" * 70)
print("DEF-01 修复验证（GlobalExceptionHandler 补 HttpMessageNotReadableException）")
print("=" * 70)
print("\n--- 修复目标：以下 4 条应由 500 转为 400 ---")
check("DEF01-1", "留言 content 传对象", "POST", "/portal/messages",
      {"content": {"a": 1}, "nickname": "x"}, 400)
check("DEF01-2", "留言 content 传数组", "POST", "/portal/messages",
      {"content": ["arr"], "nickname": "x"}, 400)
check("DEF01-3", "登录 account 传对象", "POST", "/auth/login",
      {"account": {"obj": 1}, "password": "x"}, 400)
check("DEF01-4", "注册 nickname 传对象", "POST", "/auth/register",
      {"email": "v@example.com", "nickname": {"o": 1}, "password": "123456"}, 400)

print("\n--- 回归：以下必须保持原有行为不变 ---")
check("REG-1", "对照组：content 传字符串（应 200）", "POST", "/portal/messages",
      {"content": "正常文本", "nickname": "x"}, 200)
check("REG-2", "必选参数：留言缺 content（应 400）", "POST", "/portal/messages",
      {"nickname": "x"}, 400)
check("REG-3", "边界：留言 301 字（应 400）", "POST", "/portal/messages",
      {"content": "x" * 301, "nickname": "x"}, 400)
check("REG-4", "参数类型：page=abc（应 400）", "GET", "/portal/articles?page=abc",
      None, 400)
check("REG-5", "正常分页（应 200）", "GET", "/portal/articles?page=1&size=3", None, 200)
check("REG-6", "不存在接口（应 404）", "GET", "/portal/nonexistent", None, 404)
check("REG-7", "后台未登录（应 401）", "GET", "/admin/dashboard", None, 401)

print("\n" + "=" * 70)
p = sum(1 for x in R if x)
print(f"结果：{p}/{len(R)} 通过")
if p == len(R):
    print("✅ DEF-01 修复生效，且无回归")
else:
    print("❌ 存在未通过项，请检查后端是否已重启加载新代码")
sys.exit(0 if p == len(R) else 1)
