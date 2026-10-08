/**
 * 鉴权 + 管理端全流程冒烟测试
 * 运行：node smoke-auth.mjs
 */
const BASE = process.env.SMOKE_BASE || 'http://127.0.0.1:8080/api'

const results = []
let adminToken = ''
let userToken = ''
let createdArticleId = 0
let smokeCommentId = 0
let smokeMessageId = 0
let smokeLinkId = 0

async function call(method, path, { token, body, raw } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  if (raw) return { status: res.status, json: null }
  const json = await res.json()
  return { status: res.status, json }
}

function check(name, cond, extra = '') {
  results.push({ name, pass: !!cond, extra })
  console.log(`${cond ? 'PASS' : 'FAIL'}  ${name}${extra ? '  | ' + extra : ''}`)
}

async function main() {
  /* 1. 博主登录（DataLoader seed 的账号） */
  let r = await call('POST', '/auth/login', { body: { account: '13800000001', password: 'jianyou2026' } })
  check('owner login', r.json?.code === 200 && r.json?.data?.role === 'ADMIN', `code=${r.json?.code}`)
  adminToken = r.json?.data?.token ?? ''

  /* 2. /auth/me */
  r = await call('GET', '/auth/me', { token: adminToken })
  check('auth me', r.json?.code === 200 && r.json?.data?.nickname === '简之航', `code=${r.json?.code} nickname=${r.json?.data?.nickname}`)

  /* 3. 错误密码 */
  r = await call('POST', '/auth/login', { body: { account: '13800000001', password: 'wrong-password' } })
  check('wrong password rejected', r.json?.code !== 200, `code=${r.json?.code} msg=${r.json?.message}`)

  /* 4. 注册新用户（手机号/邮箱至少一项，无用户名字段） */
  const uphone = '13900001234'
  const upwd = 'smoke123456'
  r = await call('POST', '/auth/register', { body: { phone: uphone, nickname: '冒烟测试', password: upwd } })
  if (r.json?.code === 200) {
    check('register new user (phone)', r.json?.data?.token?.length > 20, `userId=${r.json?.data?.userId}`)
  } else {
    r = await call('POST', '/auth/login', { body: { account: uphone, password: upwd } })
    check('register new user (fallback login)', r.json?.code === 200, `code=${r.json?.code}`)
  }
  userToken = r.json?.data?.token ?? ''
  check('new user role is USER', r.json?.data?.role === 'USER', `role=${r.json?.data?.role}`)

  /* 4b. 登录用手机号 */
  r = await call('POST', '/auth/login', { body: { account: uphone, password: upwd } })
  check('login by phone', r.json?.code === 200, `code=${r.json?.code}`)

  /* 4c. 手机号与邮箱都为空 -> 拒绝 */
  r = await call('POST', '/auth/register', { body: { nickname: '无联系方式', password: upwd } })
  check('register without phone/email rejected', r.json?.code !== 200, `code=${r.json?.code} msg=${r.json?.message}`)

  /* 5. 重复注册被拒（同手机号） */
  r = await call('POST', '/auth/register', { body: { phone: uphone, nickname: 'x', password: upwd } })
  check('duplicate phone rejected', r.json?.code !== 200, `code=${r.json?.code}`)

  /* 6. 无 token 访问管理端 */
  r = await call('GET', '/admin/dashboard')
  check('admin no token -> 401', r.json?.code === 401, `code=${r.json?.code}`)

  /* 7. USER token 访问管理端 */
  r = await call('GET', '/admin/dashboard', { token: userToken })
  check('admin user-token -> 403', r.json?.code === 403, `code=${r.json?.code}`)

  /* 8. ADMIN token 仪表盘 */
  r = await call('GET', '/admin/dashboard', { token: adminToken })
  check('admin dashboard', r.json?.code === 200 && r.json?.data?.articleTotal >= 17, `articleTotal=${r.json?.data?.articleTotal} viewTotal=${r.json?.data?.viewTotal}`)

  /* 9. 管理端文章列表（含状态过滤） */
  r = await call('GET', '/admin/articles?page=1&size=5', { token: adminToken })
  check('admin article list', r.json?.code === 200 && r.json?.data?.records?.length === 5, `total=${r.json?.data?.total}`)

  /* 10. 新建草稿 -> 发布 -> 置顶 -> 编辑 -> 删除 */
  r = await call('POST', '/admin/articles', {
    token: adminToken,
    body: {
      title: '[SMOKE] 测试文章',
      summary: 'smoke summary',
      content: '# 冒烟标题\n\n这是冒烟测试正文，包含中文与代码 `code`。',
      categoryId: 1,
      tagIds: [1, 2],
      status: 0,
      isTop: false,
      allowComment: true
    }
  })
  createdArticleId = r.json?.data ?? 0
  check('create draft', r.json?.code === 200 && createdArticleId > 0, `id=${createdArticleId}`)

  r = await call('PUT', `/admin/articles/${createdArticleId}/status?status=1`, { token: adminToken })
  check('publish article', r.json?.code === 200, `code=${r.json?.code}`)

  r = await call('GET', `/portal/articles/${createdArticleId}`)
  check('published visible on portal', r.json?.code === 200 && r.json?.data?.status === 1 && r.json?.data?.wordCount > 0, `status=${r.json?.data?.status} wordCount=${r.json?.data?.wordCount}`)

  r = await call('PUT', `/admin/articles/${createdArticleId}/top`, { token: adminToken })
  check('toggle top', r.json?.code === 200)

  r = await call('PUT', `/admin/articles/${createdArticleId}`, {
    token: adminToken,
    body: {
      title: '[SMOKE] 测试文章-改',
      summary: 'updated',
      content: 'updated content',
      categoryId: 2,
      tagIds: [3],
      status: 1,
      isTop: false,
      allowComment: true
    }
  })
  check('update article', r.json?.code === 200, `code=${r.json?.code}`)

  /* 11. 评论：游客 + 登录用户 */
  r = await call('POST', '/portal/comments', {
    body: { articleId: createdArticleId, content: '游客冒烟评论', nickname: '冒烟访客', type: 1 }
  })
  check('guest comment', r.json?.code === 200, `code=${r.json?.code} id=${r.json?.data?.id}`)
  smokeCommentId = r.json?.data?.id ?? 0

  r = await call('POST', '/portal/comments', {
    token: userToken,
    body: { articleId: createdArticleId, content: '登录用户冒烟评论', type: 1 }
  })
  check('user comment bound to account', r.json?.code === 200 && r.json?.data?.user?.nickname === '冒烟测试', `user=${r.json?.data?.user?.nickname}`)

  /* 12. 管理端评论管理：隐藏 + 删除 */
  r = await call('PUT', `/admin/comments/${smokeCommentId}/status?status=2`, { token: adminToken })
  check('hide comment', r.json?.code === 200)
  r = await call('DELETE', `/admin/comments/${smokeCommentId}`, { token: adminToken })
  check('delete comment', r.json?.code === 200)

  /* 12b. 我的评论：登录用户维度（必须在删文章前查 —— 删文章会级联删评论） */
  r = await call('GET', '/portal/comments/me?page=1&size=20', { token: userToken })
  check('my comments has smoke comment', r.json?.code === 200 && r.json?.data?.records?.some?.((c) => c.content === '登录用户冒烟评论') === true, `total=${r.json?.data?.total}`)

  /* 13. 留言：游客留言 + 博主回复 + 删除 */
  r = await call('POST', '/portal/messages', { body: { content: '冒烟测试留言', nickname: '冒烟访客' } })
  check('guest message', r.json?.code === 200, `code=${r.json?.code}`)

  r = await call('GET', '/admin/messages?page=1&size=5', { token: adminToken })
  smokeMessageId = r.json?.data?.records?.find((m) => m.content === '冒烟测试留言')?.id ?? 0
  check('admin message list', smokeMessageId > 0, `id=${smokeMessageId}`)

  r = await call('PUT', `/admin/messages/${smokeMessageId}/reply`, { token: adminToken, body: { content: '冒烟回复' } })
  check('reply message', r.json?.code === 200)

  r = await call('DELETE', `/admin/messages/${smokeMessageId}`, { token: adminToken })
  check('delete message', r.json?.code === 200)

  /* 14. 友链：申请 -> 审核 -> 删除 */
  r = await call('POST', '/portal/links/apply', {
    body: { name: '[SMOKE] 测试站点', url: 'https://smoke.example.com', description: 'smoke' }
  })
  check('apply link', r.json?.code === 200, `code=${r.json?.code}`)

  r = await call('GET', '/admin/links', { token: adminToken })
  smokeLinkId = r.json?.data?.find((l) => l.name === '[SMOKE] 测试站点')?.id ?? 0
  check('pending link in admin list', smokeLinkId > 0, `id=${smokeLinkId} status=${r.json?.data?.find((l) => l.id === smokeLinkId)?.status}`)

  r = await call('PUT', `/admin/links/${smokeLinkId}/status?status=1`, { token: adminToken })
  check('approve link', r.json?.code === 200)

  r = await call('GET', '/portal/links')
  check('approved link visible on portal', r.json?.data?.some?.((l) => l.name === '[SMOKE] 测试站点'), 'visible=true')

  r = await call('DELETE', `/admin/links/${smokeLinkId}`, { token: adminToken })
  check('delete link', r.json?.code === 200)

  /* 15. 删除冒烟文章 */
  r = await call('DELETE', `/admin/articles/${createdArticleId}`, { token: adminToken })
  check('delete article', r.json?.code === 200)

  r = await call('GET', `/portal/articles/${createdArticleId}`)
  check('deleted article gone', r.json?.code !== 200, `code=${r.json?.code}`)

  /* 16. 收藏：落库 + 用户隔离（t_collect 维度） */
  await call('DELETE', '/portal/articles/2/collect', { token: userToken }) // 复位，保证可重复执行
  r = await call('POST', '/portal/articles/2/collect', { token: userToken })
  check('collect article', r.json?.code === 200, `code=${r.json?.code} msg=${r.json?.message}`)

  r = await call('GET', '/portal/articles/2/collected', { token: userToken })
  check('collected flag true', r.json?.code === 200 && r.json?.data === true, `data=${r.json?.data}`)

  r = await call('GET', '/portal/articles/me/collections?page=1&size=20', { token: userToken })
  check('my collections has article 2', r.json?.code === 200 && r.json?.data?.records?.some?.((a) => a.id === 2) === true, `total=${r.json?.data?.total}`)

  /* 18. 取消收藏 */
  r = await call('DELETE', '/portal/articles/2/collect', { token: userToken })
  check('uncollect article', r.json?.code === 200, `code=${r.json?.code}`)

  r = await call('GET', '/portal/articles/2/collected', { token: userToken })
  check('collected flag false after uncollect', r.json?.code === 200 && r.json?.data === false, `data=${r.json?.data}`)

  /* 19. 评论点赞（按登录账号落库 t_like_record；游客 401 引导登录） */
  r = await call('POST', '/portal/comments/1/like')
  check('guest comment like -> 401', r.json?.code === 401, `code=${r.json?.code} msg=${r.json?.message}`)

  await call('DELETE', '/portal/comments/1/like', { token: userToken }) // 复位，保证可重复执行
  r = await call('POST', '/portal/comments/1/like', { token: userToken })
  check('comment like', r.json?.code === 200, `code=${r.json?.code} msg=${r.json?.message}`)

  r = await call('POST', '/portal/comments/1/like', { token: userToken })
  check('duplicate comment like rejected', r.json?.code === 400, `code=${r.json?.code} msg=${r.json?.message}`)

  r = await call('GET', '/portal/comments?articleId=1', { token: userToken })
  const likedComment = r.json?.data?.find?.((c) => c.id === 1)
  check('comment liked flag reflected in tree', likedComment?.liked === true, `liked=${likedComment?.liked}`)

  r = await call('GET', '/portal/comments?articleId=1', { token: adminToken })
  const otherView = r.json?.data?.find?.((c) => c.id === 1)
  check('another account sees comment not liked', otherView?.liked === false, `liked=${otherView?.liked}`)

  r = await call('DELETE', '/portal/comments/1/like', { token: userToken })
  check('comment unlike', r.json?.code === 200, `code=${r.json?.code}`)

  /* 20. 修改资料（写回原值，保持数据不变） */
  r = await call('PUT', '/auth/profile', {
    token: adminToken,
    body: { nickname: '简之航', bio: '软件技术专业在读。不太会说话，所以写下来。' }
  })
  check('update profile', r.json?.code === 200 && r.json?.data?.nickname === '简之航', `code=${r.json?.code}`)

  /* 21. 文章点赞（按登录账号落库 t_like_record；游客 401） */
  r = await call('POST', '/portal/articles/2/like')
  check('guest article like -> 401', r.json?.code === 401, `code=${r.json?.code} msg=${r.json?.message}`)

  await call('DELETE', '/portal/articles/2/like', { token: userToken }) // 复位
  r = await call('POST', '/portal/articles/2/like', { token: userToken })
  check('article like', r.json?.code === 200, `code=${r.json?.code} msg=${r.json?.message}`)

  r = await call('POST', '/portal/articles/2/like', { token: userToken })
  check('duplicate article like rejected', r.json?.code === 400, `code=${r.json?.code}`)

  r = await call('GET', '/portal/articles/2', { token: userToken })
  check('article liked flag true for liker', r.json?.data?.liked === true, `liked=${r.json?.data?.liked}`)

  r = await call('GET', '/portal/articles/2', { token: adminToken })
  check('article liked flag false for another account', r.json?.data?.liked === false, `liked=${r.json?.data?.liked}`)

  r = await call('GET', '/portal/articles/2')
  check('article liked flag false for guest', r.json?.data?.liked === false, `liked=${r.json?.data?.liked}`)

  r = await call('DELETE', '/portal/articles/2/like', { token: userToken })
  check('article unlike', r.json?.code === 200, `code=${r.json?.code}`)

  /* 汇总 */
  const failed = results.filter((x) => !x.pass)
  console.log(`\nTOTAL ${results.length}, PASS ${results.length - failed.length}, FAIL ${failed.length}`)
}

main().catch((e) => {
  console.error('SMOKE CRASH:', e?.message ?? e)
  process.exit(1)
})
