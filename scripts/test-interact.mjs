/**
 * 真实浏览器交互验证：文章点赞 / 收藏 / 评论点赞，数字是否真的变化
 * 走 5173 vite dev server（代理到 8080 后端），以游客身份操作
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { writeFileSync } from 'node:fs'

const CWD = path.resolve(import.meta.dirname, '..', 'blog-web')
const AB_JS = path.resolve(CWD, 'node_modules', 'agent-browser', 'bin', 'agent-browser.js')

function ab(args) {
  const r = spawnSync(process.execPath, [AB_JS, ...args], {
    cwd: CWD, encoding: 'utf8', timeout: 120000
  })
  return ((r.stdout || '') + (r.stderr || '')).trim()
}

const log = []
log.push(ab(['open', 'http://127.0.0.1:5173/article/2']))
spawnSync('ping', ['127.0.0.1', '-n', '7'], { shell: true })

// 文章区两个数字按钮（❤ likeCount / 🔖 collectCount）
const READ_BAR = `(() => {
  const bar = [...document.querySelectorAll('button')].filter(b => /^\\d+$/.test(b.textContent.trim()) && b.querySelector('svg'))
  return JSON.stringify(bar.map(b => b.textContent.trim()))
})()`

log.push('BAR BEFORE: ' + ab(['eval', READ_BAR]))

// 1) 文章点赞
log.push(ab(['eval', `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.querySelector('svg path[d^="M8 13.5"]') && /^\\d+$/.test(x.textContent.trim()))
  if (!b) return 'NO_LIKE_BTN'
  b.click(); return 'CLICKED_LIKE'
})()`]))
spawnSync('ping', ['127.0.0.1', '-n', '4'], { shell: true })
log.push('BAR AFTER LIKE: ' + ab(['eval', READ_BAR]))

// 2) 再点一次取消
log.push(ab(['eval', `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.querySelector('svg path[d^="M8 13.5"]') && /^\\d+$/.test(x.textContent.trim()))
  if (!b) return 'NO_LIKE_BTN'
  b.click(); return 'CLICKED_UNLIKE'
})()`]))
spawnSync('ping', ['127.0.0.1', '-n', '4'], { shell: true })
log.push('BAR AFTER UNLIKE: ' + ab(['eval', READ_BAR]))

// 3) 收藏（游客应跳登录页）
log.push(ab(['eval', `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.querySelector('svg path[d^="M4 2.5h8"]'))
  if (!b) return 'NO_COLLECT_BTN'
  b.click(); return 'CLICKED_COLLECT'
})()`]))
spawnSync('ping', ['127.0.0.1', '-n', '4'], { shell: true })
log.push('URL AFTER COLLECT: ' + ab(['eval', 'location.pathname + location.search']))

// 4) 回到文章页测评论点赞
log.push(ab(['open', 'http://127.0.0.1:5173/article/2']))
spawnSync('ping', ['127.0.0.1', '-n', '7'], { shell: true })
const READ_COMMENT_LIKE = `(() => {
  const btns = [...document.querySelectorAll('button')].filter(b => b.querySelector('svg path[d^="M8 13.5"]') && !/^\\d+$/.test(b.textContent.trim()) && b.closest('[class*=comment]'))
  return JSON.stringify(btns.slice(0, 6).map(b => b.textContent.trim() || '(0)'))
})()`
log.push('COMMENT LIKES BEFORE: ' + ab(['eval', READ_COMMENT_LIKE]))
log.push(ab(['eval', `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.querySelector('svg path[d^="M8 13.5"]') && x.closest('[class*=comment]'))
  if (!b) return 'NO_COMMENT_LIKE_BTN'
  b.click(); return 'CLICKED_COMMENT_LIKE'
})()`]))
spawnSync('ping', ['127.0.0.1', '-n', '4'], { shell: true })
log.push('COMMENT LIKES AFTER: ' + ab(['eval', READ_COMMENT_LIKE]))
log.push(ab(['screenshot', path.resolve(import.meta.dirname, '..', 'shots', 'interact-article.png')]))

writeFileSync(path.resolve(import.meta.dirname, '..', 'shots', '_interact-result.txt'), log.join('\n'), 'utf8')
console.log('DONE')
