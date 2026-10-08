/**
 * 毕业设计正文截图：前台 12 页（测试号登录态）+ 后台 7 页（管理员登录态）。
 * 数据准备：用测试号通过真实 API 生成点赞与收藏（非伪造，均为接口真实落库）。
 * 输出：shots/_thesis/t*.png
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '..')
const OUT = path.join(ROOT, 'shots', '_thesis')
const WEB = path.join(ROOT, 'blog-web')
const SITE = 'http://127.0.0.1:5173'
const API = 'http://127.0.0.1:8080/api'
fs.mkdirSync(OUT, { recursive: true })

const run = (args) => {
  const r = spawnSync('npx.cmd', ['agent-browser', ...args], {
    cwd: WEB, shell: true, encoding: 'utf8', timeout: 120000
  })
  return { out: (r.stdout ?? '').trim(), err: (r.stderr ?? '').trim(), code: r.status }
}

function apiCall(method, urlPath, token, bodyObj) {
  const args = ['-s', '-X', method, API + urlPath, '-H', 'Content-Type: application/json']
  if (token) args.push('-H', `Authorization: Bearer ${token}`)
  if (bodyObj) args.push('-d', JSON.stringify(bodyObj))
  const r = spawnSync('curl.exe', args, { encoding: 'utf8', timeout: 30000 })
  try {
    return JSON.parse(r.stdout)
  } catch {
    return { code: -1, message: (r.stdout ?? '') + (r.stderr ?? '') }
  }
}

function login(account, password) {
  const j = apiCall('POST', '/auth/login', null, { account, password })
  if (j.code !== 200) throw new Error(`login fail ${account}: ${j.message}`)
  return j.data.token
}

const log = []

/* ---------- 1. 数据准备（测试号真实调用接口） ---------- */
const MEMBER = { account: '13800000002', password: 'jianyou2026' }   // 林深（种子演示用户）
const OWNER = { account: '13800000001', password: 'jianyou2026' }    // 简之航（站长）

try {
  const token = login(MEMBER.account, MEMBER.password)
  for (const id of [1, 2, 3]) {
    const j = apiCall('POST', `/portal/articles/${id}/collect`, token)
    log.push(`collect ${id}: ${j.code} ${j.message ?? ''}`)
  }
  for (const id of [1, 2]) {
    const j = apiCall('POST', `/portal/articles/${id}/like`, token)
    log.push(`like ${id}: ${j.code} ${j.message ?? ''}`)
  }
} catch (e) {
  log.push('PREPARE WARN: ' + e.message)
}

/* ---------- 2. 浏览器会话与截图函数 ---------- */
run(['close', '--all'])

async function shoot(name, path_url, wait = 3000, full = true) {
  const url = path_url.startsWith('http') ? path_url : SITE + path_url
  const out = path.join(OUT, `${name}.png`)
  if (fs.existsSync(out)) fs.unlinkSync(out)
  run(['open', url])
  run(['wait', String(wait)])
  run(['screenshot', out, ...(full ? ['--full'] : [])])
  const size = fs.existsSync(out) ? fs.statSync(out).size : -1
  log.push(`${name}\t${url}\tpng=${size}B`)
}

async function setToken(token) {
  run(['open', SITE + '/login'])
  run(['wait', '1200'])
  run(['eval', `localStorage.setItem('access_token','${token}');localStorage.setItem('refresh_token','${token}')`])
}

/* ---------- 3. 前台（测试号登录态） ---------- */
const memberToken = login(MEMBER.account, MEMBER.password)
await setToken(memberToken)

const front = [
  ['t01-home', '/', 4200],
  ['t02-about', '/about', 2600],
  ['t03-travel', '/category/2', 3000],
  ['t04-essay', '/category/3', 3000],
  ['t05-record', '/category/1', 3000],
  ['t06-album', '/album', 3200],
  ['t07-toolbox', '/toolbox', 3000],
  ['t08-message', '/message', 4200, false],
  ['t09-article', '/article/1', 3600],
  ['t10-register', '/register', 2400],
  ['t11-login', '/login', 2400],
  ['t12-collections', '/user/collections', 3000]
]
for (const [n, u, w, f] of front) await shoot(n, u, w, f !== false)

/* ---------- 4. 后台（管理员登录态） ---------- */
const adminToken = login(OWNER.account, OWNER.password)
await setToken(adminToken)

const admin = [
  ['t14-dashboard', '/admin', 3600],
  ['t15-admin-articles', '/admin/articles', 3000],
  ['t16-admin-article-edit', '/admin/articles/edit?id=1', 3200],
  ['t17-admin-messages', '/admin/messages', 3000],
  ['t18-admin-album', '/admin/album', 3000],
  ['t19-admin-users', '/admin/users', 3000],
  ['t20-admin-settings', '/admin/settings', 3000]
]
for (const [n, u, w, f] of admin) await shoot(n, u, w, f !== false)

fs.writeFileSync(path.join(OUT, '_log.txt'), log.join('\n'), 'utf8')
console.log(log.join('\n'))
