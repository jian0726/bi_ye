<template>
  <div class="min-h-screen flex flex-col relative">
    <AppHeader />

    <!-- 版心竖线：标出内容区两侧边界，给留白区一个「杂志版心」的分界。
         登录/注册是全屏沉浸页，不套版心（否则窄窗口下右侧/底部露出多余留白带） -->
    <div v-if="!isFullBleed" class="rule-line rule-line-left" aria-hidden="true" />
    <div v-if="!isFullBleed" class="rule-line rule-line-right" aria-hidden="true" />

    <!-- 版心内容区底色：版心线之内铺一层 elevated 底色，与两侧留白区分 -->
    <div v-if="!isFullBleed" class="canvas-fill" aria-hidden="true" />

    <main class="flex-1">
      <!-- 嵌套路由子页面必须用 RouterView 渲染（slot 不生效）；页面切换动画已按需求移除 -->
      <RouterView />
    </main>

    <!-- 页脚仅保留在「家」页：站点导航与版权收尾的定位与关于页契合，其余页面保持沉浸 -->
    <AppFooter v-if="route.path === '/about'" />
    <BackToTop v-if="route.path !== '/message'" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppFooter from '@/components/AppFooter.vue'
import BackToTop from '@/components/BackToTop.vue'

const route = useRoute()

/** 全屏沉浸页：不画版心线与版心底色，内容满幅铺底 */
const FULL_BLEED = ['/login', '/register', '/forgot']
const isFullBleed = computed(() => FULL_BLEED.includes(route.path))
</script>

<style scoped>
/* 版心线位置 = 内容容器（--shell-max）边界 + 容器内边距，与各页 .shell 对齐 */
/*
 * 版心线用 absolute（相对根 div，宽度不含滚动条）而非 fixed 的 100vw（含滚动条），
 * 避免 ~7px 的滚动条误差导致线切进内容。
 */
.rule-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 1px;
  background: var(--color-divider);
  pointer-events: none;
  /* 压到内容层之下：墨色刊头、卡片等自带背景的区块会盖住线，
     线只在暖纸留白区露出 —— 留白从刊头之下的正文区才开始 */
  z-index: -1;
}
.rule-line-left {
  left: max(0px, calc((100% - var(--shell-max)) / 2 + clamp(1.5rem, 3vw, 3.5rem)));
}
.rule-line-right {
  right: max(0px, calc((100% - var(--shell-max)) / 2 + clamp(1.5rem, 3vw, 3.5rem)));
}
/* 窄屏下容器贴边，线失去意义，隐藏。
   阈值取 1280px：--shell-max 下限为 1120px，若窗口低于该值则容器装不下版心，
   会算出「右边界贴 0、左边界仍内缩」的不对称白带，故提前隐藏避免夹缝状态 */
@media (max-width: 1280px) {
  .rule-line {
    display: none;
  }
}

/*
 * 版心内容区底色：铺在两条版心线之间（elevated 底色，亮色主题为白），
 * z-index -1 被满幅刊头等背景盖住，只在正文区可见；两侧留白保持 body 原背景。
 */
.canvas-fill {
  position: absolute;
  top: 0;
  bottom: 0;
  z-index: -1;
  left: max(0px, calc((100% - var(--shell-max)) / 2 + clamp(1.5rem, 3vw, 3.5rem)));
  right: max(0px, calc((100% - var(--shell-max)) / 2 + clamp(1.5rem, 3vw, 3.5rem)));
  background: var(--color-surface-elevated);
  pointer-events: none;
}
@media (max-width: 1280px) {
  .canvas-fill {
    display: none;
  }
}

</style>
