/**
 * Markdown 渲染器
 * 统一配置 markdown-it 与 highlight.js，供全站复用
 */
import MarkdownIt from 'markdown-it'
// 按需注册语言（全量引入会使 chunk 超过 1MB）
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import bash from 'highlight.js/lib/languages/bash'
import json from 'highlight.js/lib/languages/json'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('json', json)

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: false,
  typographer: false,
  highlight(code: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
      } catch {
        /* 降级为转义输出 */
      }
    }
    return md.utils.escapeHtml(code)
  }
})

/** 外链新窗口打开 */
const defaultLinkOpen =
  md.renderer.rules.link_open ??
  ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const href = tokens[idx].attrGet('href') ?? ''
  if (/^https?:\/\//.test(href)) {
    tokens[idx].attrSet('target', '_blank')
    tokens[idx].attrSet('rel', 'noopener noreferrer')
  }
  return defaultLinkOpen(tokens, idx, options, env, self)
}

/** 给代码块加语言标识，便于 CSS 显示 */
md.renderer.rules.fence = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const info = token.info ? token.info.trim() : ''
  const lang = info.split(/\s+/)[0]

  const html = self.renderToken(tokens, idx, options)
  if (!lang) return html

  // 在 <pre> 上注入 data-lang 属性
  return html.replace('<pre>', `<pre data-lang="${md.utils.escapeHtml(lang)}">`)
}

/** 渲染 Markdown 为 HTML */
export function renderMarkdown(source: string): string {
  if (!source) return ''
  return md.render(source)
}

/** 为文章中每个标题注入 id，支持锚点跳转 */
export function withHeadingAnchors(html: string): string {
  let index = 0
  return html.replace(/<h([1-4])>(.*?)<\/h\1>/g, (_match, level, content) => {
    index++
    const id = `heading-${index}`
    return `<h${level} id="${id}">${content}</h${level}>`
  })
}

/** 从渲染后的 HTML 中提取目录 */
export interface TocItem {
  id: string
  text: string
  level: number
}

export function extractToc(html: string): TocItem[] {
  const items: TocItem[] = []
  const regex = /<h([2-3])\s+id="([^"]+)"[^>]*>(.*?)<\/h\1>/g
  let match: RegExpExecArray | null
  while ((match = regex.exec(html)) !== null) {
    items.push({
      level: Number(match[1]),
      id: match[2],
      text: match[3].replace(/<[^>]+>/g, '')
    })
  }
  return items
}

export default md
