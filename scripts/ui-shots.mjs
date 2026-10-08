/**
 * 批量把 docs/02-设计文档/UI设计图/*.html 渲染成 PNG。
 * 前置：给每页插入 Noto Serif SC 字体链接（已存在则跳过）；给 06-相册 注入弹层开关。
 * 用法：node scripts/ui-shots.mjs
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import fs from 'node:fs'

const DIR = path.resolve(import.meta.dirname, '..', 'docs', '02-设计文档', 'UI设计图')
const FONT_LINK = '<link rel="preconnect" href="https://fonts.googleapis.com">\n<link href="https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap" rel="stylesheet">'

// --- 前置处理：字体 + 06 弹层开关 ---
for (const f of fs.readdirSync(DIR).filter(x => x.endsWith('.html'))) {
  const p = path.join(DIR, f)
  let html = fs.readFileSync(p, 'utf8')
  let changed = false
  if (!html.includes('fonts.googleapis.com') && html.includes('./design.css')) {
    html = html.replace('<link rel="stylesheet" href="./design.css">', FONT_LINK + '\n<link rel="stylesheet" href="./design.css">')
    changed = true
  }
  if (f.startsWith('06') && !html.includes('nolbSwitch')) {
    html = html.replace('</body>', `<script id="nolbSwitch">if(new URLSearchParams(location.search).has('nolb')){var lb=document.querySelector('.lightbox');if(lb)lb.style.display='none'}<\/script>\n</body>`)
    changed = true
  }
  if (changed) fs.writeFileSync(p, html, 'utf8')
}

// --- 渲染清单 ---
const pages = [
  ['01-首页.html', '01-首页', true, ''],
  ['02-家.html', '02-家', true, ''],
  ['03-游记.html', '03-游记', true, ''],
  ['04-随笔.html', '04-随笔', true, ''],
  ['05-记录.html', '05-记录', true, ''],
  ['06-相册.html', '06-相册', true, 'nolb'],
  ['06-相册.html', '06-相册-大图弹层', false, ''],
  ['07-百宝箱.html', '07-百宝箱', true, ''],
  ['08-留言弹幕墙.html', '08-留言弹幕墙', false, ''],
  ['09-文章详情.html', '09-文章详情', true, ''],
  ['10-登录注册.html', '10-登录注册', false, ''],
]

const run = (args) => {
  const r = spawnSync('npx.cmd', ['agent-browser', ...args], { cwd: DIR, shell: true, encoding: 'utf8', timeout: 120000 })
  return { out: (r.stdout ?? '').trim(), err: (r.stderr ?? '').trim(), code: r.status }
}

const log = []
for (const [file, name, full, q] of pages) {
  run(['close', '--all'])
  const url = 'file:///' + encodeURI(path.join(DIR, file).replace(/\\/g, '/')) + (q ? '?' + q : '')
  const steps = [
    ['open', url],
    ['set', 'viewport', '1440', '900'],
    ['wait', '4200'],
    ['screenshot', path.join(DIR, name + '.png'), ...(full ? ['--full'] : [])],
  ]
  for (const s of steps) {
    const r = run(s)
    log.push(`>>> ${s.join(' ')}\n[exit ${r.code}]\n${r.out}\n${r.err ? 'ERR: ' + r.err : ''}`)
  }
  console.log('shot ->', name)
}
fs.writeFileSync(path.join(DIR, '_shots.log.txt'), log.join('\n\n'), 'utf8')
console.log('ALL DONE')
