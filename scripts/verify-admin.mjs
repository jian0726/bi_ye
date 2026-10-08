/**
 * 登录 + 管理后台浏览器级验证
 * 用法：node scripts/verify-admin.mjs
 * 单脚本内串行完成（agent-browser daemon 跨进程调用会被杀）
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { writeFileSync } from 'node:fs'

const CWD = path.resolve(import.meta.dirname, '..', 'blog-web')
const AB_JS = path.resolve(CWD, 'node_modules', 'agent-browser', 'bin', 'agent-browser.js')
const NODE = process.execPath
const BASE = 'http://127.0.0.1:5173'

function ab(args) {
  const r = spawnSync(NODE, [AB_JS, ...args], {
    cwd: CWD,
    encoding: 'utf8',
    timeout: 120000
  })
  return ((r.stdout || '') + (r.stderr || '')).trim()
}

function wait(n) {
  spawnSync('ping', ['127.0.0.1', '-n', String(n + 1)], { shell: true })
}

const OUT = 'scripts/verify-admin-result.txt'
const lines = []

/* 1. 登录页 */
lines.push('== open /login ==')
lines.push(ab(['open', `${BASE}/login`]))
wait(3)

/* 填表单并提交 */
lines.push('== fill & submit ==')
lines.push(ab(['eval', `(() => {
  const inputs = document.querySelectorAll('input')
  const user = inputs[0], pass = inputs[1]
  if (!user || !pass) return 'INPUTS NOT FOUND'
  const setVal = (el, v) => {
    const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
    setter.call(el, v)
    el.dispatchEvent(new Event('input', { bubbles: true }))
  }
  setVal(user, 'jianyou')
  setVal(pass, 'jianyou2026')
  return 'FILLED'
})()`]))
lines.push(ab(['eval', `(() => {
  const btn = [...document.querySelectorAll('button')].find(b => b.textContent.includes('登录'))
  if (!btn) return 'BUTTON NOT FOUND'
  btn.click()
  return 'CLICKED'
})()`]))
wait(4)

/* 2. 登录态断言 */
lines.push('== after login ==')
lines.push(ab(['eval', `JSON.stringify({
  path: location.pathname,
  hasToken: !!localStorage.getItem('access_token'),
  headerUser: document.querySelector('header a[href="/profile"]')?.getAttribute('title') ?? null
})`]))

/* 3. 管理后台 */
lines.push('== open /admin ==')
lines.push(ab(['open', `${BASE}/admin`]))
wait(4)
lines.push(ab(['eval', `JSON.stringify({
  path: location.pathname,
  title: document.title,
  hasMenu: !!document.querySelector('.admin-menu'),
  statCards: document.querySelectorAll('.grid > div').length,
  bodySnippet: document.body.innerText.slice(0, 200).replace(/\\n/g, ' | ')
})`]))

/* 4. 文章管理页 */
lines.push('== open /admin/articles ==')
lines.push(ab(['open', `${BASE}/admin/articles`]))
wait(4)
lines.push(ab(['eval', `JSON.stringify({
  path: location.pathname,
  rows: document.querySelectorAll('.el-table__row').length,
  hasWriteBtn: [...document.querySelectorAll('button')].some(b => b.textContent.includes('写文章'))
})`]))

/* 5. 退出登录态清理（避免影响后续人工验收） */
lines.push('== cleanup ==')
lines.push(ab(['eval', `(() => { localStorage.removeItem('access_token'); localStorage.removeItem('refresh_token'); return 'CLEARED' })()`]))

writeFileSync(OUT, lines.join('\n'), 'utf8')
console.log('done -> ' + OUT)
