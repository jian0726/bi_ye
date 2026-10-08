import { computed, ref } from 'vue'
import { getMusicPlaylist } from '@/api'
import type { Music } from '@/types'

/**
 * 全站背景音乐（单例，歌单来自后端 /portal/music）
 * - 启动时拉取后台「音乐管理」配置的歌单；歌单为空时不播放
 * - 首次播放必须由用户手势触发（浏览器自动播放策略）
 * - 开关状态、选中的曲目、音量都记忆在 localStorage；
 *   若记忆为「开」，下次进入站点后在用户第一次点击页面时自动恢复播放
 * - pending 状态：点击后到真正出声之间（网络加载），按钮显示加载动画
 */
const STORAGE_ENABLED = 'jianyou-bgm-enabled'
const STORAGE_TRACK = 'jianyou-bgm-track'
const STORAGE_VOLUME = 'jianyou-bgm-volume'

export interface BgmTrack {
  title: string
  artist: string
  url: string
}

/** 当前歌单（后台「音乐管理」配置，接口失败时为空） */
const playlist = ref<BgmTrack[]>([])

function restoreIndex(): number {
  const saved = Number(localStorage.getItem(STORAGE_TRACK))
  return Number.isInteger(saved) && saved >= 0 && saved < playlist.value.length ? saved : 0
}

function restoreVolume(): number {
  const saved = Number(localStorage.getItem(STORAGE_VOLUME))
  return saved > 0 && saved <= 1 ? saved : 0.22
}

const audio = typeof Audio !== 'undefined' ? new Audio() : null

const trackIndex = ref(typeof localStorage !== 'undefined' ? restoreIndex() : 0)
const volume = ref(typeof localStorage !== 'undefined' ? restoreVolume() : 0.22)

if (audio) {
  audio.loop = false
  audio.preload = 'none'
  audio.volume = volume.value
}

const playing = ref(false)
/** 点击后、真正出声前为 true（在线音源有网络加载延迟） */
const pending = ref(false)
/** 当前曲目信息（歌单为空时为 undefined，播放器显示占位） */
const current = computed(() => playlist.value[trackIndex.value])
let installedRestore = false
let playlistLoaded = false

/** 拉取后台配置的歌单（幂等，仅在首次 useBgm() 时执行一次） */
async function loadPlaylist() {
  if (playlistLoaded) return
  playlistLoaded = true
  try {
    const list = await getMusicPlaylist()
    playlist.value = list.map((m: Music) => ({
      title: m.title,
      artist: m.artist ?? '',
      url: m.url
    }))
    if (playlist.value.length === 0) {
      trackIndex.value = 0
      return
    }
    // 记忆的曲目下标越界时回到第一首
    if (trackIndex.value >= playlist.value.length) {
      trackIndex.value = 0
      persistTrack()
    }
    // 未在播放时静默换源（正在播放的曲子不打断）
    if (audio && !playing.value && !pending.value) {
      audio.src = playlist.value[trackIndex.value].url
    }
  } catch {
    // 后端不可用时歌单为空，播放器不可用
    playlist.value = []
  }
}

/** 强制重新拉取歌单（站长新收一首曲子后调用），返回最新歌单 */
async function refreshPlaylist() {
  playlistLoaded = false
  await loadPlaylist()
  return playlist.value
}

function markStopped() {
  playing.value = false
  pending.value = false
}

function persistTrack() {
  localStorage.setItem(STORAGE_TRACK, String(trackIndex.value))
}

function persistVolume() {
  localStorage.setItem(STORAGE_VOLUME, String(volume.value))
}

/** 在用户手势内调用才可能成功 */
async function play() {
  if (!audio || playlist.value.length === 0) return
  pending.value = true
  try {
    await audio.play()
    playing.value = true
    pending.value = false
    localStorage.setItem(STORAGE_ENABLED, '1')
  } catch {
    // 自动播放被拒 / 网络失败
    markStopped()
  }
}

function pause() {
  if (!audio) return
  audio.pause()
  markStopped()
  localStorage.setItem(STORAGE_ENABLED, '0')
}

/** 切歌：保持播放状态，切换后继续播 */
function setTrack(index: number, autoplayIfPlaying = true) {
  if (!audio) return
  const total = playlist.value.length
  if (total === 0) return
  const next = ((index % total) + total) % total
  if (next === trackIndex.value && audio.src) return
  const wasPlaying = playing.value || pending.value
  trackIndex.value = next
  persistTrack()
  markStopped()
  audio.src = playlist.value[next].url
  if (wasPlaying && autoplayIfPlaying) {
    play()
  }
}

function next() {
  setTrack(trackIndex.value + 1)
}

function prev() {
  setTrack(trackIndex.value - 1)
}

function setVolume(v: number) {
  volume.value = Math.min(1, Math.max(0, v))
  if (audio) audio.volume = volume.value
  persistVolume()
}

if (audio) {
  audio.addEventListener('error', markStopped)
  audio.addEventListener('playing', () => {
    playing.value = true
    pending.value = false
  })
  // 一首播完自动接下一首，循环往复
  audio.addEventListener('ended', () => {
    setTrack(trackIndex.value + 1, true)
  })
}

/** 恢复监听：记忆为开的用户，第一次点击页面任意处时恢复播放（一次性） */
function installRestoreListener() {
  if (installedRestore || typeof document === 'undefined') return
  installedRestore = true
  document.addEventListener(
    'click',
    () => {
      if (!playing.value && !pending.value && localStorage.getItem(STORAGE_ENABLED) === '1') {
        play()
      }
    },
    { once: true }
  )
}

export function useBgm() {
  installRestoreListener()
  loadPlaylist()

  const toggle = () => {
    if (playing.value || pending.value) {
      pause()
    } else {
      play()
    }
  }

  return {
    playing,
    pending,
    current,
    trackIndex,
    playlist,
    volume,
    toggle,
    next,
    prev,
    setTrack,
    setVolume,
    refreshPlaylist
  }
}
