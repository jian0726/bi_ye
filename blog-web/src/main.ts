import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { setupRouterGuard } from './router/guard'

// 样式：令牌必须最先加载
import './styles/tokens.css'
import './styles/base.css'
import './styles/markdown.css'
import './styles/element-theme.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
setupRouterGuard(router)

app.mount('#app')
