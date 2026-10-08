"""同步设计/方案 docx 的浏览量去重口径 + 新表 t_view_record。

用法：python sync-docx-viewcount.py [B C D E ...]   不带参数则全部执行。
A（数据库设计文档）已在首轮执行完毕，如需重跑请显式传入 A（会因已完成而报错，属预期保护）。
"""
import copy
import pathlib
import sys

from docx import Document
from docx.text.paragraph import Paragraph
from docx.table import Table

BASE = pathlib.Path(r'D:/quanbudaima/bi_ye_she_ji/docs')


def set_para_text(p, text):
    if p.runs:
        p.runs[0].text = text
        for r in p.runs[1:]:
            r.text = ''
    else:
        p.add_run(text)


def set_cell_text(cell, text):
    set_para_text(cell.paragraphs[0], text)


def rewrite_para(doc, key, new_text):
    hits = [p for p in doc.paragraphs if key in p.text]
    assert len(hits) == 1, f'段落匹配 {len(hits)} 次: {key[:50]!r}'
    set_para_text(hits[0], new_text)


def rewrite_cell(doc, key, new_text, exact=False):
    hits = [c for t in doc.tables for r in t.rows for c in r.cells
            if (c.text.strip() == key if exact else key in c.text)]
    assert len(hits) == 1, f'单元格匹配 {len(hits)} 次: {key[:60]!r}'
    set_cell_text(hits[0], new_text)


def find_row(table, key, col=0):
    for i, r in enumerate(table.rows):
        if key in r.cells[col].text:
            return i
    raise AssertionError(f'未找到行: {key!r}')


def find_row2(table, c0, c1):
    for i, r in enumerate(table.rows):
        if r.cells[0].text.strip() == c0 and r.cells[1].text.strip() == c1:
            return i
    raise AssertionError(f'未找到行: {c0}/{c1}')


def insert_row_after(table, idx, values):
    tr = copy.deepcopy(table.rows[idx]._tr)
    table.rows[idx]._tr.addnext(tr)
    row = table.rows[idx + 1]
    for c, v in zip(row.cells, values):
        set_cell_text(c, v)
    return row


def clone_after(anchor, ref_p, text):
    new_el = copy.deepcopy(ref_p._p)
    anchor._p.addnext(new_el)
    p = Paragraph(new_el, anchor._parent)
    set_para_text(p, text)
    return p


def clone_table_after(anchor, ref_tbl):
    new_el = copy.deepcopy(ref_tbl._tbl)
    anchor._p.addnext(new_el)
    return Table(new_el, anchor._parent)


VIEW_TABLE = [
    ['字段名', '类型', '长度', '允许空', '键/约束', '默认值', '说明'],
    ['id', 'BIGINT', '20', '否', 'PK', '—', '浏览记录ID'],
    ['article_id', 'BIGINT', '20', '否', 'FK / 联合唯一', '—', '被浏览文章ID'],
    ['visitor_key', 'VARCHAR', '64', '否', '联合唯一', '—', '访客标识：登录用户 u:{userId}；游客 ip:{客户端IP}'],
    ['create_time', 'DATETIME', '—', '否', '—', 'CURRENT_TIMESTAMP', '首次浏览时间'],
]
VIEW_INDEX = '索引：PRIMARY(id)、uk_article_visitor(article_id, visitor_key)（唯一）、idx_article(article_id)'
VIEW_NOTES = [
    '设计说明：',
    '• 本表是浏览量的计数依据：文章详情接口先执行 INSERT IGNORE 写本表，写入成功（影响行数为 1）才让 t_article.view_count + 1；'
    '写入被唯一索引挡下（影响行数为 0）说明该访客已读过，不计数。',
    '• 去重由唯一索引承担，不依赖 Redis 或内存状态，与 t_like_record、t_collect 是同一套做法，天然支持多实例部署与服务重启后状态不丢。',
    '• visitor_key 取法：已登录用户取 u:{userId}（换设备、换网络仍算同一人）；未登录游客取 ip:{客户端IP}，'
    '同一网络出口（宿舍宽带、校园网 NAT）下的多个访客会被合并计一次，这是无登录态站点的常规近似。',
    '• 与 t_visit_log 的区别：后者是站点级访问埋点（拦截器对 /portal/** 异步记录 IP、省份、路径、UA），本表是文章级去重台账，'
    '两者互不替代。表内不预置种子数据，浏览量从真实访问累计。',
]


def stage_a():
    P = BASE / '02-设计文档/数据库设计文档.docx'
    d = Document(str(P))
    rewrite_para(d, '表数量：14 张', '表数量：15 张（另有 3 张预留扩展表，本期未建表，见 3.16）')
    rewrite_para(d, 'idx_view(view_count)',
                 '• idx_view(view_count) — 浏览量排序（原为热门排行预留；该功能已整体移除，索引暂留备用，当前无查询使用）')
    rewrite_para(d, '3.15 预留扩展表（设计保留，本期未建表）', '3.16 预留扩展表（设计保留，本期未建表）')
    rewrite_para(d, '4.3 浏览量计数', '4.3 浏览量计数（独立访客去重）')
    rewrite_para(d, '当前实现为数据库直接自增',
                 '• 浏览量口径为「读过这篇文章的独立访客数」：详情接口先 INSERT IGNORE 写 t_view_record，写入成功才执行 '
                 'UPDATE t_article SET view_count = view_count + 1（setSql 原子自增，InnoDB 行锁保证并发准确）；'
                 '重复访问被唯一索引挡下，不计数——访客反复刷新、浏览器前进后退、调试预览都不会把数字刷高。')
    rewrite_para(d, '之所以未采用"Redis 累加 + 定时批量落库"',
                 '• 去重未采用 Redis SETNX 方案：Redis 重启或清库后去重窗口会整体失效，还会引入「Redis 不可用时是否继续计数」的降级分支；'
                 '本项目规模下数据库唯一索引方案更简单、结果可复现且可核对。同理也未引入定时批量落库，直接更新不存在缓存与库的最终一致性问题，'
                 '也不会出现「刷新后数字不立即变化」的观感。')
    rewrite_para(d, '该字段同时被 idx_view 支撑热门排行使用',
                 '• idx_view 索引保留但当前不参与查询：原设计用于支撑热门文章排行（按 view_count 倒序取 TOP N），'
                 '该功能已于设计后期整体移除（前台未接入、后端接口与 Redis 缓存代码一并删除）。')
    rewrite_para(d, '统计字段（view_count 等）采用冗余存储',
                 '• 统计字段（view_count 等）采用冗余存储，避免每次列表查询都执行 COUNT；更新由服务层在对应业务动作中同步写入。'
                 '其中 view_count 的更新需先通过 t_view_record 的唯一索引完成去重（见 4.3）。')
    t0 = d.tables[0]
    insert_row_after(t0, find_row(t0, 'collect'), ['view_record', 't_view_record', '文章浏览记录表'])
    t16 = [t for t in d.tables if len(t.columns) == 5 and '支撑场景' in t.rows[0].cells[-1].text][0]
    i = find_row2(t16, 'collect', 'idx_article')
    insert_row_after(t16, i, ['view_record', 'uk_article_visitor', 'article_id, visitor_key', '唯一',
                              '浏览量去重（同一访客同一文章只计一次）+ 判断是否已读过'])
    insert_row_after(t16, i + 1, ['view_record', 'idx_article', 'article_id', '普通', '按文章统计去重浏览数、级联清理'])
    t17 = [t for t in d.tables if len(t.columns) == 4 and '预估年增量' in t.rows[0].cells[1].text][0]
    insert_row_after(t17, find_row(t17, 'collect'), ['view_record', '100,000', '~70 B', '~7 MB'])
    for r in t17.rows:
        if r.cells[0].text.strip() == '合计':
            set_cell_text(r.cells[3], '约 197 MB/年')
    music_note = [p for p in d.paragraphs if p.text.strip().startswith('设计说明：全站 BGM 歌单')][0]
    title_ref = [p for p in d.paragraphs if p.text.strip() == '3.14 music — 背景音乐表'][0]
    blank_ref = [p for p in d.paragraphs if p.text.strip() == ''][0]
    index_ref = [p for p in d.paragraphs if p.text.strip().startswith('索引：PRIMARY(id)、idx_music_sort')][0]
    cur = clone_after(music_note, title_ref, '3.15 view_record — 文章浏览记录表')
    tbl = clone_table_after(cur, d.tables[14])
    while len(tbl.rows) > len(VIEW_TABLE):
        tbl._tbl.remove(tbl.rows[-1]._tr)
    for ri, values in enumerate(VIEW_TABLE):
        for ci, v in enumerate(values):
            set_cell_text(tbl.rows[ri].cells[ci], v)
    cur = clone_after(Paragraph(tbl._tbl, music_note._parent), blank_ref, '')
    cur = clone_after(cur, index_ref, VIEW_INDEX)
    for note in VIEW_NOTES:
        cur = clone_after(cur, music_note, note)
    d.save(str(P))
    print('A. 数据库设计文档.docx 已同步')


def stage_b():
    P = BASE / '02-设计文档/需求分析.docx'
    d = Document(str(P))
    rewrite_para(d, '共 14 张表（物理表名统一 t_ 前缀）', '共 15 张表（物理表名统一 t_ 前缀）：')
    rewrite_para(d, '详见数据库设计文档 3.15',
                 '另有 3 张预留扩展表（t_sensitive_word、t_operation_log、t_article_history）已完成结构设计但本期未建表，'
                 '详见数据库设计文档 3.16 与本文 9.2。')
    t29 = [t for t in d.tables if len(t.columns) == 2 and t.rows[0].cells[0].text.strip() == '分类'][0]
    set_cell_text(t29.rows[find_row(t29, '互动')].cells[1], 't_like_record、t_collect、t_view_record、t_message')
    rewrite_cell(d, '现为详情页直接 UPDATE view_count + 1',
                 '现为按独立访客去重（t_view_record 唯一索引 uk_article_visitor + INSERT IGNORE，写入成功才计数）后直接 UPDATE view_count + 1')
    rewrite_cell(d, '改为 Redis Hash 累加 + Spring Task 定时批量落库；热门榜由 SELECT 排序改为 Redis ZSet',
                 '改为 Redis Hash 累加 + Spring Task 定时批量落库；热门排行功能已整体移除（含前端接口与后端缓存代码），'
                 'Redis ZSet 榜不在本期范围')
    d.save(str(P))
    print('B. 需求分析.docx 已同步')


def stage_c():
    P = BASE / '02-设计文档/系统架构设计.docx'
    d = Document(str(P))
    t4 = [t for t in d.tables if '点赞去重' in [r.cells[0].text.strip() for r in t.rows]][0]
    insert_row_after(t4, find_row(t4, '收藏去重'),
                     ['浏览量去重',
                      '唯一索引 uk_article_visitor(article_id, visitor_key) + INSERT IGNORE，写入成功才计数，同一访客终身只计一次',
                      'MySQL、MyBatis-Plus'])
    rewrite_cell(d, '详情页直接 UPDATE view_count = view_count + 1',
                 '详情页按独立访客去重：先 INSERT IGNORE 写 t_view_record，写入成功才 UPDATE view_count + 1'
                 '（唯一索引 + InnoDB 行锁保证并发准确）')
    t6 = d.tables[6]
    r = find_row(t6, 'ViewCountAspect')
    set_cell_text(t6.rows[r].cells[2], '未实现（现为按独立访客去重后直接入库，见数据库设计文档 4.3）')
    # 清理与「热门排行已整体移除」矛盾的旧行
    try:
        hot = find_row(t4, '热门文章')
        t4._tbl.remove(t4.rows[hot]._tr)
        print('   （已移除 T4 中与现状不符的「热门文章」行）')
    except AssertionError:
        pass
    d.save(str(P))
    print('C. 系统架构设计.docx 已同步')


def stage_d():
    P = BASE / '02-设计文档/功能结构图与流程图.docx'
    d = Document(str(P))
    rewrite_cell(d, '未实现，现为直接更新 view_count 并按该列排序取热门',
                 '未实现，现为按独立访客去重（t_view_record.uk_article_visitor + INSERT IGNORE）后更新 view_count；'
                 '原按该列排序取热门的能力已随热门排行功能一并移除')
    d.save(str(P))
    print('D. 功能结构图与流程图.docx 已同步')


def stage_e():
    P = BASE / '01-选题与方案/选题方案与技术方案.docx'
    d = Document(str(P))
    rewrite_para(d, '共 14 张表', '共 15 张表（下表为逻辑名 ↔ 物理表名对照，物理表统一带 t_ 前缀）：')
    rewrite_para(d, '为什么点赞 / 收藏必须登录',
                 '为什么点赞 / 收藏必须登录：点赞与收藏必须先回答"是谁点的"才能去重与回显（见 t_like_record、t_collect 的唯一索引），'
                 '因此必须绑定账号；留言则允许游客试发（仅本页可见、不入库），保持「路过说一句」的低门槛。'
                 '浏览量同样依赖唯一索引去重（t_view_record.uk_article_visitor），但它允许以游客 IP 作为访客标识兜底，'
                 '因此不强制登录——两者都靠唯一索引保证"同一人不重复计数"，差别只在于未登录时还能不能取到身份。')
    rewrite_cell(d, '详情读取时 view_count = view_count + 1 原子自增',
                 '详情读取时先 INSERT IGNORE 写 t_view_record（唯一索引去重），写入成功才 view_count = view_count + 1 原子自增',
                 exact=True)
    rewrite_cell(d, '未实现。改为详情读取时 view_count = view_count + 1 原子自增',
                 '未实现。改为按独立访客去重（t_view_record 唯一索引）后直接 view_count = view_count + 1 原子自增——'
                 '单机场景更简单且不丢数；热门排行功能在设计后期整体移除，Redis ZSet 榜不在本期范围')
    rewrite_cell(d, '14 张表的范式化设计', '15 张表的范式化设计 + 有意冗余计数列 + 唯一索引承担业务约束')
    for t in d.tables:
        if len(t.columns) >= 5 and 't_music' in t.rows[-1].cells[2].text:
            insert_row_after(t, find_row(t, 't_collect', col=2),
                             ['8', 'view_record', 't_view_record', '文章浏览记录表',
                              'uk_article_visitor(article_id, visitor_key) 承担浏览量去重：同一访客对同一篇文章终身只计一次'])
            break
    else:
        raise AssertionError('选题方案：未找到表清单表')
    d.save(str(P))
    print('E. 选题方案与技术方案.docx 已同步')


STAGES = {k: v for k, v in zip('ABCDE', [stage_a, stage_b, stage_c, stage_d, stage_e])}
targets = sys.argv[1:] or list('ABCDE')
for key in targets:
    STAGES[key]()
