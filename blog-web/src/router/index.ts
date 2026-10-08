import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '今日' }
      },
      {
        // 聚合列表页已下线：游记/随笔/记录是三个独立页面，旧链接回落到首页
        path: 'articles',
        redirect: '/'
      },
      {
        path: 'article/:id',
        name: 'article-detail',
        component: () => import('@/views/article/ArticleDetailView.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'archive',
        name: 'archive',
        component: () => import('@/views/archive/ArchiveView.vue'),
        meta: { title: '归档' }
      },
      {
        path: 'category/1',
        name: 'category-record',
        component: () => import('@/views/category/RecordView.vue'),
        meta: { title: '记录' }
      },
      {
        path: 'category/2',
        name: 'category-travel',
        component: () => import('@/views/category/TravelView.vue'),
        meta: { title: '游记' }
      },
      {
        path: 'category/3',
        name: 'category-essay',
        component: () => import('@/views/category/EssayView.vue'),
        meta: { title: '随笔' }
      },
      {
        path: 'category/:id',
        name: 'category',
        component: () => import('@/views/category/CategoryView.vue'),
        meta: { title: '分类' }
      },
      {
        path: 'tag/:id',
        name: 'tag',
        component: () => import('@/views/tag/TagView.vue'),
        meta: { title: '标签' }
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('@/views/search/SearchView.vue'),
        meta: { title: '搜索' }
      },
      {
        path: 'about',
        name: 'about',
        component: () => import('@/views/about/AboutView.vue'),
        meta: { title: '关于' }
      },
      {
        path: 'message',
        name: 'message',
        component: () => import('@/views/message/MessageView.vue'),
        meta: { title: '留言' }
      },
      {
        path: 'album',
        name: 'album',
        component: () => import('@/views/album/AlbumView.vue'),
        meta: { title: '相册' }
      },
      {
        path: 'toolbox',
        name: 'toolbox',
        component: () => import('@/views/toolbox/ToolboxView.vue'),
        meta: { title: '百宝箱' }
      },
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/user/LoginView.vue'),
        meta: { title: '登录' }
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/views/user/RegisterView.vue'),
        meta: { title: '注册' }
      },
      {
        path: 'forgot',
        name: 'forgot',
        component: () => import('@/views/user/ForgotPasswordView.vue'),
        meta: { title: '找回密码' }
      },
      {
        path: 'profile',
        component: () => import('@/views/user/ProfileView.vue'),
        meta: { requiresAuth: true },
        children: [
          {
            path: '',
            name: 'profile',
            component: () => import('@/views/user/ProfileInfoView.vue'),
            meta: { title: '个人中心', requiresAuth: true }
          },
          {
            path: 'collections',
            name: 'my-collections',
            component: () => import('@/views/user/MyCollectionsView.vue'),
            meta: { title: '我的收藏', requiresAuth: true }
          }
        ]
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '仪表盘', requiresAdmin: true }
      },
      {
        path: 'articles',
        name: 'admin-articles',
        component: () => import('@/views/admin/ArticleManageView.vue'),
        meta: { title: '文章管理', requiresAdmin: true }
      },
      {
        path: 'articles/edit',
        name: 'admin-article-edit',
        component: () => import('@/views/admin/ArticleEditView.vue'),
        meta: { title: '编辑文章', requiresAdmin: true }
      },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/CategoryManageView.vue'),
        meta: { title: '分类管理', requiresAdmin: true }
      },
      {
        path: 'tags',
        name: 'admin-tags',
        component: () => import('@/views/admin/TagManageView.vue'),
        meta: { title: '标签管理', requiresAdmin: true }
      },
      {
        path: 'messages',
        name: 'admin-messages',
        component: () => import('@/views/admin/MessageManageView.vue'),
        meta: { title: '留言管理', requiresAdmin: true }
      },
      {
        path: 'links',
        name: 'admin-links',
        component: () => import('@/views/admin/LinkManageView.vue'),
        meta: { title: '友链管理', requiresAdmin: true }
      },
      {
        path: 'files',
        name: 'admin-files',
        component: () => import('@/views/admin/ResourceManageView.vue'),
        meta: { title: '资源管理', requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('@/views/admin/UserManageView.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      {
        path: 'album',
        name: 'admin-album',
        component: () => import('@/views/admin/AlbumManageView.vue'),
        meta: { title: '相册管理', requiresAdmin: true }
      },
      {
        path: 'music',
        name: 'admin-music',
        component: () => import('@/views/admin/MusicManageView.vue'),
        meta: { title: '音乐管理', requiresAdmin: true }
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: () => import('@/views/admin/SettingView.vue'),
        meta: { title: '网站设置', requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0, behavior: 'smooth' }
  }
})

export default router
