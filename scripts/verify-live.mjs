/**
 * 联调验证：无头浏览器打开 vite dev 页面，确认文章数据来自真实后端
 * 用法：node scripts/verify-live.mjs
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { writeFileSync } from 'node:fs'

const CWD = path.resolve(import.meta.dirname, '..', 'blog-web')
const AB_JS = path.resolve(CWD, 'node_modules', 'agent-browser', 'bin', 'agent-browser.js')
const NODE = process.execPath

function ab(args) {
  const r = spawnSync(NODE, [AB_JS, ...args], {
    cwd: CWD,
    encoding: 'utf8',
    timeout: 120000
  })
  return (r.stdout || '') + (r.stderr || '')
}

const URL = 'http://127.0.0.1:5173'
const OUT = 'scripts/verify-live-result.txt'

const lines = []
lines.push('== open ==')
lines.push(ab(['open', URL]))
// 等待页面 + API 请求完成（preloader 约 1.5s，数据请求 220ms）
spawnSync('ping', ['127.0.0.1', '-n', '6'], { shell: true })

lines.push('== eval ==')
const js = `(() => {
  const links = document.querySelectorAll('a[href*="/article/"]')
  const headings = [...document.querySelectorAll('h1,h2,h3')].map(h => h.textContent.trim()).slice(0, 8)
  return JSON.stringify({
    title: document.title,
    articleLinks: links.length,
    firstLink: links[0] ? links[0].textContent.trim().slice(0, 40) : '',
    headings,
    bodySnippet: document.body.innerText.slice(0, 260).replace(/\\n/g, ' | ')
  })
})()`
lines.push(ab(['eval', js]))

lines.push('== screenshot ==')
lines.push(ab(['screenshot', path.resolve(CWD, '..', 'scripts', 'verify-live.png'), '--full-page']))

writeFileSync(OUT, lines.join('\n'), 'utf8')
console.log('done -> ' + OUT)
