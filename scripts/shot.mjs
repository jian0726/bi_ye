/**
 * 用 Node 子进程驱动 agent-browser，把 open + 交互 + 截图放在同一个进程内完成，
 * 规避 daemon 在每次 Bash/PowerShell 调用结束后被回收、页面退回 about:blank 的问题。
 *
 * 用法：
 *   node scripts/shot.mjs <url> <outName> [--dark] [--wait 5000] [--full] [--w 1920] [--h 1080]
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import fs from 'node:fs'

const args = process.argv.slice(2)
const url = args[0]
const outName = args[1] ?? 'shot'
const flag = (name, def = false) => args.includes(`--${name}`) ? true : def
const value = (name, def) => {
  const i = args.indexOf(`--${name}`)
  return i > -1 ? args[i + 1] : def
}

const isDark = flag('dark')
const wait = Number(value('wait', 5000))
const full = flag('full')
const vpW = value('w', '1440')
const vpH = value('h', '900')
const CWD = path.resolve(import.meta.dirname, '..', 'blog-web')
const SHOT_DIR = path.resolve(import.meta.dirname, '..', 'shots')
fs.mkdirSync(SHOT_DIR, { recursive: true })

// 直连 agent-browser 的 JS 入口、用 process.execPath 拉起：
//   1) `npx.cmd agent-browser` 在本机 spawnSync 返回 status=null（静默失败）
//   2) 走 .cmd / shell:true 会命中 `spawnSync C:\WINDOWS\system32\cmd.exe EBUSY`
// 用 node + js 入口可同时绕开两者。
const AB_BIN = path.join(CWD, 'node_modules', 'agent-browser', 'bin', 'agent-browser.js')
// 允许用 AB_NODE 指定另一个 node 可执行文件：
// 沙箱内 spawnSync(process.execPath) 会返回 EBUSY（spawn 自己所在的可执行文件被锁），
// 换成系统 node（如 D:\node.js\nodejs\node.exe）即可绕开。
const NODE_BIN = process.env.AB_NODE || process.execPath

const run = (abArgs) => {
  const r = spawnSync(NODE_BIN, [AB_BIN, ...abArgs], {
    cwd: CWD, shell: false, encoding: 'utf8', timeout: 120000
  })
  return {
    out: (r.stdout ?? '').trim(),
    err: (r.stderr ?? '').trim(),
    code: r.status,
    spawnErr: r.error ? String(r.error.message) : ''
  }
}

// 关掉可能存在的旧会话，保证从干净状态开始
run(['close', '--all'])

const steps = []
steps.push(['open', url])
if (isDark) steps.push(['set', 'media', 'dark'])
steps.push(['set', 'viewport', vpW, vpH])
steps.push(['wait', String(wait)])

const evalExpr = `JSON.stringify({
  href: location.href,
  title: document.title,
  appLen: (document.getElementById('app')||{innerHTML:''}).innerHTML.length,
  mainLen: document.querySelector('main') ? document.querySelector('main').innerHTML.length : -1,
  mainHead: document.querySelector('main') ? document.querySelector('main').innerHTML.slice(0,500) : 'NOMA',
  reveal: document.querySelectorAll('.reveal').length,
  visible: document.querySelectorAll('.reveal.is-visible').length,
  opacity0: Array.from(document.querySelectorAll('.reveal')).filter(e=>getComputedStyle(e).opacity==='0').length,
  h1: document.querySelector('h1') ? document.querySelector('h1').textContent.trim() : 'none',
  sections: document.querySelectorAll('section').length
})`
steps.push(['eval', evalExpr])
steps.push(['errors'])
steps.push(['console'])

let shotPath = path.join(SHOT_DIR, `${outName}.png`)
const shotArgs = ['screenshot', shotPath]
if (full) shotArgs.push('--full')
steps.push(shotArgs)

const log = []
for (const s of steps) {
  const r = run(s)
  log.push(`>>> ${s.join(' ')}\n[exit ${r.code}]${r.spawnErr ? ' spawnErr: ' + r.spawnErr : ''}\n${r.out}\n${r.err ? 'ERR: ' + r.err : ''}`)
}

fs.writeFileSync(path.join(SHOT_DIR, `_log-${outName}.txt`), log.join('\n\n'), 'utf8')
console.log(`OK -> ${shotPath}`)
