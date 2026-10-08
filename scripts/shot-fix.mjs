/**
 * 正文配图补拍（5 张）：
 *   前台：首页（导航顺序已变）、留言（文字雨形态）
 *   后台：仪表盘（侧边栏 11 项）、资源管理、音乐管理（新增小节配图）
 * 输出：shots/_fix/*.png
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '..')
const OUT = path.join(ROOT, 'shots', '_fix')
const WEB = path.join(ROOT, 'blog-web')
const SITE = 'http://127.0.0.1:5173'
const API = 'http://127.0.0.1:8080/api'
fs.mkdirSync(OUT, { recursive: true })

const AGENT_BROWSER = 'C:/Users/32916/AppData/Local/npm-cache/_npx/6de2aa2fded2970c/node_modules/agent-browser/bin/agent-browser.js'
const NODE = process.execPath

// 直接用 node 跑 agent-browser 入口，绕过 npx.cmd（沙箱内 cmd.exe 被锁）
const run = (args) => {
  const r = spawnSync(NODE, [AGENT_BROWSER, ...args], {
    cwd: WEB, shell: false, encoding: 'utf8', timeout: 180000
  })
  return { out: (r.stdout ?? '').trim(), err: (r.stderr ?? '').trim(), code: r.status, error: r.error ? String(r.error) : '' }
}

async function apiCall(method, urlPath, token, bodyObj) {
  try {
    const res = await fetch(API + urlPath, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: bodyObj ? JSON.stringify(bodyObj) : undefined
    })
    return await res.json()
  } catch (e) {
    return { code: -1, message: String(e) }
  }
}

async function login(account, password) {
  const j = await apiCall('POST', '/auth/login', null, { account, password })
  if (j.code !== 200) throw new Error(`login fail ${account}: ${j.message} (code=${j.code})`)
  return j.data.token
}

const log = []
const OWNER = { account: '13800000001', password: 'jianyou2026' }   // 简之航（站长/管理员）

run(['close', '--all'])

function shoot(name, pathUrl, wait = 3000, full = true) {
  const url = SITE + pathUrl
  const out = path.join(OUT, `${name}.png`)
  if (fs.existsSync(out)) fs.unlinkSync(out)
  run(['open', url])
  run(['wait', String(wait)])
  const r = run(['screenshot', out, ...(full ? ['--full'] : [])])
  const size = fs.existsSync(out) ? fs.statSync(out).size : -1
  log.push(`${name}\t${url}\tpng=${size}B\tcode=${r.code}${r.error ? ' err=' + r.error : ''}${r.err ? ' stderr=' + r.err.slice(0, 120) : ''}`)
}

/* ---------- 后台登录态：注入管理员 Token ---------- */
const token = await login(OWNER.account, OWNER.password)
run(['open', SITE + '/login'])
run(['wait', '1500'])
run(['eval', `localStorage.setItem('access_token','${token}');localStorage.setItem('refresh_token','${token}')`])
log.push('token 注入完成')

/* ---------- 截图 ---------- */
const targets = [
  ['fix-home',      '/',              5000, true],   // 首页（导航 8 项顺序）
  ['fix-message',   '/message',       5000, false],  // 留言文字雨（100dvh 全屏）
  ['fix-dashboard', '/admin',         4500, true],   // 后台仪表盘（侧边栏 11 项）
  ['fix-files',     '/admin/files',   4000, true],   // 资源管理
  ['fix-music',     '/admin/music',   4000, true],   // 音乐管理
]

for (const [n, u, w, f] of targets) shoot(n, u, w, f)

fs.writeFileSync(path.join(OUT, '_log.txt'), log.join('\n'), 'utf8')
console.log(log.join('\n'))
