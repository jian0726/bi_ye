# -*- coding: utf-8 -*-
"""从 docs 的 md 中提取 mermaid 代码块，生成可截图渲染的 HTML 页面。"""
import json
import pathlib
import re

ROOT = pathlib.Path(r'D:\quanbudaima\bi_ye_she_ji')
DOCS = [
    ('topic', ROOT / 'docs/01-选题与方案/选题方案与技术方案.md'),
    ('req', ROOT / 'docs/02-设计文档/需求分析.md'),
    ('arch', ROOT / 'docs/02-设计文档/系统架构设计.md'),
    ('db', ROOT / 'docs/02-设计文档/数据库设计文档.md'),
    ('flow', ROOT / 'docs/02-设计文档/功能结构图与流程图.md'),
]
OUT = ROOT / 'shots/_figs/html'
OUT.mkdir(parents=True, exist_ok=True)

TPL = '''<!doctype html>
<html lang="zh"><head><meta charset="utf-8">
<title>__NAME__</title>
<style>
  html,body{margin:0;padding:20px;background:#ffffff;width:__W__px;box-sizing:border-box;}
  .mermaid{font-family:"Microsoft YaHei","SimSun",sans-serif;}
</style>
</head>
<body>
<pre class="mermaid">__CODE__</pre>
<script src="../mermaid.min.js"></script>
<script>
mermaid.initialize({startOnLoad:true,theme:'neutral',securityLevel:'loose',
  flowchart:{useMaxWidth:true,htmlLabels:true,curve:'basis'},
  er:{useMaxWidth:true},
  themeVariables:{fontFamily:'"Microsoft YaHei","SimSun",sans-serif',fontSize:'15px'}});
</script>
</body></html>'''


def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


items = []
for key, path in DOCS:
    lines = path.read_text(encoding='utf-8').splitlines()
    last_head = ''
    idx = 0
    i = 0
    while i < len(lines):
        ln = lines[i]
        if re.match(r'^#{1,4} ', ln):
            last_head = ln.lstrip('#').strip()
        if ln.strip() == '```mermaid':
            j = i + 1
            body = []
            while j < len(lines) and lines[j].strip() != '```':
                body.append(lines[j])
                j += 1
            idx += 1
            name = '%s-%02d' % (key, idx)
            code = '\n'.join(body)
            wide = ('erDiagram' in code) or ('classDiagram' in code)
            w = 2000 if wide else 1500 if len(code) > 900 else 1200
            html = TPL.replace('__NAME__', name).replace('__W__', str(w)).replace('__CODE__', esc(code))
            (OUT / (name + '.html')).write_text(html, encoding='utf-8')
            items.append({'name': name, 'doc': key, 'head': last_head, 'width': w, 'lines': len(body)})
            i = j
        i += 1

(ROOT / 'shots/_figs/list.json').write_text(
    json.dumps(items, ensure_ascii=False, indent=2), encoding='utf-8')
print('figs total:', len(items))
for it in items:
    print(it['name'], '|', it['head'], '|', it['width'], 'px')
