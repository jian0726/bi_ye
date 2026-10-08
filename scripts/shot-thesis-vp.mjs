/**
 * 为超高页面补视口截图（1440x900，不 full）。
 * 输出 shots/_thesis/v*.png
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '..')
const OUT = path.join(ROOT, 'shots', '_thesis')
const WEB = path.join(ROOT, 'blog-web')
const SITE = 'http://127.0.0.1:5173'
const API = 'http://127.0.0.1:8080/api'

const run = (args) => {
  const r = spawnSync('npx.cmd', ['agent-browser', ...args], {
    cwd: WEB, shell: true, encoding: 'utf8', timeout: 120000
  })
  return { out: (r.stdout ?? '').trim(), err: (r.stderr ?? '').trim() }
}

function login(account, password) {
  const r = spawnSync('curl.exe', ['-s', '-X', 'POST', `${API}/auth/login`,
    '-H', 'Content-Type: application/json',
    '-d', JSON.stringify({ account, password })], { encoding: 'utf8' })
  const j = JSON.parse(r.stdout)
  if (j.code !== 200) throw new Error(`login fail: ${j.message}`)
  return j.data.token
}

const log = []
run(['close', '--all'])
run(['open', SITE + '/login'])
run(['wait', '1500'])
run(['set', 'viewport', '1440', '900'])

async function shoot(name, urlPath, token, wait = 3200) {
  run(['eval', `localStorage.setItem('access_token','${token}');localStorage.setItem('refresh_token','${token}')`])
  const out = path.join(OUT, `${name}.png`)
  if (fs.existsSync(out)) fs.unlinkSync(out)
  run(['open', SITE + urlPath])
  run(['wait', String(wait)])
  run(['screenshot', out])   // 视口截图
  const size = fs.existsSync(out) ? fs.statSync(out).size : -1
  log.push(`${name}\t${urlPath}\tpng=${size}B`)
}

const member = login('13900001234', 'smoke123456')
await shoot('v01-home', '/', member, 4200)
await shoot('v02-about', '/about', member, 2600)
await shoot('v05-record', '/category/1', member, 3000)
await shoot('v09-article', '/article/1', member, 3600)

const admin = login('13800000001', 'jianyou2026')
await shoot('v18-admin-album', '/admin/album', admin, 3000)

fs.appendFileSync(path.join(OUT, '_log.txt'), '\n--viewport--\n' + log.join('\n'), 'utf8')
console.log(log.join('\n'))
