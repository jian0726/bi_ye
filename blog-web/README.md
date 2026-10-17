# blog-web · 前端

简柚博客的访客端与管理端界面。前台与后台是同一个 Vite 工程，后台页面在 `src/views/admin/`。

完整的环境准备与部署说明见[仓库根目录 README](../README.md)。

---

## 技术栈

| 类别 | 技术 | 版本 |
|---|---|---|
| 框架 | Vue（组合式 API + `<script setup>`） | 3.5 |
| 构建 | Vite | 5.4 |
| 语言 | TypeScript | 5.6 |
| 样式 | Tailwind CSS + CSS 设计令牌 | 3.4 |
| 路由 | Vue Router | 4.4 |
| 状态 | Pinia | 2.2 |
| 请求 | Axios | 1.7 |
| 管理端组件 | Element Plus | 2.14 |
| Markdown | markdown-it + highlight.js | 14 / 11 |
| 时间 | Day.js | 1.11 |

---

## 常用命令

```bash
npm install      # 安装依赖
npm run dev      # 开发服务，http://localhost:5173
npm run build    # vue-tsc 类型检查 + vite 构建
npm run preview  # 预览 dist 产物
```

`npm run build` 会先跑 `vue-tsc --noEmit`，类型不通过就不会产出构建结果。

---

## 开发代理

`vite.config.ts` 已配置两条代理，前端无需处理跨域：

| 前缀 | 默认目标 | 环境变量覆盖 |
|---|---|---|
| `/api` | `http://127.0.0.1:8080` | `VITE_PROXY_TARGET` |
| `/files` | `http://127.0.0.1:9000`（MinIO） | `VITE_MINIO_TARGET` |

后端 `context-path` 是 `/api`，所以 `/api/portal/articles` 会被原样转发到后端，不需要 rewrite。

`/files` 需要 rewrite：浏览器请求 `/files/jianyou-blog/xxx.png`，MinIO 期望的路径是 `/jianyou-blog/xxx.png`，故去掉前缀。

`server.host` 设为 `0.0.0.0` 且 `allowedHosts: true`，是为了让 ngrok 等隧道工具能从外部访问。生产构建不受这两项影响。

---

## 目录结构

```
src/
├── api/
│   ├── request.ts        Axios 实例、双 token 拦截与 401 单飞刷新
│   ├── admin.ts          管理端接口
│   ├── auth.ts           登录 / 注册 / 当前用户
│   └── index.ts          前台接口
├── components/
│   ├── AppHeader.vue     导航栏（留言页 overInk 恒亮）
│   ├── AppFooter.vue
│   ├── ArticleCard.vue
│   ├── PageMasthead.vue  各页统一刊头
│   ├── Pagination.vue
│   ├── Preloader.vue
│   ├── RouteProgress.vue
│   ├── BackToTop.vue
│   └── Skeleton.vue
├── composables/
│   ├── useScrollReveal.ts   滚动淡入 / 阅读进度
│   └── useTheme.ts          主题切换
├── layouts/               布局（默认布局、后台布局）
├── router/                路由与守卫
├── stores/                Pinia
├── styles/
│   ├── tokens.css         设计令牌（改这里可全站换肤）
│   ├── base.css
│   └── markdown.css       正文排版与代码高亮
├── utils/
├── views/
│   ├── home/ articles 等前台页面
│   └── admin/             后台 11 个模块
└── types.ts
```

---

## 页面路由

### 前台

| 路由 | 说明 |
|---|---|
| `/` | 首页：刊头 + 精选 + 时间线 |
| `/articles` | 文章列表 |
| `/article/:id` | 文章详情 |
| `/category/1` `/category/2` `/category/3` | 记录 / 游记 / 随笔（三个独立页面，版式各异） |
| `/archive` | 归档 |
| `/tag/:id` | 标签 |
| `/search` | 搜索 |
| `/album` | 相册 |
| `/toolbox` | 百宝箱 |
| `/about` | 家 |
| `/message` | 留言板（100dvh 不滚动，canvas 文字雨） |
| `/login` `/register` `/forgot` | 认证相关 |
| `/profile` `/profile/collections` | 个人中心与收藏 |

### 后台

`/admin` 下：仪表盘、文章管理、文章编辑、分类、标签、留言、友链、文件、用户、相册、音乐。

---

## 认证与请求

- 双 token：access 2 小时、refresh 7 天
- `request.ts` 拦截器在收到 `code=401` 时触发**单飞刷新**（模块级 Promise，避免并发请求同时刷新），成功后重放原请求并写入新 refresh
- 鉴权失败后端返回的是 **HTTP 200 + code 401**，不是 HTTP 401，判断依据是响应体里的 `code`

---

## 视觉体系

- **默认浅色主题**（暖纸白 `#faf9f7`），用户切换后写入 `localStorage`
- 首屏主题在 `index.html` 内联脚本中提前应用，避免闪白
- 首页深墨绿刊头 `#101714` + 反白衬线大字
- 主色：柚橙 `#d95d18`、墨绿 `#2f5d50`
- 版心：`--shell-max: clamp(1120px, 88vw, 1560px)`，下限必须是绝对长度，否则版心会塌成全宽
- 留言板为墨绿夜空 + canvas 文字雨，鼠标左右控制风向

---

## 已知限制

- 全站无评论功能（`CommentItem.vue` 等评论组件已不存在，历史文档若提及请忽略）
- 无 `/links`（友链）前台页面，友链仅后台维护
- 无单元测试
