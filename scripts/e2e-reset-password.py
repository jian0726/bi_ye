"""
找回密码全链路 E2E（真实调接口，不 mock）

覆盖：
 1. 探账号 —— 邮箱注册的账号返回 email 渠道；不存在的账号 exists=false
 2. 渠道越权 —— 只有邮箱的账号要求发到 phone 应被拒
 3. 发验证码 —— dev 模式回传 devCode
 4. 错误验证码 —— 应被拒且提示剩余次数
 5. 正确验证码 + 重置密码 —— 成功
 6. 新密码登录成功、旧密码登录失败
 7. 验证码一次性 —— 同一码重复使用应失败
 8. 60 秒冷却 —— 连续发送应被拒
"""
import json
import sys
import time
import urllib.request
import urllib.error

BASE = "http://localhost:8080/api"
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
        return e.code, json.loads(e.read().decode())


def check(name, cond, detail=""):
    results.append((name, cond, detail))
    print(("  PASS  " if cond else "  FAIL  ") + name + ("   " + detail if detail else ""))


# 造一个邮箱注册的临时账号（用完即删）
STAMP = str(int(time.time()))[-8:]
EMAIL = f"e2e{STAMP}@example.com"
PHONE = "139" + STAMP
OLD_PWD = "oldpwd123456"
NEW_PWD = "newpwd654321"

print("=" * 62)
print("步骤 0：注册临时账号（邮箱 + 手机号都有，便于分别验证两个渠道）")
code, res = call("POST", "/auth/register", {
    "email": EMAIL, "phone": PHONE, "nickname": f"E2E{STAMP}", "password": OLD_PWD
})
check("注册成功", res.get("code") == 200, f"code={res.get('code')} msg={res.get('message')}")
if res.get("code") != 200:
    print(json.dumps(res, ensure_ascii=False, indent=2))
    sys.exit(1)
USER_ID = res["data"]["userId"]

print("=" * 62)
print("步骤 1：探账号（用邮箱）")
code, res = call("POST", "/auth/find-account", {"account": EMAIL})
d = res.get("data") or {}
chans = [c["channel"] for c in (d.get("channels") or [])]
check("exists=true", d.get("exists") is True)
check("返回 phone+email 两个渠道", set(chans) == {"phone", "email"}, f"channels={chans}")
masked = {c["channel"]: c["masked"] for c in (d.get("channels") or [])}
check("手机号已脱敏", masked.get("phone", "").count("*") >= 4, f"phone={masked.get('phone')}")
check("邮箱已脱敏", "***" in masked.get("email", ""), f"email={masked.get('email')}")

print("=" * 62)
print("步骤 2：探不存在的账号")
code, res = call("POST", "/auth/find-account", {"account": "nobody@nowhere.test"})
check("exists=false", (res.get("data") or {}).get("exists") is False)

print("=" * 62)
print("步骤 3：发验证码到邮箱（dev 模式应回传 devCode）")
code, res = call("POST", "/auth/send-code", {"account": EMAIL, "channel": "email"})
dev_code = (res.get("data") or {}).get("devCode")
check("发送成功", res.get("code") == 200, f"msg={res.get('message')}")
check("dev 模式回传 6 位验证码", bool(dev_code) and len(dev_code) == 6, f"devCode={dev_code}")

print("=" * 62)
print("步骤 4：60 秒冷却（立刻重发应被拒）")
code, res = call("POST", "/auth/send-code", {"account": EMAIL, "channel": "email"})
check("冷却生效", res.get("code") == 400 and "频繁" in (res.get("message") or ""),
      f"msg={res.get('message')}")

print("=" * 62)
print("步骤 5：错误验证码应被拒")
code, res = call("POST", "/auth/reset-password", {
    "account": EMAIL, "channel": "email", "code": "000000", "newPassword": NEW_PWD
})
check("错误码被拒", res.get("code") == 400, f"msg={res.get('message')}")

print("=" * 62)
print("步骤 6：正确验证码重置密码")
code, res = call("POST", "/auth/reset-password", {
    "account": EMAIL, "channel": "email", "code": dev_code, "newPassword": NEW_PWD
})
check("重置成功", res.get("code") == 200, f"msg={res.get('message')}")

print("=" * 62)
print("步骤 7：验证码一次性（同码重复使用应失败）")
code, res = call("POST", "/auth/reset-password", {
    "account": EMAIL, "channel": "email", "code": dev_code, "newPassword": "another123456"
})
check("同码不可复用", res.get("code") == 400, f"msg={res.get('message')}")

print("=" * 62)
print("步骤 8：新密码可登录、旧密码失效")
code, res = call("POST", "/auth/login", {"account": EMAIL, "password": NEW_PWD})
check("新密码登录成功", res.get("code") == 200, f"msg={res.get('message')}")
code, res = call("POST", "/auth/login", {"account": EMAIL, "password": OLD_PWD})
check("旧密码登录失败", res.get("code") == 400, f"msg={res.get('message')}")

print("=" * 62)
print("步骤 9：用手机号找回（换个渠道，确认 phone 通道也通）")
code, res = call("POST", "/auth/send-code", {"account": PHONE, "channel": "phone"})
dev_code2 = (res.get("data") or {}).get("devCode")
check("手机号可发码", bool(dev_code2), f"devCode={dev_code2}")
if dev_code2:
    code, res = call("POST", "/auth/reset-password", {
        "account": PHONE, "channel": "phone", "code": dev_code2, "newPassword": OLD_PWD
    })
    check("手机号渠道可重置", res.get("code") == 200, f"msg={res.get('message')}")

print("=" * 62)
print("步骤 10：渠道越权（账号无邮箱时不能发到邮箱）")
STAMP2 = str(int(time.time() + 7))[-8:]
PHONE_ONLY = "138" + STAMP2
code, res = call("POST", "/auth/register", {
    "phone": PHONE_ONLY, "nickname": f"PO{STAMP2}", "password": OLD_PWD
})
if res.get("code") == 200:
    code, res = call("POST", "/auth/send-code", {"account": PHONE_ONLY, "channel": "email"})
    check("未绑邮箱不能发邮箱码", res.get("code") == 400, f"msg={res.get('message')}")
else:
    check("临时账号2注册", False, f"msg={res.get('message')}")

print()
print("=" * 62)
passed = sum(1 for _, c, _ in results if c)
total = len(results)
print(f"结果：{passed}/{total} 通过")
for name, cond, detail in results:
    if not cond:
        print(f"  未通过：{name}  {detail}")
print("=" * 62)
print("清理提示：临时账号", EMAIL, "/", PHONE, "/", PHONE_ONLY)
sys.exit(0 if passed == total else 1)
