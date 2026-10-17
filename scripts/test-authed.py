#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""需登录态/写库的维度测试：正常流程 / 必选参数 / 写库特殊字段 / 排重 / 默认值(注册)
一次性数据，跑完输出清理 SQL"""
import json, urllib.request, urllib.error, time, uuid

BASE = "http://localhost:8080/api"
R = []
TS = str(int(time.time()))
U1 = {}   # 主测试用户

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
        return 0, {"code": None, "message": f"{type(e).__name__}: {e}"}

def rec(dim, cid, name, expect, http, resp, ok, note=""):
    R.append({"dim": dim, "id": cid, "name": name, "expect": expect, "http": http,
              "code": resp.get("code"), "message": resp.get("message"),
              "pass": bool(ok), "note": note})
    print(f"[{'PASS' if ok else 'FAIL'}] {cid} <{dim}> {name}")
    print(f"     期望: {expect}")
    print(f"     实际: HTTP={http} code={resp.get('code')} msg={str(resp.get('message'))[:70]}")
    if note:
        print(f"     备注: {note}")

# 一次性账号（排重测试需要固定值）
EMAIL1 = f"smoke{TS}@example.com"
EMAIL2 = f"smokeB{TS}@example.com"
PHONE1 = "139" + TS[-8:]
NICK = "冒烟测试"
PWD = "Test123456"
MSG_IDS = []
USER_IDS = []

print("=" * 76)
print("登录态/写库维度测试  ", BASE)
print("一次性标识:", TS, " 邮箱:", EMAIL1, " 手机:", PHONE1)
print("=" * 76)

# ================= 1 正常流程 =================
print("\n##### 1 正常流程覆盖 #####")
st, r = call("POST", "/auth/register", {"email": EMAIL1, "nickname": NICK, "password": PWD})
d = r.get("data") or {}
ok = r.get("code") == 200 and d.get("token") and d.get("refreshToken")
if ok:
    USER_IDS.append(d.get("userId"))
    U1 = {"id": d.get("userId"), "token": d.get("token"), "refresh": d.get("refreshToken"),
          "role": d.get("role")}
rec("正常流程", "NF-01", "注册新用户（邮箱+昵称+密码）", "code=200 且返回 access+refresh 双 token",
    st, r, bool(ok), f"userId={d.get('userId')} role={d.get('role')}")

st, r = call("POST", "/auth/login", {"account": EMAIL1, "password": PWD})
d = r.get("data") or {}
ok = r.get("code") == 200 and d.get("token") and d.get("refreshToken")
rec("正常流程", "NF-02", "用邮箱登录", "code=200 且返回双 token", st, r, bool(ok),
    f"role={d.get('role')}")
if ok:
    U1["token"] = d.get("token"); U1["refresh"] = d.get("refreshToken")

st, r = call("POST", "/auth/login", {"account": PHONE1, "password": PWD})
rec("正常流程", "NF-03", "用未注册手机号登录", "code=400 账号或密码错误", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])

st, r = call("GET", "/auth/me", token=U1.get("token"))
rec("正常流程", "NF-04", "取当前用户", "code=200 且 userId 一致", st, r,
    r.get("code") == 200 and (r.get("data") or {}).get("userId") == U1.get("id"),
    f"userId={(r.get('data') or {}).get('userId')}")

st, r = call("POST", "/auth/refresh", {"refreshToken": U1.get("refresh")})
d = r.get("data") or {}
ok = r.get("code") == 200 and d.get("token")
old_refresh = U1.get("refresh")
rec("正常流程", "NF-05", "refresh 换发新双 token（轮换）", "code=200 且新 refresh != 旧 refresh",
    st, r, bool(ok) and d.get("refreshToken") != old_refresh,
    f"新 refresh 与旧不同={d.get('refreshToken') != old_refresh}")
if ok:
    U1["token"] = d.get("token"); U1["refresh"] = d.get("refreshToken")

st, r = call("POST", "/auth/refresh", {"refreshToken": old_refresh})
rec("正常流程", "NF-06", "旧 refresh 已被轮换撤销（防重放）", "code!=200", st, r,
    r.get("code") != 200, f"重放旧 refresh → code={r.get('code')} {str(r.get('message'))[:40]}")

st, r = call("GET", "/portal/articles/1", token=U1.get("token"))
d = r.get("data") or {}
rec("正常流程", "NF-07", "登录态读文章详情带 liked/collected", "code=200 且 liked/collected 为布尔",
    st, r, r.get("code") == 200 and isinstance(d.get("liked"), bool) and isinstance(d.get("collected"), bool),
    f"liked={d.get('liked')} collected={d.get('collected')}")

st, r = call("PUT", "/auth/profile", {"nickname": "冒烟改名", "bio": "测试简介"}, token=U1.get("token"))
rec("正常流程", "NF-08", "修改个人资料", "code=200 且昵称已更新", st, r,
    r.get("code") == 200 and (r.get("data") or {}).get("nickname") == "冒烟改名",
    f"nickname={(r.get('data') or {}).get('nickname')}")

st, r = call("PUT", "/auth/profile", {"nickname": NICK, "bio": "测试简介"}, token=U1.get("token"))
rec("正常流程", "NF-09", "资料改回", "code=200", st, r, r.get("code") == 200)

st, r = call("POST", "/auth/logout", {"refreshToken": U1.get("refresh")})
rec("正常流程", "NF-10", "登出撤销 refresh", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:40])
revoked = U1.get("refresh")
st, r = call("POST", "/auth/refresh", {"refreshToken": revoked})
rec("正常流程", "NF-11", "登出后 refresh 已失效", "code!=200", st, r, r.get("code") != 200,
    f"code={r.get('code')}")

# 重新登录取可用 token
st, r = call("POST", "/auth/login", {"account": EMAIL1, "password": PWD})
d = r.get("data") or {}
if r.get("code") == 200:
    U1["token"] = d.get("token"); U1["refresh"] = d.get("refreshToken")
TK = U1.get("token")
rec("正常流程", "NF-12", "重新登录获取 token（后续用例前置）", "code=200", st, r,
    r.get("code") == 200)

# ================= 5 必选参数 =================
print("\n##### 5 必选参数校验 #####")
cases = [
    ("RQ-01", "注册缺邮箱与手机号", "/auth/register", {"nickname": NICK, "password": PWD}, "code=400"),
    ("RQ-02", "注册缺昵称", "/auth/register", {"email": EMAIL2, "password": PWD}, "code=400 请输入昵称"),
    ("RQ-03", "注册缺密码", "/auth/register", {"email": EMAIL2, "nickname": NICK}, "code=400 请输入密码"),
    ("RQ-04", "登录缺 account", "/auth/login", {"password": PWD}, "code=400 请输入账号"),
    ("RQ-05", "登录缺 password", "/auth/login", {"account": EMAIL1}, "code=400 请输入密码"),
    ("RQ-06", "留言缺 content", "/portal/messages", {"nickname": "路人"}, "code=400 留言内容不能为空"),
    ("RQ-07", "refresh 缺 refreshToken", "/auth/refresh", {}, "code=400"),
    ("RQ-08", "空 JSON 体登录", "/auth/login", {}, "code=400"),
    ("RQ-09", "申请友链缺站点名", "/portal/links/apply", {"url": "https://x.com"}, "code=400"),
    ("RQ-10", "申请友链缺站点地址", "/portal/links/apply", {"name": "测试站"}, "code=400"),
]
for cid, name, path, body, expect in cases:
    st, r = call("POST", path, body)
    rec("必选参数", cid, name, expect, st, r, r.get("code") == 400, str(r.get("message"))[:60])

# ================= 2 边界值（写库部分）=================
print("\n##### 2 边界值（写库部分）#####")
def reg(tag, email, nick, pwd):
    st, r = call("POST", "/auth/register", {"email": email, "nickname": nick, "password": pwd})
    if r.get("code") == 200:
        USER_IDS.append((r.get("data") or {}).get("userId"))
    return st, r

st, r = reg("", f"bd1{TS}@example.com", "边界一", "12345")
rec("边界值", "BD-10", "密码 5 位（下限-1）", "code=400 密码 6-32 位", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = reg("", f"bd2{TS}@example.com", "边界二", "1" * 6)
rec("边界值", "BD-11", "密码 6 位（下限，应通过）", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:60])
st, r = reg("", f"bd3{TS}@example.com", "边界三", "1" * 32)
rec("边界值", "BD-12", "密码 32 位（上限，应通过）", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:60])
st, r = reg("", f"bd4{TS}@example.com", "边界四", "1" * 33)
rec("边界值", "BD-13", "密码 33 位（上限+1）", "code=400 密码 6-32 位", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = reg("", f"bd5{TS}@example.com", "一", PWD)
rec("边界值", "BD-14", "昵称 1 字（下限-1）", "code=400 昵称 2-20 个字符", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = reg("", f"bd6{TS}@example.com", "二二", PWD)
rec("边界值", "BD-15", "昵称 2 字（下限，应通过）", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:60])
st, r = reg("", f"bd7{TS}@example.com", "名" * 20, PWD)
rec("边界值", "BD-16", "昵称 20 字（上限，应通过）", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:60])
st, r = reg("", f"bd8{TS}@example.com", "名" * 21, PWD)
rec("边界值", "BD-17", "昵称 21 字（上限+1）", "code=400 昵称 2-20 个字符", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])

# 留言边界 300/301
st, r = call("POST", "/portal/messages", {"content": "边" * 300, "nickname": "边界"})
mid = (r.get("data") or {}).get("id") if isinstance(r.get("data"), dict) else None
if mid:
    MSG_IDS.append(mid)
rec("边界值", "BD-18", "留言内容 300 字（上限，应通过）", "code=200", st, r,
    r.get("code") == 200, f"id={mid}")
st, r = call("POST", "/portal/messages", {"content": "边" * 301, "nickname": "边界"})
rec("边界值", "BD-19", "留言内容 301 字（上限+1）", "code=400 留言内容最多 300 字",
    st, r, r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/portal/messages", {"content": "昵称边界", "nickname": "名" * 21})
rec("边界值", "BD-20", "留言昵称 21 字（上限+1）", "code=400 昵称最多 20 字", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/portal/messages", {"content": "昵称边界", "nickname": "名" * 20})
mid = (r.get("data") or {}).get("id") if isinstance(r.get("data"), dict) else None
if mid:
    MSG_IDS.append(mid)
rec("边界值", "BD-21", "留言昵称 20 字（上限，应通过）", "code=200", st, r,
    r.get("code") == 200, f"id={mid}")

# ================= 3 特殊字段（写库）=================
print("\n##### 3 特殊字段（写库部分）#####")
st, r = call("POST", "/portal/messages", {"content": "<script>alert(1)</script>", "nickname": "测试"})
d = r.get("data") or {}
mid = d.get("id") if isinstance(d, dict) else None
if mid:
    MSG_IDS.append(mid)
rec("特殊字段", "SP-08", "留言含 <script> 标签", "code=200（落库成功）", st, r, r.get("code") == 200,
    f"id={mid}")
if mid:
    st2, r2 = call("GET", f"/portal/messages?page=1&size=50")
    recs = (r2.get("data") or {}).get("records") or []
    stored = next((x.get("content") for x in recs if x.get("id") == mid), "未找到")
    rec("特殊字段", "SP-09", "回读 XSS 内容（确认存储行为）",
        "后端按原样存储（HTML 转义由前端 markdown.ts 负责，属预期）", st2, r2,
        stored != "未找到", f"落库内容={str(stored)[:60]}")

st, r = call("POST", "/portal/messages", {"content": "' OR '1'='1' -- ", "nickname": "测试"})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("id"):
    MSG_IDS.append(d.get("id"))
rec("特殊字段", "SP-10", "留言含 SQL 注入串", "code=200（参数化，库不受影响）", st, r,
    r.get("code") == 200, f"id={d.get('id') if isinstance(d, dict) else '?'}")

st, r = call("POST", "/portal/messages", {"content": "emoji 🍊🔥 与转义 \\ \" '", "nickname": "测试"})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("id"):
    MSG_IDS.append(d.get("id"))
rec("特殊字段", "SP-11", "留言含 emoji + 转义字符", "code=200（UTF-8 正常落库）", st, r,
    r.get("code") == 200, f"id={d.get('id') if isinstance(d, dict) else '?'}")

st, r = call("POST", "/portal/messages", {"content": "   ", "nickname": "测试"})
rec("特殊字段", "SP-12", "留言内容为纯空格", "code=400（@NotBlank 拦截空白）", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/portal/messages", {"content": "特殊字段测试", "nickname": "  "})
rec("特殊字段", "SP-13", "留言昵称为空白", "code=200 或 400（不报 500）", st, r,
    r.get("code") in (200, 400), f"code={r.get('code')}")
if r.get("code") == 200 and isinstance(r.get("data"), dict) and r["data"].get("id"):
    MSG_IDS.append(r["data"]["id"])

st, r = call("POST", "/auth/register", {"email": "not-an-email", "nickname": NICK, "password": PWD})
rec("特殊字段", "SP-14", "注册邮箱格式非法", "code=400 邮箱格式不正确", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/auth/register", {"phone": "12345678901", "nickname": NICK, "password": PWD})
rec("特殊字段", "SP-15", "注册手机号格式非法（12 开头）", "code=400 手机号格式不正确", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/auth/register", {"phone": "13912345678", "email": "bad@@x", "nickname": NICK, "password": PWD})
rec("特殊字段", "SP-16", "手机合法但邮箱非法（组合）", "code=400 邮箱格式不正确", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])

# ================= 4 参数类型（写库）=================
print("\n##### 4 参数类型（写库）#####")
st, r = call("POST", "/auth/login", {"account": 12345, "password": PWD})
rec("参数类型", "TP-10", "account 传数字类型", "code=400（类型不匹配/账号密码错误）", st, r,
    r.get("code") == 400, f"code={r.get('code')} msg={str(r.get('message'))[:50]}")
st, r = call("POST", "/auth/login", {"account": EMAIL1, "password": 123456})
rec("参数类型", "TP-11", "password 传数字类型", "code=400", st, r, r.get("code") == 400,
    f"code={r.get('code')} msg={str(r.get('message'))[:50]}")
st, r = call("POST", "/portal/messages", {"content": {"nested": "obj"}, "nickname": "测试"})
rec("参数类型", "TP-12", "content 传对象类型", "code=400（不报 500）", st, r,
    r.get("code") == 400, f"code={r.get('code')}")
st, r = call("POST", "/portal/messages", {"content": ["array"], "nickname": "测试"})
rec("参数类型", "TP-13", "content 传数组类型", "code=400（不报 500）", st, r,
    r.get("code") == 400, f"code={r.get('code')}")

# ================= 6 组合参数（写库）=================
print("\n##### 6 组合参数（写库）#####")
st, r = call("POST", "/portal/messages", {"content": "组合参数测试", "nickname": "组合", "email": "not-an-email"})
rec("组合参数", "CB-08", "留言含非法邮箱", "code=400 邮箱格式不正确", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/portal/messages", {"content": "组合参数测试", "nickname": "组合", "email": "ok@example.com"})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("id"):
    MSG_IDS.append(d.get("id"))
rec("组合参数", "CB-09", "留言含合法邮箱", "code=200", st, r, r.get("code") == 200,
    f"id={d.get('id') if isinstance(d, dict) else '?'}")
st, r = call("POST", "/portal/messages", {"content": "x" * 300, "nickname": "名" * 20, "email": "a@b.com"})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("id"):
    MSG_IDS.append(d.get("id"))
rec("组合参数", "CB-10", "留言三字段同时取上限", "code=200（content300+昵称20+合法邮箱）",
    st, r, r.get("code") == 200, f"id={d.get('id') if isinstance(d, dict) else '?'}")
st, r = call("POST", "/portal/links/apply", {"name": "测试站", "url": "https://example.com", "email": "bad"})
rec("组合参数", "CB-11", "友链申请邮箱非法", "code=200 或 400（该字段无 @Email 校验则为 200）",
    st, r, r.get("code") in (200, 400), f"code={r.get('code')} msg={str(r.get('message'))[:50]}")

# ================= 7 有效性（写库）=================
print("\n##### 7 有效性（写库）#####")
st, r = call("POST", "/auth/login", {"account": EMAIL1, "password": "wrongpwd123"})
rec("有效性", "VA-07", "登录密码错误", "code=400 账号或密码错误", st, r, r.get("code") == 400,
    str(r.get("message"))[:60])
st, r = call("POST", "/auth/login", {"account": "nobody@example.com", "password": PWD})
rec("有效性", "VA-08", "账号不存在", "code=400（不泄露账号是否存在）", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])
st, r = call("POST", "/auth/refresh", {"refreshToken": "invalid.token"})
rec("有效性", "VA-09", "refresh 传非法 token", "code!=200", st, r, r.get("code") != 200,
    f"code={r.get('code')}")
st, r = call("POST", "/auth/refresh", {"refreshToken": TK})
rec("有效性", "VA-10", "用 access 冒充 refresh", "code!=200（type 隔离）", st, r,
    r.get("code") != 200, f"code={r.get('code')} msg={str(r.get('message'))[:50]}")
st, r = call("POST", "/auth/logout", {"refreshToken": "invalid.token"})
rec("有效性", "VA-11", "登出传非法 token（应幂等）", "code=200（幂等不报错）", st, r,
    r.get("code") == 200, f"code={r.get('code')}")

# ================= 8 默认值（写库）=================
print("\n##### 8 默认值（写库）#####")
st, r = call("POST", "/auth/register", {"email": f"df{TS}@example.com", "nickname": "默认测试", "password": PWD})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("userId"):
    USER_IDS.append(d.get("userId"))
rec("默认值", "DF-05", "注册用户默认角色", "code=200 且 role=USER（非 ADMIN）", st, r,
    r.get("code") == 200 and d.get("role") == "USER", f"role={d.get('role')}")
st, r = call("POST", "/auth/register", {"email": f"df2{TS}@example.com", "nickname": "默认测试2", "password": PWD})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("userId"):
    USER_IDS.append(d.get("userId"))
rec("默认值", "DF-06", "注册不传性别等选填字段", "code=200（选填缺省不报错）", st, r,
    r.get("code") == 200, f"code={r.get('code')}")

# ================= 9 排重 =================
print("\n##### 9 排重校验 #####")
st, r = call("POST", "/auth/register", {"email": EMAIL1, "nickname": "重复注册", "password": PWD})
rec("排重", "DP-01", "邮箱重复注册", "code=400 该邮箱已被注册", st, r, r.get("code") == 400,
    str(r.get("message"))[:60])
st, r = call("POST", "/auth/register", {"phone": PHONE1, "nickname": "重复注册", "password": PWD})
rec("排重", "DP-02", "手机号重复注册", "code=400 该手机号已被注册", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])

st, r = call("POST", "/auth/register", {"phone": "139" + TS[-8:], "nickname": "组合唯一", "password": PWD})
d = r.get("data") or {}
if isinstance(d, dict) and d.get("userId"):
    USER_IDS.append(d.get("userId"))
rec("排重", "DP-03", "手机号+邮箱都传（手机唯一）", "code=200", st, r, r.get("code") == 200,
    str(r.get("message"))[:60])
st, r = call("POST", "/auth/register", {"phone": "139" + TS[-8:], "email": f"dup{TS}@example.com",
                                        "nickname": "重复手机", "password": PWD})
rec("排重", "DP-04", "再次用同一手机号注册", "code=400 该手机号已被注册", st, r,
    r.get("code") == 400, str(r.get("message"))[:60])

first = call("POST", "/portal/articles/1/like", token=TK)
second = call("POST", "/portal/articles/1/like", token=TK)
rec("排重", "DP-05", "同一用户重复点赞（唯一索引幂等）",
    "两次均 code=200 且不产生重复记录", second[0], second[1],
    first[1].get("code") == 200 and second[1].get("code") == 200,
    f"第一次={first[1].get('code')} 第二次={second[1].get('code')} msg={str(second[1].get('message'))[:40]}")

first = call("POST", "/portal/articles/1/collect", token=TK)
second = call("POST", "/portal/articles/1/collect", token=TK)
rec("排重", "DP-06", "同一用户重复收藏（uk_user_article 幂等）",
    "两次均 code=200", second[0], second[1],
    first[1].get("code") == 200 and second[1].get("code") == 200,
    f"第一次={first[1].get('code')} 第二次={second[1].get('code')} msg={str(second[1].get('message'))[:40]}")

st, r = call("POST", "/portal/articles/1/like", token=TK)
rec("排重", "DP-07", "第三次点赞仍幂等", "code=200", st, r, r.get("code") == 200,
    f"code={r.get('code')}")

# 浏览量去重（同一 IP 同一文章只计一次）
v = []
for i in range(3):
    st, r = call("GET", "/portal/articles/2")
    v.append((r.get("data") or {}).get("viewCount"))
rec("排重", "DP-08", "同一访客 3 次访问同一文章", "viewCount 三次一致（只计一次）",
    st, r, len(set(v)) == 1, f"三次 viewCount={v}")

# ================= 11 越权（登录态）=================
print("\n##### 11 越权（普通用户 token）#####")
st, r = call("GET", "/admin/dashboard", token=TK)
rec("越权", "AU-11", "普通用户 token 访问后台仪表盘", "code=403 无权限访问管理后台", st, r,
    r.get("code") == 403, str(r.get("message"))[:60])
st, r = call("GET", "/admin/users", token=TK)
rec("越权", "AU-12", "普通用户读用户列表", "code=403", st, r, r.get("code") == 403,
    str(r.get("message"))[:60])
st, r = call("POST", "/admin/articles", {"title": "越权发文章"}, token=TK)
rec("越权", "AU-13", "普通用户创建文章", "code=403", st, r, r.get("code") == 403,
    str(r.get("message"))[:60])
st, r = call("DELETE", "/admin/articles/1", token=TK)
rec("越权", "AU-14", "普通用户删除文章", "code=403", st, r, r.get("code") == 403,
    str(r.get("message"))[:60])
st, r = call("POST", "/admin/uploads", token=TK)
rec("越权", "AU-15", "普通用户上传素材", "code=403 或 400", st, r,
    r.get("code") in (400, 403), f"code={r.get('code')} msg={str(r.get('message'))[:40]}")
st, r = call("GET", "/portal/articles/me/collections", token=TK)
rec("越权", "AU-16", "登录用户读自己的收藏（应放行）", "code=200", st, r,
    r.get("code") == 200, f"共 {len(r.get('data') or []) if isinstance(r.get('data'), list) else '?'} 条")

print("\n" + "=" * 76)
dims = {}
for x in R:
    d = dims.setdefault(x["dim"], {"t": 0, "p": 0})
    d["t"] += 1
    d["p"] += 1 if x["pass"] else 0
T = len(R); P = sum(1 for x in R if x["pass"])
print(f"总用例 {T}  通过 {P}  未通过 {T-P}  通过率 {P/T*100:.1f}%")
print(f"{'维度':<10}{'总数':<6}{'通过':<6}{'失败':<6}")
for k, v in dims.items():
    print(f"{k:<10}{v['t']:<6}{v['p']:<6}{v['t']-v['p']:<6}")
print("-" * 76)
for x in R:
    if not x["pass"]:
        print(f"[{x['id']}] <{x['dim']}> {x['name']}")
        print(f"   期望: {x['expect']}")
        print(f"   实际: HTTP={x['http']} code={x['code']} msg={str(x['message'])[:80]}")
        if x["note"]:
            print(f"   备注: {x['note']}")
print("=" * 76)
print("\n【清理】一次性数据标识:", TS)
print("用户 ID 列表:", USER_IDS)
print("留言 ID 列表:", MSG_IDS)
with open("authed-result.json", "w", encoding="utf-8") as f:
    json.dump({"total": T, "passed": P, "dims": dims, "results": R,
               "ts": TS, "user_ids": USER_IDS, "message_ids": MSG_IDS,
               "test_email": EMAIL1, "test_phone": PHONE1}, f,
              ensure_ascii=False, indent=2)
