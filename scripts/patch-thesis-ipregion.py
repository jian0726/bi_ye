"""把正文 docx 中与 IP 归属地实现相关的 5 处描述改为 ip2region 离线库口径。

用法：
    python scripts/patch-thesis-ipregion.py           # 仅打印定位与改写预览
    python scripts/patch-thesis-ipregion.py --apply   # 写盘
"""
import pathlib
import sys

from docx import Document

ROOT = pathlib.Path(r'D:/quanbudaima/bi_ye_she_ji')
DOCX = ROOT / 'docs/03-正文/毕业设计正文-简柚个人博客系统的设计与实现.docx'

APPLY = '--apply' in sys.argv

# (定位用的旧文本片段, 改写后的完整段落文本, 是否整段替换)
PARA_PATCHES = [
    (
        '引入完整安全框架反而增加理解与调试成本。',
        '在技术选型上，各项方案都围绕“个人站点的真实规模”与“长期可维护”两个约束反复比较。'
        '服务端选择 Spring Boot 3.5.5 而不是传统 SSM，是因为其起步依赖与自动配置可以省去大量 XML 配置，'
        '内嵌容器也让部署简化为一个可执行 jar；持久层选择 MyBatis-Plus 3.5.7 而不是 JPA，'
        '是因为本站查询以“动态条件拼接 + 分页”为主，条件构造器在表达这类查询时更直观，'
        '也不会因对象关系映射的隐式行为产生意料之外的 SQL；对象存储选择自建的 MinIO 8.5.12 而不是公有云 OSS，'
        '是为了让图片与音频完全掌握在自己的服务器上，避免外部依赖与长期费用；'
        '认证选择 JJWT 0.12.6 自行签发与解析令牌，而不引入 Spring Security 的完整过滤器链，'
        '是因为本站权限模型只有游客、注册用户、管理员三级，用拦截器即可覆盖，引入完整安全框架反而增加理解与调试成本。'
        'IP 归属地解析选择 ip2region 3.3.7 的离线数据文件而不是调用免费的在线查询接口，'
        '是因为地域统计属于旁路功能，不应把它的可用性绑定在第三方接口上：'
        '数据文件随项目内置、启动时整体载入内存做二分查找，查询为微秒级且不产生任何外部请求，断网环境下同样可用。',
    ),
    (
        '埋点由拦截器统一写入，业务代码无需关心。',
        '• 埋点由拦截器统一写入，业务代码无需关心；省份在写入时即由内置的 ip2region 离线数据文件解析完成。',
    ),
    (
        '省份分布依据访问来源 IP 解析得到，用于粗略了解访客的地域构成。',
        '仪表盘的数据全部来自真实业务表：文章、留言、用户等数量由聚合查询实时统计，'
        '访问明细来自前台请求时由拦截器异步写入的访问日志，省份分布由内置的 ip2region 离线数据文件'
        '在埋点写入时解析得到（不依赖任何外部接口），用于粗略了解访客的地域构成。'
        '需说明的是，“今日访问”统计的是门户接口的请求次数而非独立访客数，'
        '同一访客刷新页面会使其增长，与文章浏览量的独立访客口径不同。'
        '统计查询只读不写，不会对前台业务产生压力。',
    ),
    (
        '同时记录 IP 属地，供后台按属地核查。',
        '留言与前台文字雨同源：前台飘落的字幅拉取的正是本表数据，因此删除留言会同步移除其在前台字幅流中的展示，'
        '不会留下悬空的引用记录。留言无需登录即可提交，游客留言的 user_id 记为 NULL、昵称缺省为“访客”，'
        '同时记录 IP 属地（由内置 ip2region 离线库解析，回环与内网地址记为“本地”），供后台按属地核查。',
    ),
]

# 表格单元格替换：(旧值片段, 新值)
CELL_PATCHES = [
    ('省份（IP 归属地；本机 / 内网记为', '省份（由 ip2region 离线库解析；本机 / 内网与保留地址记为“本地”）'),
]

# P197 含 Consolas 代码 run（9 个 run），整段替换会丢字体，故按 run 精确改写
RUN_PATCHES = [
    ('）；"今日各省访问分布"按 ', '）；“今日各省访问分布”按 '),
    ('），避免函数计算导致索引失效。',
     '），避免函数计算导致索引失效；province 的值由内置的 ip2region 离线数据文件'
     '（启动时载入内存、二分查找、不产生外部请求）在写入时解析，回环与内网地址统一记为“本地”。'),
]


def set_text(paragraph, text):
    """保留首个 run 的格式，整段替换文本。"""
    if not paragraph.runs:
        paragraph.add_run(text)
        return
    paragraph.runs[0].text = text
    for r in paragraph.runs[1:]:
        r.text = ''


d = Document(str(DOCX))
hits = 0

for old, new in PARA_PATCHES:
    idx = [i for i, p in enumerate(d.paragraphs)
           if old in p.text and not p.style.name.startswith('toc')]
    assert len(idx) == 1, f'“{old}” 匹配 {len(idx)} 段: {idx}'
    i = idx[0]
    print(f'--- P{i} 命中“{old}”')
    print(f'    旧: {d.paragraphs[i].text.strip()[:110]}')
    print(f'    新: {new[:110]}')
    if APPLY:
        set_text(d.paragraphs[i], new)
    hits += 1

for ti, tb in enumerate(d.tables):
    for ri, row in enumerate(tb.rows):
        for ci, cell in enumerate(row.cells):
            if '省份（IP 归属地；本机 / 内网记为' in cell.text:
                print(f'--- T{ti} R{ri} C{ci} 单元格命中')
                print(f'    旧: {cell.text.strip()}')
                print(f'    新: {CELL_PATCHES[0][1]}')
                if APPLY:
                    set_text(cell.paragraphs[0], CELL_PATCHES[0][1])
                    for extra in cell.paragraphs[1:]:
                        set_text(extra, '')
                hits += 1

# P197：按 run 精确替换，保留 Consolas 代码字体
p197 = d.paragraphs[197]
assert '访问趋势按' in p197.text, 'P197 定位失败'
for old, new in RUN_PATCHES:
    done = False
    for r in p197.runs:
        if old in r.text:
            print(f'--- P197 run 命中（字体 {r.font.name}）: {old[:30]}')
            if APPLY:
                r.text = r.text.replace(old, new)
            done = True
            hits += 1
    assert done, f'P197 内未找到 run: {old}'

print(f'\n命中 {hits} 处')
if APPLY:
    d.save(str(DOCX))
    print('已写盘')
else:
    print('（未写盘；确认后加 --apply）')
