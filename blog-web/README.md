# 简柚博客前台 · blog-web

个人博客系统的访客端界面，采用 Apple 风格的高级简约设计。

## 技术栈

| 类别 | 技术 |
|---|---|
| 框架 | Vue 3.4（组合式 API） |
| 构建 | Vite 5 |
| 语言 | TypeScript 5 |
| 样式 | Tailwind CSS 3.4 + 原生 CSS 设计令牌 |
| 路由 | Vue Router 4 |
| 状态 | Pinia 2 |
| 请求 | Axios 1 |
| Markdown | markdown-it 14 + highlight.js 11 |
| 时间 | Day.js 1 |

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务（默认 http://127.0.0.1:5173）
npm run dev

# 类型检查 + 生产构建
npm run build

# 预览构建产物
npm run preview
```

## 设计体系

### 设计令牌层

所有视觉参数集中在 `src/styles/tokens.css`，修改此文件即可全站换肤。

| 令牌类别 | 说明 |
|---|---|
| 字体 | SF Pro Display 优先，中文回退 PingFang SC |
| 字号阶梯 | display / hero / title / subtitle，使用 clamp() 实现流式缩放 |
| 字距 | 大字号收紧字距（-0.03em），这是 Apple 风格的关键特征 |
| 间距 | 8px 基准，区块间距 clamp(4rem, 10vw, 8rem) 保证大留白 |
| 圆角 | 卡片 20px，按钮 12px，胶囊 999px |
| 阴影 | 两层叠加而非单层重阴影，营造柔和立体感 |
| 毛玻璃 | backdrop-filter: blur(20px) + saturate(180%) |
| 缓动 | cubic-bezier(0.4, 0, 0.2, 1)，与 Apple 官方一致 |

### 主题

支持亮色与暗色两套主题，通过 `html.light` / `html.dark` 类切换。

- 首屏主题在 `index.html` 内联脚本中提前应用，避免闪白
- 用户选择持久化到 `localStorage`
- 未设置时跟随系统 `prefers-color-scheme`

### 动效

| 动效 | 实现方式 |
|---|---|
| 滚动淡入 | IntersectionObserver + `.reveal` 类，`useScrollReveal` 组合式函数 |
| 交错延迟 | `.reveal-delay-1` ~ `.reveal-delay-6` |
| 路由过渡 | Vue `<Transition name="page">` |
| 卡片悬浮 | `translateY(-4px)` + 阴影升级 |
| 返回顶部 | 滚动超过 600px 时淡入 |
| 阅读进度 | 顶部 2px 进度条，`useScrollProgress` |

已适配 `prefers-reduced-motion`，用户关闭动效时自动禁用。

## 目录结构

```
src/
├── api/              接口封装
│   ├── request.ts    Axios 实例 + 拦截器
│   ├── admin.ts      管理端接口
│   ├── auth.ts       登录 / 注册 / 当前用户
│   └── index.ts      前台接口方法
├── components/       通用组件
│   ├── AppHeader.vue       毛玻璃导航栏
│   ├── AppFooter.vue       页脚
│   ├── ArticleCard.vue     文章卡片
│   ├── CommentItem.vue     递归评论项
│   ├── Pagination.vue      分页
│   ├── Skeleton.vue        骨架屏
│   └── BackToTop.vue       返回顶部
├── composables/      组合式函数
│   ├── useScrollReveal.ts  滚动动效 / 阅读进度
│   └── useTheme.ts         主题切换
├── layouts/          布局
├── router/           路由与守卫
├── stores/           Pinia 状态
├── styles/           样式
│   ├── tokens.css    ⭐ 设计令牌
│   ├── base.css      基础重置 + 工具类
│   └── markdown.css  正文排版 + 代码高亮
├── utils/            工具
│   ├── format.ts     格式化 / 防抖
│   └── markdown.ts   Markdown 渲染
├── views/            页面
└── types.ts          类型定义
```

## 页面清单

| 路由 | 页面 | 说明 |
|---|---|---|
| `/` | 首页 | Hero + 精选 + 最新文章 + 侧栏 |
| `/articles` | 文章列表 | 分页、分类筛选、排序切换 |
| `/article/:id` | 文章详情 | Markdown 渲染、目录、阅读进度、评论区 |
| `/archive` | 归档 | 按年份时间线 |
| `/category/:id` | 分类 | 复用文章列表 |
| `/tag/:id` | 标签 | 复用文章列表 |
| `/search` | 搜索 | 实时关键词检索 |
| `/about` | 关于 | 博主介绍 + 技术栈 |
| `/message` | 留言板 | 留言 + 博主回复 |
| `/links` | 友链 | 友链列表 + 申请表单 |
| `/login` | 登录 | 手机号 / 邮箱 任一 |
| `/register` | 注册 | 手机号或邮箱必填 + 昵称 + 密码强度 |
| `/profile` | 个人中心 | 资料编辑 + 密码修改 |
| `/profile/comments` | 我的评论 | 登录用户维度，按时间倒序 |
| `/profile/collections` | 我的收藏 | 登录用户维度，按收藏时间倒序 |
| `*` | 404 | |

## 开发说明

### 后端接入

前台无本地模拟数据，所有接口直连后端（`request.ts` 统一带 token 并拆掉 `Result` 外壳）。

1. 先启动后端 `blog-backend`（默认 `http://127.0.0.1:8080`）
2. 代理目标在 `vite.config.ts` 的 `server.proxy['/api']` 中配置
3. 列表页在接口失败时显示空状态，不再回落演示数据

### 登录账号规则

**手机号 / 邮箱 任一登录**，密码统一。**没有用户名字段**——唯一的登录标识就是手机号与邮箱。

- 注册必填：手机号或邮箱（**至少填一个**，全站唯一）、昵称（2-20 字符）、密码（6-32 位）
- 唯一性校验只有两套：`t_user.uk_phone`、`t_user.uk_email`（`username` 列已从表和代码中整体移除）
- 不接入短信/邮件服务，手机号与邮箱仅做格式校验，不做验证码验证
- 站长账号由后端 `DataLoader` 首次启动时初始化：手机号 `13800000001`、邮箱 `jianyou@local.dev`，密码见部署约定（BCrypt 存储）

### 用户数据隔离

收藏与评论都以**登录用户 id** 为唯一维度，切换账号后看到的是各自的数据。

| 数据 | 落库位置 | 隔离依据 |
|---|---|---|
| 收藏 | `t_collect`（`uk_user_article` 联合唯一） | `AuthContext.getUserId()` |
| 点赞 | `t_like_record`（`uk_user_target` 联合唯一） | `AuthContext.getUserId()` |

- **点赞按账号落库**（非 IP），这样才能在刷新后保持红心并回答"我赞过哪些"
- **收藏**必须落库成用户×文章关系，否则无法回答"我的收藏"；`collect_count` 只是展示用冗余计数
- 相关接口：`POST/DELETE /portal/articles/{id}/collect`、`GET /portal/articles/{id}/collected`、`GET /portal/articles/me/collections`
- 未登录调用收藏 / 点赞接口返回 401；`/collected` 与详情页对游客返回 `false` 不报错
- **全站无评论功能**（无表、无 Controller、无页面），互动由留言板承担
- 建库直接用 `blog-backend/src/main/resources/db/init.sql`（已合并全部历史升级变更，可重复执行，无需再跑 upgrade 脚本）
