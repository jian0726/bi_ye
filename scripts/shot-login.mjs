// 登录页弹性布局验证：1080x561（用户窗口）/ 1920x1080 / 420x900
// 注：沙箱内 spawnSync shell:true 会 EBUSY（cmd.exe 被锁），
// 故绕过 npx/cmd，用 node 直接执行 agent-browser 的 bin JS。
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import fs from 'node:fs'

const NODE = 'C:/Users/32916/.workbuddy/binaries/node/versions/22.22.2-3/node.exe'
const AB = 'C:/Users/32916/AppData/Local/npm-cache/_npx/6de2aa2fded2970c/node_modules/agent-browser/bin/agent-browser.js'

const SHOT_DIR = path.resolve(import.meta.dirname, '..', 'shots')
fs.mkdirSync(SHOT_DIR, { recursive: true })

const run = (args) => {
  const r = spawnSync(NODE, [AB, ...args], {
    cwd: path.resolve(import.meta.dirname, '..', 'blog-web'),
    shell: false,
    encoding: 'utf8',
    timeout: 120000
  })
  const o = (r.stdout ?? '') + (r.stderr ?? '')
  console.log(`>>> ${args.join(' ')} [exit=${r.status}]${r.error ? ' err=' + r.error.message : ''}`)
  if (o.trim()) console.log(o.trim().slice(0, 400))
  return o
}

run(['close', '--all'])
run(['open', 'http://127.0.0.1:5173/login'])

run(['set', 'viewport', '1080', '561'])
run(['wait', '2000'])
run(['screenshot', path.join(SHOT_DIR, 'login-1080.png')])
run([
  'eval',
  "JSON.stringify({vw:window.innerWidth,vh:window.innerHeight,hh:document.querySelector('header').offsetHeight,splitH:document.querySelector('.auth-split').offsetHeight,sideW:document.querySelector('.auth-side').offsetWidth})"
])

run(['set', 'viewport', '1920', '1080'])
run(['wait', '1500'])
run(['screenshot', path.join(SHOT_DIR, 'login-1920.png')])
run([
  'eval',
  "JSON.stringify({vw:window.innerWidth,vh:window.innerHeight,splitH:document.querySelector('.auth-split').offsetHeight,sideW:document.querySelector('.auth-side').offsetWidth})"
])

run(['set', 'viewport', '420', '900'])
run(['wait', '1500'])
run(['screenshot', path.join(SHOT_DIR, 'login-420.png')])

run(['close', '--all'])
