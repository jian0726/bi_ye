/**
 * 截取手绘图 HTML（shots/_figs/html-draw/*.html）为 PNG。
 * 用法：node scripts/shot-draw.mjs [namePrefix]
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '..')
const PNG = path.join(ROOT, 'shots', '_figs', 'png')
const WEB = path.join(ROOT, 'blog-web')
fs.mkdirSync(PNG, { recursive: true })

const only = process.argv[2] ?? ''
const items = JSON.parse(fs.readFileSync(path.join(ROOT, 'shots', '_figs', 'list.json'), 'utf8'))
const draws = [
  'draw-module', 'draw-flow', 'draw-er-core', 'draw-er-support'
]

const run = (args) => {
  const r = spawnSync('npx.cmd', ['agent-browser', ...args], {
    cwd: WEB, shell: true, encoding: 'utf8', timeout: 120000
  })
  return { out: (r.stdout ?? '').trim(), err: (r.stderr ?? '').trim(), code: r.status }
}

const log = []
for (const name of draws) {
  if (only && !name.startsWith(only)) continue
  const url = `http://127.0.0.1:8799/html-draw/${name}.html`
  const out = path.join(PNG, `${name}.png`)
  if (fs.existsSync(out)) fs.unlinkSync(out)
  run(['open', url])
  run(['wait', name.startsWith('draw-er') ? '2600' : '1800'])
  run(['screenshot', out, '--full'])
  const size = fs.existsSync(out) ? fs.statSync(out).size : -1
  log.push(`${name}\tpng=${size}B`)
}

fs.appendFileSync(path.join(ROOT, 'shots', '_figs', '_shots.log.txt'), '\n--draw--\n' + log.join('\n'), 'utf8')
console.log(log.join('\n'))
