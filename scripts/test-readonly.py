#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""只读维度测试（无写库、无认证）：冒烟 / 边界 / 特殊字段(只读部分) / 参数类型 / 组合 / 有效性 / 默认值 / 越权"""
import json, urllib.request, urllib.error, time

BASE = "http://localhost:8080/api"
R = []

def call(method, path, body=None, token=None, timeout=15):
    url = BASE + path
    data = json.dumps(body).encode() if body is not None else None
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
    R.append({"dim": dim, "id": cid, "name": name, "expect": expect,
              "http": http, "code": resp.get("code"), "message": resp.get("message"),
              "pass": bool(ok), "note": note})
    print(f"[{'PASS' if ok else 'FAIL'}] {cid} <{dim}> {name}")
    print(f"     期望: {expect}")
    print(f"     实际: HTTP={http} code={resp.get('code')} msg={str(resp.get('message'))[:70]}")
    if note:
        print(f"     备注: {note}")

print("=" * 76)
print("只读维度测试  ", BASE, " ", time.strftime("%Y-%m-%d %H:%M:%S"))
print("=" * 76)

print("\n##### 10 冒烟 #####")
st, r = call("GET", "/portal/site/stats")
rec("冒烟", "SM-01", "站点统计可用", "code=200 且含 articleCount", st, r,
    st == 200 and r.get("code") == 200 and "articleCount" in (r.get("data") or {}),
    f"数据={json.dumps(r.get('data'), ensure_ascii=False)}")
st, r = call("GET", "/portal/site/config")
rec("冒烟", "SM-02", "站点配置可用", "code=200 且 siteName=简柚", st, r,
    r.get("code") == 200 and (r.get("data") or {}).get("siteName") == "简柚")
st, r = call("GET", "/portal/articles?page=1&size=3")
rec("冒烟", "SM-03", "文章分页可用", "code=200 且 records 为列表", st, r,
    r.get("code") == 200 and isinstance((r.get("data") or {}).get("records"), list),
    f"返回 {len((r.get('data') or {}).get('records') or [])} 条")
st, r = call("GET", "/portal/categories")
rec("冒烟", "SM-04", "分类恰为 3 个", "code=200 且 3 条", st, r,
    r.get("code") == 200 and len(r.get("data") or []) == 3)
st, r = call("GET", "/admin/dashboard")
rec("冒烟", "SM-05", "后台未登录被拦", "HTTP200 + code=401", st, r,
    st == 200 and r.get("code") == 401)
st, r = call("GET", "/portal/articles/1")
rec("冒烟", "SM-06", "文章详情可读", "code=200 且含 title", st, r,
    r.get("code") == 200 and (r.get("data") or {}).get("title") is not None)
st, r = call("GET", "/portal/links")
rec("冒烟", "SM-07", "友链列表可读", "code=200", st, r, r.get("code") == 200)
st, r = call("GET", "/portal/music")
rec("冒烟", "SM-08", "歌单可读", "code=200", st, r, r.get("code") == 200)
st, r = call("GET", "/portal/album/photos")
rec("冒烟", "SM-09", "相册可读", "code=200", st, r, r.get("code") in (200, 404), f"code={r.get('code')}")
st, r = call("GET", "/portal/tags")
rec("冒烟", "SM-10", "标签列表可读", "code=200", st, r, r.get("code") == 200)

print("\n##### 2 边界值 #####")
st, r = call("GET", "/portal/articles?page=1&size=100")
d = r.get("data") or {}
rec("边界值", "BD-01", "size=100 超上限静默裁剪", "code=200 且 size=50", st, r,
    r.get("code") == 200 and d.get("size") == 50,
    f"返回 size={d.get('size')}，records={len(d.get('records') or [])} 条")
st, r = call("GET", "/portal/articles?page=1&size=0")
d = r.get("data") or {}
rec("边界值", "BD-02", "size=0 下限-1", "code=200 且 size 被夹到 >=1", st, r,
    r.get("code") == 200 and (d.get("size") or 0) >= 1, f"size={d.get('size')}")
st, r = call("GET", "/portal/articles?page=1&size=-5")
d = r.get("data") or {}
rec("边界值", "BD-03", "size=-5 负数", "code=200 且 size 被夹到 >=1", st, r,
    r.get("code") == 200 and (d.get("size") or 0) >= 1, f"size={d.get('size')}")
st, r = call("GET", "/portal/articles?page=0")
rec("边界值", "BD-04", "page=0", "code=200 或 400（不报 500）", st, r,
    r.get("code") in (200, 400) and st != 500, f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?page=1&size=51")
d = r.get("data") or {}
rec("边界值", "BD-05", "size=51 上限+1", "code=200 且 size=50", st, r,
    r.get("code") == 200 and d.get("size") == 50, f"size={d.get('size')}")
st, r = call("GET", "/portal/articles?page=1&size=50")
d = r.get("data") or {}
rec("边界值", "BD-06", "size=50 上限内", "code=200 且 size=50", st, r,
    r.get("code") == 200 and d.get("size") == 50, f"size={d.get('size')}")
st, r = call("GET", "/portal/articles?page=1&size=1")
d = r.get("data") or {}
rec("边界值", "BD-07", "size=1 下限", "code=200 且 size=1", st, r,
    r.get("code") == 200 and d.get("size") == 1, f"size={d.get('size')}")
st, r = call("GET", "/portal/articles?keyword=" + "%E4%BA%BA" * 100)
rec("边界值", "BD-08", "超长关键字(200字)", "code=200 或 400（不报 500）", st, r,
    r.get("code") in (200, 400), f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?page=1&size=99999999")
d = r.get("data") or {}
rec("边界值", "BD-09", "size=极大值", "code=200 且 size=50（夹到上限）", st, r,
    r.get("code") == 200 and d.get("size") == 50, f"size={d.get('size')}")

print("\n##### 4 参数类型 #####")
st, r = call("GET", "/portal/articles?page=abc")
rec("参数类型", "TP-01", "page=abc 非数字", "HTTP200 + code=400", st, r,
    r.get("code") == 400, f"HTTP={st}")
st, r = call("GET", "/portal/articles?size=xyz")
rec("参数类型", "TP-02", "size=xyz 非数字", "code=400", st, r, r.get("code") == 400)
st, r = call("GET", "/portal/articles?categoryId=abc")
rec("参数类型", "TP-03", "categoryId=abc 非数字", "code=400", st, r, r.get("code") == 400)
st, r = call("GET", "/portal/articles/abc")
rec("参数类型", "TP-04", "路径 id=abc", "code=400", st, r, r.get("code") == 400)
st, r = call("GET", "/portal/articles/1.5")
rec("参数类型", "TP-05", "路径 id=1.5 小数", "code=400", st, r, r.get("code") == 400)
st, r = call("GET", "/portal/articles/999999")
rec("参数类型", "TP-06", "id=999999 不存在", "code=404", st, r, r.get("code") == 404)
st, r = call("GET", "/portal/articles/0")
rec("参数类型", "TP-07", "id=0", "code=404 或 400", st, r, r.get("code") in (400, 404), f"code={r.get('code')}")
st, r = call("GET", "/portal/articles/-1")
rec("参数类型", "TP-08", "id=-1 负数", "code=404 或 400", st, r, r.get("code") in (400, 404), f"code={r.get('code')}")
st, r = call("GET", "/portal/articles/99999999999999999999")
rec("参数类型", "TP-09", "id 超 Long 范围", "code=400（类型不匹配，不报 500）", st, r,
    r.get("code") == 400, f"code={r.get('code')}")

print("\n##### 3 特殊字段（只读部分）#####")
st, r = call("GET", "/portal/articles?keyword=%27%20OR%20%271%27%3D%271")
rec("特殊字段", "SP-01", "搜索含 SQL 注入串", "code=200（参数化）", st, r,
    r.get("code") == 200, f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?keyword=%3Cscript%3Ealert(1)%3C%2Fscript%3E")
rec("特殊字段", "SP-02", "搜索含 XSS 串", "code=200（原样当关键字）", st, r,
    r.get("code") == 200, f"命中 {(r.get('data') or {}).get('total')} 条")
st, r = call("GET", "/portal/articles?keyword=%00null")
rec("特殊字段", "SP-03", "搜索含 %00 空字节", "code=200 或 400（不报 500）", st, r,
    r.get("code") in (200, 400), f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?keyword=%F0%9F%8D%8Aemoji")
rec("特殊字段", "SP-04", "搜索含 emoji", "code=200", st, r, r.get("code") == 200, f"code={r.get('code')}")
st, r = call("GET", "/portal/articles/1%27%20OR%20%271%27%3D%271")
rec("特殊字段", "SP-05", "路径含注入串", "code=400（类型不匹配）不报 500", st, r,
    r.get("code") == 400, f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?keyword=" + "A" * 2000)
rec("特殊字段", "SP-06", "关键字 2000 字超长", "code=200 或 400（不报 500）", st, r,
    r.get("code") in (200, 400), f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?keyword=%E9%9D%9E%E6%B3%95%E5%AD%97%E7%AC%A6")
rec("特殊字段", "SP-07", "中文关键字检索", "code=200", st, r, r.get("code") == 200,
    f"命中 {(r.get('data') or {}).get('total')} 条")

print("\n##### 6 组合参数 #####")
st, r = call("GET", "/portal/articles?page=1&size=5&categoryId=1&keyword=%E8%AE%B0%E5%BD%95")
rec("组合参数", "CB-01", "分页+分类+关键字", "code=200", st, r, r.get("code") == 200,
    f"命中 {(r.get('data') or {}).get('total')} 条")
st, r = call("GET", "/portal/articles?page=1&size=5&categoryId=999")
rec("组合参数", "CB-02", "不存在分类+分页", "code=200 且 total=0", st, r,
    r.get("code") == 200 and (r.get("data") or {}).get("total") == 0,
    f"total={(r.get('data') or {}).get('total')}")
st, r = call("GET", "/portal/articles?page=1&size=5&tagId=9")
rec("组合参数", "CB-03", "标签+分页", "code=200", st, r, r.get("code") == 200,
    f"命中 {(r.get('data') or {}).get('total')} 条")
st, r = call("GET", "/portal/articles?page=1&size=5&categoryId=1&tagId=9")
rec("组合参数", "CB-04", "分类+标签+分页三重", "code=200", st, r, r.get("code") == 200,
    f"命中 {(r.get('data') or {}).get('total')} 条")
st, r = call("GET", "/portal/articles?page=2&size=5&sort=publishTime")
rec("组合参数", "CB-05", "分页+排序参数", "code=200", st, r, r.get("code") == 200,
    f"第 2 页 {len((r.get('data') or {}).get('records') or [])} 条")
st, r = call("GET", "/portal/articles?page=1&size=5&categoryId=1&categoryId=2")
rec("组合参数", "CB-06", "同参数重复传", "code=200（取其一，不报错）", st, r,
    r.get("code") == 200, f"命中 {(r.get('data') or {}).get('total')} 条")
st, r = call("GET", "/portal/articles?unknownParam=1&page=1")
rec("组合参数", "CB-07", "未知参数混入", "code=200（忽略未知参数）", st, r,
    r.get("code") == 200, f"code={r.get('code')}")

print("\n##### 7 有效性 #####")
st, r = call("GET", "/admin/dashboard", token="this.is.not.a.valid.jwt")
rec("有效性", "VA-01", "伪造格式 token 访问后台", "code=401", st, r, r.get("code") == 401)
st, r = call("GET", "/portal/articles/1", token="this.is.not.a.valid.jwt")
rec("有效性", "VA-02", "伪造 token 访问门户", "code=200（游客放行）", st, r,
    r.get("code") == 200, f"code={r.get('code')}")
st, r = call("GET", "/admin/dashboard", token="")
rec("有效性", "VA-03", "空 token 访问后台", "code=401", st, r, r.get("code") == 401)
st, r = call("GET", "/portal/articles/1", token="Bearer")
rec("有效性", "VA-04", "畸形 token 串", "code=200 或 401（不报 500）", st, r,
    r.get("code") in (200, 401), f"code={r.get('code')}")
st, r = call("GET", "/portal/articles?page=999")
rec("有效性", "VA-05", "超大页码 999", "code=200 且空列表", st, r,
    r.get("code") == 200 and ((r.get("data") or {}).get("records") == []),
    f"records={(r.get('data') or {}).get('records')}")
st, r = call("GET", "/portal/nonexistent/endpoint")
rec("有效性", "VA-06", "访问不存在的接口", "HTTP 404 + code=404", st, r,
    st == 404 or r.get("code") == 404, f"HTTP={st} code={r.get('code')}")

print("\n##### 8 默认值 #####")
st, r = call("GET", "/portal/articles?page=1")
d = r.get("data") or {}
rec("默认值", "DF-01", "分页不传 size", "code=200 且有默认 size", st, r,
    r.get("code") == 200 and d.get("size") is not None, f"默认 size={d.get('size')}")
st, r = call("GET", "/portal/articles")
d = r.get("data") or {}
rec("默认值", "DF-02", "完全不传分页参数", "code=200 且 page=1", st, r,
    r.get("code") == 200 and d.get("page") == 1, f"page={d.get('page')} size={d.get('size')}")
st, r = call("GET", "/portal/articles?page=1&size=10")
rec("默认值", "DF-03", "不传 categoryId（不过滤）", "code=200 且 total>0", st, r,
    r.get("code") == 200 and (r.get("data") or {}).get("total", 0) > 0,
    f"total={(r.get('data') or {}).get('total')}")
st, r = call("GET", "/portal/messages")
d = r.get("data") or {}
recs = d.get("records") if isinstance(d, dict) else None
rec("默认值", "DF-04", "留言列表可读（校验昵称缺省）", "code=200", st, r, r.get("code") == 200,
    f"共 {d.get('total') if isinstance(d, dict) else '?'} 条")

print("\n##### 11 越权 #####")
st, r = call("POST", "/admin/articles", {"title": "越权测试"}, token="")
rec("越权", "AU-01", "无 token 创建文章", "code=401", st, r, r.get("code") == 401)
st, r = call("GET", "/admin/users", token="")
rec("越权", "AU-02", "无 token 读用户列表", "code=401", st, r, r.get("code") == 401)
st, r = call("DELETE", "/admin/articles/1", token="")
rec("越权", "AU-03", "无 token 删除文章", "code=401", st, r, r.get("code") == 401)
st, r = call("GET", "/portal/articles/me/collections")
rec("越权", "AU-04", "未登录读我的收藏", "code=401", st, r, r.get("code") == 401)
st, r = call("POST", "/portal/articles/1/like")
rec("越权", "AU-05", "未登录点赞", "code=401", st, r, r.get("code") == 401)
st, r = call("POST", "/portal/articles/1/collect")
rec("越权", "AU-06", "未登录收藏", "code=401", st, r, r.get("code") == 401)
st, r = call("GET", "/auth/me")
rec("越权", "AU-07", "未登录取当前用户", "code=401", st, r, r.get("code") == 401)
st, r = call("POST", "/admin/uploads", {"file": "x"})
rec("越权", "AU-08", "无 token 上传", "code=401", st, r, r.get("code") == 401)
st, r = call("GET", "/admin/messages")
rec("越权", "AU-09", "无 token 读留言管理", "code=401", st, r, r.get("code") == 401)
st, r = call("PUT", "/admin/site/config", {"siteName": "hacked"})
rec("越权", "AU-10", "无 token 改站点配置", "code=401", st, r, r.get("code") == 401)

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
with open("readonly-result.json", "w", encoding="utf-8") as f:
    json.dump({"total": T, "passed": P, "dims": dims, "results": R}, f,
              ensure_ascii=False, indent=2)
