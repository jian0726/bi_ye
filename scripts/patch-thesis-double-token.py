"""正文 docx 增量修订：同步双 Token 会话机制（2026-10-07）。

修订点（按段落文本精确定位，避免索引漂移）：
  1. 段 91  — 架构总述：JWT 单令牌 → 双 Token
  2. 段 317 — 登录页：补 refresh + 存 localStorage 两个键
  3. 段 318 — 令牌无状态段：重写为双 Token 轮换 + Redis 撤销（原「无状态/不保存会话」已过时）
  4. 段 319 — 原为空段，插入「静默续期」新段（保持章节结构）
  5. 段 455-460 — 代码节选：jwtUtil.parse → jwtUtil.parseAccess，标题补说明

操作规程（踩坑记录）：先 save() 落盘，再重新 Document(P) 打开复查。
"""
import shutil
import docx
from docx.shared import Pt

P = r"docs/03-正文/毕业设计正文-简柚个人博客系统的设计与实现.docx"

# ---------- 修订文本 ----------
T91_OLD = "鉴权采用 JWT 无状态方案：登录成功后签发令牌，前端保存并在请求头中携带，服务端拦截器解析令牌后将用户身份写入 ThreadLocal 上下文，避免每个接口重复解析。"
T91_NEW = ("鉴权采用双 Token 方案：登录成功后同时签发 access 令牌（短期，默认 2 小时，携带用户标识与角色声明）"
           "与 refresh 令牌（长期，默认 7 天，仅用于换发 access）；前端将两者保存于 localStorage，"
           "请求头中只携带 access，服务端拦截器解析后将用户身份写入 ThreadLocal 上下文，避免每个接口重复解析。")

T317_OLD = "比对 BCrypt 密文后签发 JWT 令牌，令牌主体为用户 id 并附带角色声明；前端将令牌保存于 localStorage，后续请求统一在请求拦截器中附带。"
T317_NEW = ("比对 BCrypt 密文后签发一对抗衡的令牌——access 令牌主体为用户 id 并附带角色声明，"
            "refresh 令牌附带唯一标识 jti 但不下发角色；前端将两者分别保存于 localStorage，"
            "后续请求统一在请求拦截器中附带 access 令牌。")

T318_NEW = ("令牌的续期与撤销采用「短效 access + 长效 refresh + 服务端登记」的组合。access 令牌到期后，"
            "前端拦截到未登录的业务错误码，自动用 refresh 令牌调用换发接口取得新的一对令牌，并重放原请求，"
            "整个过程对用户无感知。refresh 令牌的 jti 在签发时登记于 Redis 并设置与令牌同寿的过期时间，"
            "换发时先撤销旧 jti 再签发新的一对，形成轮换——同一个 refresh 令牌只能使用一次。"
            "退出登录时撤销其 jti 登记，令牌即刻失效。换发过程中角色与账号状态会重新从数据库读取，"
            "因此管理员被降权或账号被禁用后无需等待令牌自然过期，下一次换发即生效。"
            "该设计只对 refresh 令牌做服务端登记，access 令牌仍保持无状态校验，兼顾了可撤销性与每次请求的解析开销。")

T319_NEW = ("在令牌有效期上，系统把 access 设置为 2 小时、refresh 设置为 7 天，两者均可通过配置文件调整。"
            "较短的 access 有效期把「令牌泄露后可被利用的时间窗」压到最小，而不必为 access 维护服务端黑名单；"
            "较长的 refresh 有效期则避免用户频繁重新登录。这一取舍与系统「Redis 只承担天然带有效期的数据、"
            "不缓存任何业务查询结果」的原则保持一致。")

CODE_TITLE_OLD = "2. JWT 鉴权拦截器：令牌解析与后台强制校验（AuthInterceptor.java 节选）"
CODE_TITLE_NEW = "2. JWT 鉴权拦截器：仅认 access 令牌，解析后强制校验后台权限（AuthInterceptor.java 节选）"

CODE_PARSE_OLD = "Claims claims = token != null ? jwtUtil.parse(token) : null;"
CODE_PARSE_NEW = " Claims claims = token != null ? jwtUtil.parseAccess(token) : null;"


def shift_left(p, twips=0):
    """让代码块段落左对齐，避免继承正文的 2 字符首行缩进。"""
    try:
        p.paragraph_format.first_line_indent = Pt(0)
    except Exception:
        pass


def set_text_keep_run(p, new_text):
    """保留段落首个 run 的格式，替换整段文本。"""
    if not p.runs:
        p.add_run(new_text)
        return
    p.runs[0].text = new_text
    for r in p.runs[1:]:
        r.text = ""


def main():
    d = docx.Document(P)
    cache = d.paragraphs           # 缓存：docx.paragraphs 每次访问都重建
    hits = {}

    for i, p in enumerate(cache):
        t = p.text.strip()
        # 用「句子片段」定位，避免引号/全半角差异导致整段匹配失败
        if "鉴权采用 JWT 无状态方案" in t:
            set_text_keep_run(p, T91_NEW); hits["91"] = i
        elif "比对 BCrypt 密文后签发 JWT 令牌" in t:
            set_text_keep_run(p, T317_NEW); hits["317"] = i
        elif t.startswith("令牌采用无状态设计"):
            set_text_keep_run(p, T318_NEW); hits["318"] = i
        elif t.startswith("令牌的续期与撤销采用"):
            hits["318"] = i          # 已改过，幂等：仅记录位置供 319 定位
        elif t == CODE_TITLE_OLD:
            set_text_keep_run(p, CODE_TITLE_NEW); hits["code_title"] = i
        elif t.startswith("2. JWT 鉴权拦截器：仅认 access"):
            hits["code_title"] = i   # 幂等
        elif t == CODE_PARSE_OLD:
            set_text_keep_run(p, CODE_PARSE_NEW); hits["code_parse"] = i
        elif "parseAccess" in t:
            hits["code_parse"] = i   # 幂等

    print("命中：", hits)

    # 段 319（318 的下一段）原为空段 —— 用作「有效期」补充段
    idx318 = hits.get("318")
    if idx318 is not None:
        blank = cache[idx318 + 1]
        if not blank.text.strip():
            set_text_keep_run(blank, T319_NEW)
            print(f"[319] 填充新段落（原为空段）")
        elif blank.text.strip().startswith("在令牌有效期上"):
            print(f"[319] 已是新版，跳过")
        else:
            print(f"⚠ 段 {idx318+1} 非空，跳过插入：{blank.text[:60]}")
    d.save(P)
    print("已落盘")

    # 复查
    d2 = docx.Document(P)
    print("\n=== 复查 ===")
    for key, i in hits.items():
        print(f"[{key}] {d2.paragraphs[i].text[:110]}")
    if idx318 is not None:
        print(f"[319] {d2.paragraphs[idx318+1].text[:110]}")


if __name__ == "__main__":
    main()
