<template>
  <div>
    <h2 class="text-[18px] font-semibold mb-5">网站设置</h2>

    <div v-if="loading" class="bg-white rounded-xl border border-[#eee7dc] p-8 text-center text-[13px] text-[#a39a8d]">
      加载中…
    </div>

    <div v-else class="bg-white rounded-xl border border-[#eee7dc] p-6 max-w-[720px]">
      <el-form :model="form" label-width="92px" label-position="left">
        <p class="text-[13px] text-[#8a8378] mb-4">基本信息</p>
        <el-form-item label="站点名称">
          <el-input v-model="form.siteName" maxlength="30" />
        </el-form-item>
        <el-form-item label="站点副标题">
          <el-input v-model="form.siteSubtitle" maxlength="50" />
        </el-form-item>
        <el-form-item label="站点 Logo">
          <el-input v-model="form.siteLogo" placeholder="如 /logo.svg" />
        </el-form-item>
        <el-form-item label="站点描述">
          <el-input v-model="form.siteDescription" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-form-item label="站点关键词">
          <el-input v-model="form.siteKeywords" placeholder="逗号分隔" />
        </el-form-item>

        <p class="text-[13px] text-[#8a8378] mt-6 mb-4">作者信息</p>
        <el-form-item label="作者昵称">
          <el-input v-model="form.siteAuthor" maxlength="30" />
        </el-form-item>
        <el-form-item label="作者头像">
          <el-input v-model="form.authorAvatar" placeholder="图片地址，可留空" />
        </el-form-item>
        <el-form-item label="作者简介">
          <el-input v-model="form.authorBio" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-form-item label="关于页内容">
          <el-input v-model="form.aboutContent" type="textarea" :rows="4" placeholder="支持纯文本，留空使用默认" />
        </el-form-item>

        <p class="text-[13px] text-[#8a8378] mt-6 mb-4">页脚与备案</p>
        <el-form-item label="版权信息">
          <el-input v-model="form.copyright" />
        </el-form-item>
        <el-form-item label="ICP 备案号">
          <el-input v-model="form.icpNumber" placeholder="留空则不显示" />
        </el-form-item>
        <el-form-item label="公安备案号">
          <el-input v-model="form.policeNumber" placeholder="留空则不显示" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="form.githubUrl" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.emailAddress" />
        </el-form-item>
      </el-form>

      <div class="flex justify-end gap-3 mt-6 pt-4 border-t border-[#f2ece2]">
        <el-button @click="load">重置</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminGetSiteConfig, adminUpdateSiteConfig } from '@/api/admin'

const loading = ref(true)
const saving = ref(false)

const form = reactive({
  siteName: '',
  siteSubtitle: '',
  siteLogo: '',
  siteDescription: '',
  siteKeywords: '',
  siteAuthor: '',
  authorAvatar: '',
  authorBio: '',
  aboutContent: '',
  copyright: '',
  icpNumber: '',
  policeNumber: '',
  githubUrl: '',
  emailAddress: ''
})

async function load() {
  loading.value = true
  try {
    const config = await adminGetSiteConfig()
    Object.assign(form, config)
  } catch {
    ElMessage.error('配置加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.siteName.trim()) {
    ElMessage.warning('站点名称不能为空')
    return
  }
  saving.value = true
  try {
    await adminUpdateSiteConfig({ ...form })
    ElMessage.success('已保存，前台刷新后生效')
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
