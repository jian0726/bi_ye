/**
 * 端到端验证（方案 B）：
 *   登录账号点赞 → 刷新 → 红心是否保持；换账号是否各自独立
 * 目标：临时 vite（5199，代理指向 8090 新后端）
 */
import { spawnSync } from 'node:child_process'
import path from 'node:path'
import { writeFileSync } from 'node:fs'

const CWD = path.resolve(import.meta.dirname, '..', 'blog-web')
const AB_JS = path.resolve(CWD, 'node_modules', 'agent-browser', 'bin', 'agent-browser.js')
const SITE = 'http://127.0.0.1:5199'

function ab(args) {
  const r = spawnSync(process.execPath, [AB_JS, ...args], {
    cwd: CWD, encoding: 'utf8', timeout: 120000
  })
  return ((r.stdout || '') + (r.stderr || '')).trim()
}

const wait = (n) => spawnSync('ping', ['127.0.0.1', '-n', String(n)], { shell: true })

const LOGIN = `(() => {
  const set = (id, v) => {
    const el = document.getElementById(id)
    const d = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')
    d.set.call(el, v)
    el.dispatchEvent(new Event('input', { bubbles: true }))
  }
  set('login-account', '13900001234')
  set('login-password', 'smoke123456')
  document.querySelector('form').requestSubmit()
  return 'SUBMITTED'
})()`

const ARTICLE_STATE = `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.classList.contains('btn') && x.querySelector('svg path[d^="M8 13.5"]'))
  if (!b) return 'NO_BTN'
  return JSON.stringify({ count: b.textContent.trim(), filled: b.querySelector('svg').getAttribute('fill') === 'currentColor' })
})()`

const CLICK_ARTICLE_LIKE = `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.classList.contains('btn') && x.querySelector('svg path[d^="M8 13.5"]'))
  b.click(); return 'CLICKED'
})()`

const COMMENT_STATE = `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.querySelector('svg path[d^="M8 13.5"]') && !x.classList.contains('btn'))
  if (!b) return 'NO_BTN'
  return JSON.stringify({ count: b.textContent.trim() || '0', filled: b.querySelector('svg').getAttribute('fill') === 'currentColor' })
})()`

const CLICK_COMMENT_LIKE = `(() => {
  const b = [...document.querySelectorAll('button')].find(x => x.querySelector('svg path[d^="M8 13.5"]') && !x.classList.contains('btn'))
  b.click(); return 'CLICKED'
})()`

const log = []

// 1) 登录（测试账号，避免污染博主账号）
log.push(ab(['open', `${SITE}/login`]))
wait(7)
log.push('LOGIN: ' + ab(['eval', LOGIN]))
wait(8)
log.push('PATH AFTER LOGIN: ' + ab(['eval', 'location.pathname']))

// 2) 文章点赞 → 刷新 → 红心是否保持
log.push(ab(['open', `${SITE}/article/2`]))
wait(7)
log.push('ARTICLE BEFORE      : ' + ab(['eval', ARTICLE_STATE]))
log.push('CLICK               : ' + ab(['eval', CLICK_ARTICLE_LIKE]))
wait(5)
log.push('ARTICLE AFTER LIKE  : ' + ab(['eval', ARTICLE_STATE]))
log.push(ab(['open', `${SITE}/article/2`]))
wait(7)
log.push('ARTICLE AFTER RELOAD: ' + ab(['eval', ARTICLE_STATE]))
log.push('UNLIKE (restore)    : ' + ab(['eval', CLICK_ARTICLE_LIKE]))
wait(5)
log.push('ARTICLE RESTORED    : ' + ab(['eval', ARTICLE_STATE]))

// 3) 评论点赞 → 刷新 → 红心是否保持
log.push(ab(['open', `${SITE}/article/1`]))
wait(7)
log.push('COMMENT BEFORE      : ' + ab(['eval', COMMENT_STATE]))
log.push('CLICK               : ' + ab(['eval', CLICK_COMMENT_LIKE]))
wait(5)
log.push('COMMENT AFTER LIKE  : ' + ab(['eval', COMMENT_STATE]))
log.push(ab(['open', `${SITE}/article/1`]))
wait(7)
log.push('COMMENT AFTER RELOAD: ' + ab(['eval', COMMENT_STATE]))
log.push('UNLIKE (restore)    : ' + ab(['eval', CLICK_COMMENT_LIKE]))
wait(5)
log.push('COMMENT RESTORED    : ' + ab(['eval', COMMENT_STATE]))

log.push(ab(['screenshot', path.resolve(import.meta.dirname, '..', 'shots', 'like-account-verify.png')]))

writeFileSync(path.resolve(import.meta.dirname, '..', 'shots', '_like-account-verify.txt'), log.join('\n'), 'utf8')
console.log('DONE')
