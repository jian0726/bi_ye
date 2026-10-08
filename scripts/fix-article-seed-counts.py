"""修正 init.sql 中 t_article 的浏览/点赞/收藏三列种子值，使其与记录表自洽。

- view_count  -> 0（浏览记录表不预置种子，浏览量从真实访问累计）
- like_count  -> t_like_record 中该文章的实际行数
- collect_count -> t_collect 中该文章的实际行数
"""
import re
import pathlib

BASE = pathlib.Path(r'D:/quanbudaima/bi_ye_she_ji')
P = BASE / 'blog-backend/src/main/resources/db/init.sql'
s = P.read_text(encoding='utf-8')


def count_by_article(insert_marker: str) -> dict:
    i = s.index(insert_marker)
    j = s.index(';', i)
    seg = s[i:j]
    cnt: dict = {}
    for m in re.finditer(r'^\((\d+), (\d+), (\d+),', seg, re.M):
        aid = int(m.group(3))
        cnt[aid] = cnt.get(aid, 0) + 1
    return cnt


likes = count_by_article('INSERT INTO t_like_record')
collects = count_by_article('INSERT INTO t_collect')
print('t_like_record ->', likes)
print('t_collect     ->', collects)

TAIL = re.compile(
    r", '(\d+)', '(\d+)', '(\d+)', '(\d+)', '(\d+)', '(\d+)', '(\d+)', '(\d+)',"
    r" '(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', '(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})',"
    r" ('(?:\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})'|NULL)\)([,;]?)$",
    re.M)

i = s.index('INSERT INTO t_article')
j = s.index('CREATE TABLE t_article_tag')
head, seg, tail = s[:i], s[i:j], s[j:]

starts = [(int(m.group(1)), m.start()) for m in re.finditer(r"^\('(\d+)', ", seg, re.M)]
print('t_article 记录数 =', len(starts))

out, prev, replaced, unmatched = [], 0, 0, []
for k, (aid, st) in enumerate(starts):
    out.append(seg[prev:st])
    nxt = starts[k + 1][1] if k + 1 < len(starts) else len(seg)
    piece = seg[st:nxt]

    def fn(m, aid=aid):
        g = m.groups()
        replaced_local.append(aid)
        return (f", '{g[0]}', '{g[1]}', '{g[2]}', '0', "
                f"'{likes.get(aid, 0)}', '{collects.get(aid, 0)}', '{g[6]}', '{g[7]}', "
                f"'{g[8]}', '{g[9]}', {g[10]}){g[11]}")

    replaced_local: list = []
    out.append(TAIL.sub(fn, piece))
    replaced += len(replaced_local)
    if not replaced_local:
        unmatched.append((aid, piece.rstrip()[-200:]))
    prev = nxt
out.append(seg[prev:])

print('实际替换条数 =', replaced)
if unmatched:
    for aid, tail_text in unmatched:
        print('--- 未匹配 id =', aid)
        print(repr(tail_text))
    raise SystemExit('存在未匹配记录，未写回，请先核对')
assert replaced == len(starts), '替换条数与记录数不一致，已中止'

P.write_text(head + ''.join(out) + tail, encoding='utf-8')
print('已写回', P)
