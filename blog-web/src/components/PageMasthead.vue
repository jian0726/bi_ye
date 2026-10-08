<template>
  <!-- 通用墨色刊头：全站统一的「深色沉浸开头」，与首页 MagazineHero 同族 -->
  <section class="page-masthead relative overflow-hidden" :style="{ background: 'var(--ink-bg)' }">
    <!-- 光晕与纸纹 -->
    <div class="absolute inset-0 pointer-events-none" aria-hidden="true">
      <div
        class="absolute -top-[45%] right-[-12%] w-[720px] h-[720px] rounded-full opacity-[0.20] blur-[120px]"
        :style="{ background: 'radial-gradient(circle, var(--ink-accent) 0%, transparent 62%)' }"
      />
      <div
        class="absolute bottom-[-55%] left-[-8%] w-[620px] h-[620px] rounded-full opacity-[0.13] blur-[110px]"
        :style="{ background: 'radial-gradient(circle, #2f6b55 0%, transparent 65%)' }"
      />
      <div class="absolute inset-0 opacity-[0.035] mix-blend-overlay noise-layer" />
    </div>

    <!-- 竖排幽灵字（右侧边缘） -->
    <div
      v-if="ghost"
      class="hidden xl:flex absolute right-6 top-1/2 -translate-y-1/2 z-10 select-none pointer-events-none"
      aria-hidden="true"
    >
      <span
        class="display-serif text-[clamp(2.6rem,4.4vw,4rem)] leading-none tracking-[0.3em]"
        :style="{ color: 'var(--ink-text)', opacity: 0.09, writingMode: 'vertical-rl' }"
      >{{ ghost }}</span>
    </div>

    <div
      class="relative z-10 shell-full flex flex-col justify-end"
      :style="{ minHeight: size === 'lg' ? '58vh' : '46vh', paddingTop: 'calc(var(--header-height) + clamp(2.5rem, 5vw, 4rem))', paddingBottom: 'clamp(2.75rem, 6vw, 4.5rem)' }"
    >
      <!-- 信息条 -->
      <div v-if="meta.length" class="masthead-ink mb-10">
        <span v-for="m in meta" :key="m">{{ m }}</span>
      </div>

      <p v-if="kicker" class="kicker-ink mb-5">{{ kicker }}</p>

      <h1
        class="display-serif text-[clamp(2.9rem,7vw,5.2rem)] leading-[1.06] tracking-[0.02em]"
        :style="{ color: 'var(--ink-text)' }"
      >
        {{ title }}
      </h1>

      <p
        v-if="subtitle"
        class="mt-6 max-w-[560px] display-serif text-[clamp(1rem,1.8vw,1.25rem)] leading-[1.9] whitespace-pre-line"
        :style="{ color: 'var(--ink-text-secondary)' }"
      >
        {{ subtitle }}
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

withDefaults(
  defineProps<{
    /** 衬线大标题，如「游记」 */
    title: string
    /** 英文小标，如 Travel Notes */
    kicker?: string
    /** 一句话副题 */
    subtitle?: string
    /** 信息条内容，如 ['NO.12', '15 篇'] */
    meta?: string[]
    /** 右缘竖排幽灵字，不传则不显示 */
    ghost?: string
    /** 刊头高度档位 */
    size?: 'md' | 'lg'
  }>(),
  {
    kicker: '',
    subtitle: '',
    meta: () => [],
    ghost: '',
    size: 'md'
  }
)
</script>

<style scoped>
.masthead-ink {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: var(--ink-text-tertiary);
}
.masthead-ink::before,
.masthead-ink::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--ink-divider);
}

.kicker-ink {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.2em;
  color: var(--ink-accent);
}
.kicker-ink::before {
  content: '';
  width: 20px;
  height: 1.5px;
  background: currentColor;
}

.noise-layer {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}
</style>
