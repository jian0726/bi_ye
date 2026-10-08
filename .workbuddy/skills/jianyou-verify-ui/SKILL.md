---
name: jianyou-verify-ui
description: 简柚博客项目的端到端验证套路：改动后端/前端后，如何在不打扰用户正在运行的 8080 后端与 5173 前端的前提下，用临时端口 + 真实浏览器验证交互状态（点赞/收藏/评论等），以及跑冒烟测试。触发场景：需要验证页面按钮真实行为、接口改动是否正确、刷新后状态是否保持、用户报「数据不变化/是 mock」类问题时。
agent_created: true
---

# 简柚博客：端到端验证套路

## 何时用
- 用户报「按钮点了没反应 / 数据是 mock / 刷新就丢状态」→ **先真实浏览器实测，再改代码**，别靠读代码下结论。
- 改了后端接口或前端交互逻辑后，需要证明「真的生效」。
- 需要跑接口冒烟但不希望占用用户的开发端口。

## 关键前提（先确认环境）
```powershell
# 后端真实路径带 context-path：/api
curl.exe -s -o NUL -w "%{http_code}" http://127.0.0.1:8080/api/portal/articles?page=1&size=1
# 前端 dev server
curl.exe -s -o NUL -w "%{http_code}" http://127.0.0.1:5173/
```
- 直接探 `/portal/articles` 会 404，不是后端挂了，是少了 `/api` 前缀。
- 判断 8080 上跑的是谁的代码：`Get-CimInstance Win32_Process -Filter "ProcessId=<PID>"` 看命令行；
  带 `idea_rt.jar` 说明是用户从 IDEA 启动的（**不要去杀它**，它可能是旧代码，改完要让用户自己重启）。

## 步骤 1：改完后端先编译
```powershell
$env:JAVA_HOME = "D:\Java21"
$env:Path = "D:\Java21\bin;D:\apache-maven-3.9.9\bin;" + $env:Path
Set-Location D:\quanbudaima\bi_ye_she_ji\blog-backend
& mvn.cmd -o -q compile -DskipTests   # 输出重定向到文件再 Read，PowerShell stdout 不稳
```
前端：`Set-Location blog-web; & npx.cmd vue-tsc --noEmit`

## 步骤 2：起一个「验证用」后端，不碰用户的 8080
```powershell
# run_in_background=true 执行；SERVER_PORT 会被沙箱注入，务必显式覆盖
$env:SERVER_PORT = "8090"
& mvn.cmd -o spring-boot:run "-DskipTests" "-Dspring-boot:run.arguments=--server.port=8090"
```
启动成功判据：日志出现 `Tomcat started on port 8090` 与 `Started BlogApplication`。

## 步骤 3：冒烟测试（可自定义目标）
```powershell
$env:SMOKE_BASE = 'http://127.0.0.1:8090/api'
& node scripts/smoke-auth.mjs   # 结果写文件再 Read
```
现有断言覆盖：鉴权/权限、文章 CRUD、评论（含点赞）、留言、友链、收藏、点赞（游客 401 / 重复 400 / 换账号 liked=false）、资料修改。

## 步骤 4：真实浏览器验证交互状态
`agent-browser` 的 daemon **跨脚本调用会被杀**，所以必须**一个 .mjs 脚本内**用 `spawnSync` 串完全部动作。
模板见 `scripts/verify-like-account.mjs`，要点：

1. 临时 vite 指向验证后端，避免动用户的 5173：
   ```powershell
   $env:VITE_PROXY_TARGET = 'http://127.0.0.1:8090'
   & npx.cmd vite --port 5199 --strictPort
   ```
   （`blog-web/vite.config.ts` 的 proxy target 已支持 `process.env.VITE_PROXY_TARGET` 覆盖）
2. 脚本内动作序列：
   ```js
   import { spawnSync } from 'node:child_process'
   const AB_JS = path.resolve(CWD, 'node_modules', 'agent-browser', 'bin', 'agent-browser.js')
   const ab = (args) => { const r = spawnSync(process.execPath, [AB_JS, ...args], { cwd: CWD, encoding: 'utf8', timeout: 120000 }); return ((r.stdout||'')+(r.stderr||'')).trim() }
   const wait = (n) => spawnSync('ping', ['127.0.0.1', '-n', String(n)], { shell: true })  // 等待渲染
   ab(['open', 'http://127.0.0.1:5199/article/2']); wait(7)
   ab(['eval', `(() => { /* 读状态 / 点击 */ return JSON.stringify({...}) })()`])
   ab(['open', 'http://127.0.0.1:5199/article/2']); wait(7)   // 重新 open = 刷新
   ab(['screenshot', 'shots/xxx.png'])
   ```
3. 登录态：在 `/login` 用 `#login-account` / `#login-password` 填值，**必须 dispatch input 事件**才能驱动 v-model：
   ```js
   const d = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')
   d.set.call(el, v); el.dispatchEvent(new Event('input', { bubbles: true }))
   document.querySelector('form').requestSubmit()
   ```
   用冒烟测试账号 `13900001234 / smoke123456`，别污染博主账号。测试完把状态还原（点回取消点赞）。

## 选择器备忘（易踩坑）
- 文章操作条按钮：`b.classList.contains('btn') && b.querySelector('svg path[d^="M8 13.5"]')`（心形）；收藏是 `path[d^="M4 2.5h8"]`。
- 评论点赞按钮：心形 svg 且**不含** `btn` 类 —— `CommentItem` 根元素 class 里没有 "comment"，别用 `closest('[class*=comment]')`。
- 判断红心是否点亮：`svg.getAttribute('fill') === 'currentColor'`。

## 步骤 5：收尾
- 用 TaskStop 停掉 8090 后端与 5199 vite 两个后台任务。
- 明确告诉用户：**8080 上的 IDEA 进程需要自己重启**才能看到新代码；5173 前端 Ctrl+F5 强刷。

## Windows 环境注意
- Bash 不可用（`dirname: command not found`），一律用 PowerShell 工具。
- PowerShell stdout 常丢失：命令输出统一 `| Out-File -Encoding utf8 <file>` 或 `[System.IO.File]::WriteAllText(...)`，再用 Read 读。
- 不要用 `Start-Process`（沙箱拦截）、不要用 `Remove-Item`（安全删除拦截）、不要 `cd` 前缀 git 命令。
- mysql 客户端：`D:\mysql\bin\mysql.exe --host=127.0.0.1 --port=3306 -ujianyou -pjianyou2026 jianyou_blog`；
  容器库：`docker exec -i jianyou-mysql mysql -uroot -proot2026 jianyou_blog`。
- 执行含中文注释的 .sql 会因控制台编码报 ERROR 1064：先把注释行剥掉，或写一份纯 ASCII 临时脚本再 `source`。
