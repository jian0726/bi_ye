<template>
  <div ref="root" class="msg-stage" aria-label="留言雨">
    <!-- 自定义背景：个人中心上传的留言页背景（仅上传者本人可见，压暗保证文字可读） -->
    <div
      v-if="customBg"
      class="custom-bg"
      :style="{ backgroundImage: `url(${customBg})` }"
      aria-hidden="true"
    />
    <!-- 文字雨留言板：整句留言像字幅一样缓缓飘落，鼠标控制风向（参照 gogoame.sumbioun.com 的形式） -->
    <!-- 墨绿夜空背景（与全站刊头同族，纯 CSS/SVG 绘制） -->
    <div class="sky" aria-hidden="true">
      <span class="moon" />
      <span class="stars" />
      <span class="stars stars-2" />
      <span class="cloud c1" />
      <span class="cloud c2" />
      <span class="cloud c3" />

      <!-- 地平线剪影：远山 + 房屋 + 前景台阶 -->
      <svg
        class="silhouette far"
        viewBox="0 0 1440 300"
        preserveAspectRatio="xMidYMax slice"
        aria-hidden="true"
      >
        <path
          d="M0 190 L90 150 L170 178 L260 120 L340 165 L430 140 L520 175 L640 130 L760 170 L880 145 L980 175 L1080 150 L1180 180 L1290 155 L1370 175 L1440 160 L1440 300 L0 300 Z"
          fill="#0d1613"
          opacity="0.9"
        />
        <path
          d="M0 235 L120 210 L240 232 L380 205 L520 235 L700 215 L860 240 L1020 212 L1180 238 L1320 220 L1440 236 L1440 300 L0 300 Z"
          fill="#091009"
          opacity="0.95"
        />
      </svg>
      <svg
        class="silhouette near"
        viewBox="0 0 1440 260"
        preserveAspectRatio="xMidYMax slice"
        aria-hidden="true"
      >
        <g fill="#060b08">
          <rect x="1120" y="168" width="92" height="62" />
          <path d="M1108 168 L1166 132 L1224 168 Z" />
          <rect x="1152" y="192" width="20" height="38" fill="#f0824a" opacity="0.9" />
          <rect x="960" y="182" width="70" height="48" />
          <path d="M950 182 L995 152 L1040 182 Z" />
          <rect x="260" y="120" width="5" height="120" />
          <rect x="228" y="132" width="70" height="4" />
          <rect x="236" y="146" width="54" height="3" />
          <rect x="0" y="236" width="360" height="24" />
          <rect x="0" y="212" width="300" height="24" />
          <rect x="0" y="188" width="240" height="24" />
          <rect x="0" y="164" width="180" height="24" />
          <rect x="0" y="140" width="120" height="24" />
        </g>
      </svg>
    </div>

    <!-- 雨丝装饰 -->
    <div class="rain-layer" aria-hidden="true">
      <span
        v-for="r in rainDrops"
        :key="r.key"
        class="rain-drop"
        :style="{ left: r.left, height: r.height, width: r.width, animationDuration: r.dur, animationDelay: r.delay, opacity: r.opacity }"
      />
    </div>

    <!-- 文字雨画布：一句句留言缓缓飘落 -->
    <canvas ref="stage" class="text-rain" aria-hidden="true" />

    <!-- 复制提示 -->
    <Transition name="tip">
      <div v-if="copyTip" class="copy-tip">{{ copyTip }}</div>
    </Transition>

    <!-- 顶部标题 -->
    <header class="rain-head">
      <h2 class="rain-title">留言化雨</h2>
      <p class="rain-sub">把想说的话说给雨听 · 左右移动鼠标，风会跟着你</p>
    </header>

    <!-- 底部输入区（gogoame 式极简） -->
    <div class="composer">
      <div class="composer-row">
        <input
          v-model="draft"
          type="text"
          maxlength="60"
          placeholder="写点什么，让它落进雨里"
          class="composer-input"
          @keyup.enter="submit"
        />
        <button class="composer-send" :disabled="submitting || !draft.trim()" @click="submit">
          {{ submitting ? '落雨中' : '化作雨' }}
        </button>
      </div>
      <p class="composer-meta">{{ messages.length }} 句话正在落进雨里</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getMessages, postMessage } from '@/api'
import type { Message } from '@/types'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

/** 当前登录用户的自定义留言页背景（未登录/未上传 = 默认夜空） */
const customBg = computed(() => userStore.user?.messageBg || '')

const root = ref<HTMLElement>()
const stage = ref<HTMLCanvasElement>()

const submitting = ref(false)
const messages = ref<Message[]>([])
const draft = ref('')
const copyTip = ref('')
let tipTimer: number | undefined

/* ============================ 文字雨引擎 ============================
 * 每条留言是一行完整可读的句子（一条「字幅」），像雨一样缓缓下坠：
 * - 慢速终端落速 + 水平跟随鼠标风向（带惯性与微倾）
 * - 出生淡入、近底部淡出；被风吹出屏幕后从另一侧飘回
 * - 新留言橘色发光、率先落下
 * ================================================================ */

interface RainLine {
  content: string
  x: number // 左端
  y: number // 基线（垂直中心）
  vy: number // 下落速度（px/s）
  vx: number // 水平速度（px/s，跟随风）
  size: number
  color: string
  glow: boolean // 新留言发光
  born: number // 出生时刻（秒）
  width: number // 估算整行宽度
}

/** 留言池为空时的兜底雨（页面永远有雨落下） */
const FALLBACK_PHRASES = [
  '雨天适合把心事慢慢说出来',
  '你看 这句话正在落雨',
  '风往哪吹 雨就往哪斜',
  '落进雨里的话 会被好好收着',
  '慢慢落 别着急'
]

const WIND_MAX = 2.4 // 鼠标在最左/最右时的风速档（内部换算为水平速度）
const INK = 'rgba(244, 241, 232, 0.9)'
const ORANGE = 'rgba(240, 130, 74, 0.95)'
const GREEN = 'rgba(127, 174, 158, 0.92)'
const FONT_STACK = '"Noto Serif SC", "Source Han Serif SC", "Songti SC", serif'

let ctx: CanvasRenderingContext2D | null = null
let rafId = 0
let W = 0
let H = 0
let lastTs = 0
let clock = 0 // 引擎时钟（秒）
let windTarget = 0
let wind = 0
let lines: RainLine[] = []
let nextSpawnAt = 0
let maxLines = 9

/** 雨丝装饰：粗细两档交错（氛围层） */
const rainDrops = Array.from({ length: 14 }, (_, i) => ({
  key: i,
  left: `${(i * 71) % 100}%`,
  height: `${40 + ((i * 53) % 70)}px`,
  width: i % 3 === 0 ? '1.5px' : '1px',
  dur: `${1.1 + ((i * 29) % 10) / 10}s`,
  delay: `${-((i * 37) % 20)}s`,
  opacity: 0.08 + ((i * 13) % 10) / 100
}))

function pickColor(fresh: boolean): string {
  if (fresh) return ORANGE
  const roll = Math.random()
  return roll < 0.72 ? INK : roll < 0.88 ? ORANGE : GREEN
}

/** 估算一行文字宽度：中日韩全角按 1，半角按 0.56 */
function textWidth(content: string, size: number): number {
  let units = 0
  for (const c of content) units += c.charCodeAt(0) > 255 ? 1 : 0.56
  return units * size
}

function spawnLine(content: string, fresh = false, fromTop = true) {
  let size = fresh ? 18 + Math.random() * 5 : 14 + Math.random() * 9
  // 窄屏自适应：整句必须放进屏宽内（保底 11px），否则逐级缩小字号
  while (size > 11 && textWidth(content, size) > W - 28) size -= 1
  const width = textWidth(content, size)
  const minX = 12
  const maxX = Math.max(W - width - 12, minX)
  const x = minX + Math.random() * (maxX - minX)
  lines.push({
    content,
    x,
    y: fromTop ? -30 - Math.random() * 50 : 60 + Math.random() * Math.max(H - 260, 60),
    vy: (fresh ? 42 : 30) + Math.random() * 26,
    vx: 0,
    size,
    color: pickColor(fresh),
    glow: fresh,
    born: clock,
    width
  })
}

/** 随机取一句留言（池空用兜底短语；避免连续抽到同一句） */
let lastPicked = ''
function pickContent(): string {
  const src = messages.value
  const pool = src.length ? src.map(m => m.content) : FALLBACK_PHRASES
  let pick = pool[Math.floor(Math.random() * pool.length)]
  for (let i = 0; i < 3 && pick === lastPicked && pool.length > 1; i++) {
    pick = pool[Math.floor(Math.random() * pool.length)]
  }
  lastPicked = pick
  return pick
}

function step(dt: number) {
  clock += dt

  // 风缓缓跟上鼠标
  wind += (windTarget - wind) * Math.min(0.05 * dt * 60, 1)

  // 按节奏补一句环境雨
  if (clock >= nextSpawnAt && lines.length < maxLines) {
    spawnLine(pickContent(), false)
    nextSpawnAt = clock + 1.3 + Math.random() * 1.2
  }

  const keep: RainLine[] = []
  for (const l of lines) {
    // 水平追随风（带惯性）
    l.vx += (wind * 32 - l.vx) * Math.min(0.02 * dt * 60, 1)
    l.x += l.vx * dt
    l.y += l.vy * dt
    // 被吹出一边就从另一边飘回来
    if (l.x + l.width < -26) l.x = W + 4
    if (l.x > W + 26) l.x = -l.width - 4
    if (l.y < H + 30) keep.push(l)
  }
  lines = keep
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)
  ctx.textBaseline = 'middle'
  ctx.textAlign = 'left'

  for (const l of lines) {
    const age = clock - l.born
    let a = Math.min(age / 0.5, 1) // 出生淡入
    if (l.y > H - 110) a *= Math.max(1 - (l.y - (H - 110)) / 130, 0) // 近底部淡出
    if (a <= 0.01) continue

    ctx.globalAlpha = a
    if (l.glow) {
      ctx.shadowColor = 'rgba(240, 130, 74, 0.55)'
      ctx.shadowBlur = 16
    } else {
      ctx.shadowBlur = 0
    }
    ctx.fillStyle = l.color
    ctx.font = `${l.size}px ${FONT_STACK}`
    const rot = Math.max(-0.09, Math.min(0.09, l.vx * 0.0022))
    ctx.save()
    ctx.translate(l.x + l.width / 2, l.y)
    ctx.rotate(rot)
    ctx.fillText(l.content, -l.width / 2, 0)
    ctx.restore()
  }
  ctx.shadowBlur = 0
  ctx.globalAlpha = 1
}

function loop(ts: number) {
  rafId = requestAnimationFrame(loop)
  if (!lastTs) lastTs = ts
  const dt = Math.min((ts - lastTs) / 1000, 0.05) // 秒；防切页跳变
  lastTs = ts
  if (dt <= 0) return
  step(dt)
  draw()
}

function resize() {
  const el = stage.value
  if (!el || !el.parentElement) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  W = el.parentElement.clientWidth
  H = el.parentElement.clientHeight
  el.width = Math.round(W * dpr)
  el.height = Math.round(H * dpr)
  ctx = el.getContext('2d')
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  maxLines = W < 700 ? 4 : 9
}

function onPointerMove(e: PointerEvent) {
  windTarget = (e.clientX / window.innerWidth - 0.5) * 2 * WIND_MAX
}
function onPointerLeave() {
  windTarget = 0
}

/** 点击某句话：复制这条留言 */
async function onCanvasClick(e: MouseEvent) {
  const rect = stage.value?.getBoundingClientRect()
  if (!rect) return
  const px = e.clientX - rect.left
  const py = e.clientY - rect.top
  let best: RainLine | null = null
  let bestD = Infinity
  for (const l of lines) {
    if (px >= l.x - 8 && px <= l.x + l.width + 8 && Math.abs(py - l.y) < l.size * 0.9 + 8) {
      const d = Math.abs(py - l.y)
      if (d < bestD) {
        bestD = d
        best = l
      }
    }
  }
  if (!best) return
  try {
    await navigator.clipboard.writeText(best.content)
    showTip('留言已复制')
  } catch {
    showTip('复制失败，请手动选择文字')
  }
}

function showTip(text: string) {
  copyTip.value = text
  if (tipTimer) clearTimeout(tipTimer)
  tipTimer = window.setTimeout(() => (copyTip.value = ''), 1600)
}

/* ============================ 数据 ============================ */

async function fetchMessages() {
  try {
    const res = await getMessages(1, 50)
    messages.value = res.records
  } catch (err) {
    console.error('[message] 加载失败', err)
  }
}

async function submit() {
  const content = draft.value.trim()
  if (!content || submitting.value) return

  // 游客留言不保存：只在雨里落这一次，刷新即散
  if (!userStore.isLoggedIn) {
    spawnLine(content, true)
    draft.value = ''
    showTip('这句话已落进雨里（登录后才会被收藏）')
    return
  }

  submitting.value = true
  try {
    await postMessage({ content })
    draft.value = ''
    showTip('留言已化雨落下')
    // 重新拉取，让新留言进入雨池
    await fetchMessages()
    spawnLine(content, true)
  } catch (err) {
    console.error('[message] 留言失败', err)
    showTip('留言没发出去，再试一次')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await nextTick()
  resize()
  window.addEventListener('resize', resize)
  window.addEventListener('pointermove', onPointerMove)
  document.addEventListener('pointerleave', onPointerLeave)
  stage.value?.addEventListener('click', onCanvasClick)
  rafId = requestAnimationFrame(loop)
  await fetchMessages()
  // 开场先在半空铺几条，避免空白等待
  for (let i = 0; i < Math.min(4, maxLines - 1); i++) {
    spawnLine(pickContent(), false, false)
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', resize)
  window.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerleave', onPointerLeave)
  if (tipTimer) clearTimeout(tipTimer)
})
</script>

<style scoped>
.msg-stage {
  position: relative;
  height: 100dvh;
  overflow: hidden;
  background: var(--ink-bg);
  color: var(--ink-text);
  font-family: var(--font-display);
}

/* ---------- 墨绿夜空（与全站刊头同族） ---------- */
.sky {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 900px 420px at 78% 12%, rgba(240, 130, 74, 0.16) 0%, transparent 62%),
    radial-gradient(ellipse 760px 380px at 8% 85%, rgba(47, 93, 80, 0.30) 0%, transparent 65%),
    linear-gradient(
      180deg,
      var(--ink-bg-deep) 0%,
      #0d1411 38%,
      #12201a 68%,
      #1a2e25 88%,
      #22392e 100%
    );
  animation: sky-in 2s ease both;
}
@keyframes sky-in {
  from {
    opacity: 0;
    transform: translateY(-50px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.moon {
  position: absolute;
  top: 9%;
  right: 12%;
  width: 104px;
  height: 104px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 38%, #fdf9ee 0%, #f4e9cd 48%, rgba(244, 233, 205, 0.24) 68%, transparent 74%);
  filter: blur(0.4px);
  box-shadow: 0 0 90px 30px rgba(240, 170, 110, 0.22);
}

.stars,
.stars-2 {
  position: absolute;
  top: 0;
  left: 0;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: transparent;
  animation: twinkle 3.4s ease-in-out infinite alternate;
}
.stars {
  box-shadow:
    80px 60px #fff, 240px 120px rgba(255, 255, 255, 0.8), 420px 40px #fff,
    610px 90px rgba(255, 255, 255, 0.7), 760px 30px #fff, 900px 110px rgba(255, 255, 255, 0.8),
    1080px 50px #fff, 1240px 100px rgba(255, 255, 255, 0.75), 1380px 70px #fff,
    180px 210px rgba(255, 255, 255, 0.6), 540px 180px rgba(255, 255, 255, 0.55),
    980px 200px rgba(255, 255, 255, 0.5), 1300px 170px rgba(255, 255, 255, 0.6);
}
.stars-2 {
  animation-delay: 1.6s;
  box-shadow:
    150px 90px rgba(255, 255, 255, 0.7), 340px 60px #fff, 500px 130px rgba(255, 255, 255, 0.6),
    690px 60px rgba(255, 255, 255, 0.8), 840px 150px #fff, 1010px 90px rgba(255, 255, 255, 0.65),
    1180px 140px #fff, 1420px 120px rgba(255, 255, 255, 0.6), 60px 150px rgba(255, 255, 255, 0.5),
    460px 90px rgba(255, 255, 255, 0.55), 1120px 30px rgba(255, 255, 255, 0.7);
}
@keyframes twinkle {
  from {
    opacity: 0.45;
  }
  to {
    opacity: 1;
  }
}

.cloud {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(ellipse at center, rgba(93, 162, 145, 0.22) 0%, rgba(93, 162, 145, 0) 70%);
  filter: blur(28px);
  animation: drift 46s linear infinite alternate;
}
.c1 { top: 16%; left: 6%; width: 420px; height: 120px; }
.c2 { top: 30%; right: 4%; width: 520px; height: 150px; animation-duration: 58s; animation-direction: alternate-reverse; }
.c3 { top: 46%; left: 22%; width: 380px; height: 110px; opacity: 0.8; animation-duration: 52s; }
@keyframes drift {
  from { transform: translateX(0); }
  to { transform: translateX(90px); }
}

.silhouette {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  /* 有自定义背景时仍压在背景图之上（z1 的 .custom-bg 之上、雨层之下） */
  z-index: 2;
}
.far { height: 34%; }
.near { height: 30%; }

/* ---------- 自定义背景（个人中心上传，仅本人可见） ---------- */
.custom-bg {
  position: absolute;
  inset: 0;
  z-index: 1;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  animation: sky-in 1.2s ease both;
}
/* 压暗遮罩：保住墨色氛围与文字可读性 */
.custom-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(10, 15, 12, 0.66) 0%,
    rgba(10, 15, 12, 0.42) 46%,
    rgba(7, 11, 9, 0.74) 100%
  );
}

/* ---------- 雨丝装饰 ---------- */
.rain-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 2;
}
.rain-drop {
  position: absolute;
  top: -120px;
  border-radius: 2px;
  background: linear-gradient(180deg, rgba(244, 241, 232, 0) 0%, rgba(244, 241, 232, 0.55) 70%, rgba(244, 241, 232, 0.9) 100%);
  animation: rain-fall linear infinite;
  will-change: transform;
}
@keyframes rain-fall {
  from {
    transform: translateY(-15vh);
  }
  to {
    transform: translateY(120vh);
  }
}

/* ---------- 文字雨画布 ---------- */
.text-rain {
  position: absolute;
  inset: 0;
  z-index: 3;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

/* ---------- 顶部标题 ---------- */
.rain-head {
  position: absolute;
  top: 11%;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 5;
  pointer-events: none;
}
.rain-title {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.35em;
  text-indent: 0.35em;
  color: var(--ink-text);
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.45);
}
.rain-sub {
  margin: 0;
  font-size: 12.5px;
  letter-spacing: 0.2em;
  color: var(--ink-text-secondary);
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.45);
}

/* 复制提示：柚橙描边脉冲一次 */
.copy-tip {
  position: absolute;
  top: 20%;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 20px;
  border-radius: 999px;
  background: rgba(7, 12, 10, 0.8);
  border: 1px solid rgba(240, 130, 74, 0.6);
  color: var(--ink-text);
  font-size: 13px;
  letter-spacing: 0.08em;
  z-index: 20;
  backdrop-filter: blur(6px);
  animation: tip-pulse 0.6s ease-out 1;
}
@keyframes tip-pulse {
  0% { box-shadow: 0 0 0 0 rgba(240, 130, 74, 0.55); }
  100% { box-shadow: 0 0 0 14px rgba(240, 130, 74, 0); }
}
.tip-enter-active,
.tip-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.tip-enter-from,
.tip-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
}

/* ---------- 底部输入区（gogoame 式极简） ---------- */
.composer {
  position: absolute;
  left: 50%;
  bottom: 6.5%;
  transform: translateX(-50%);
  width: 460px;
  max-width: calc(100vw - 48px);
  text-align: center;
  z-index: 10;
}

.composer-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.composer-input {
  flex: 1;
  min-width: 0;
  height: 46px;
  padding: 10px 20px;
  border: 1px solid rgba(244, 241, 232, 0.28);
  border-radius: 23px;
  background: rgba(10, 15, 13, 0.55);
  color: var(--ink-text);
  font-size: 14px;
  font-family: var(--font-display);
  outline: none;
  backdrop-filter: blur(4px);
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.composer-input::placeholder {
  color: var(--ink-text-tertiary);
}
.composer-input:focus {
  border-color: rgba(240, 130, 74, 0.75);
  box-shadow: 0 0 0 3px rgba(240, 130, 74, 0.18);
}

.composer-send {
  height: 46px;
  padding: 0 26px;
  border: none;
  border-radius: 23px;
  background: linear-gradient(135deg, #d95d18, #f0824a);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-indent: 0.09em;
  cursor: pointer;
  box-shadow: 0 6px 22px rgba(217, 93, 24, 0.4);
  transition: transform 0.25s ease, box-shadow 0.25s ease, opacity 0.25s ease;
  flex-shrink: 0;
}
.composer-send:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(217, 93, 24, 0.55);
}
.composer-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.composer-meta {
  margin-top: 12px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--ink-text-tertiary);
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
}

@media (max-width: 640px) {
  .rain-title {
    font-size: 21px;
  }
  .composer-row {
    gap: 8px;
  }
  .composer-send {
    padding: 0 18px;
  }
}
</style>
