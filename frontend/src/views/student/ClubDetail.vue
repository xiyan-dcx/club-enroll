<template>
  <div class="min-h-screen bg-[var(--bg-base)]" v-loading="loading">
    <!-- 顶栏 -->
    <header class="bg-white/80 backdrop-blur-md border-b border-[#F2EAE0] sticky top-0 z-30">
      <div class="max-w-5xl mx-auto px-6 lg:px-8 h-16 flex items-center justify-between">
        <button class="flex items-center gap-1.5 text-[var(--text-primary)] hover:text-[#FF6B6B] transition-colors" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          <span class="font-medium">返回</span>
        </button>
        <span class="font-semibold">社团详情</span>
        <div class="w-12"></div>
      </div>
    </header>

    <main v-if="club" class="max-w-5xl mx-auto px-6 lg:px-8 py-8">
      <!-- 社团 hero 卡 -->
      <div class="rounded-3xl overflow-hidden shadow-xl mb-6 border border-[#F2EAE0]">
        <div class="brand-hero text-white p-8 lg:p-10 relative overflow-hidden">
          <div class="absolute -top-10 -right-10 w-64 h-64 rounded-full bg-white/15 blur-3xl"></div>
          <div class="absolute -bottom-10 left-10 w-48 h-48 rounded-full bg-[#FFE066]/30 blur-2xl"></div>

          <div class="relative flex flex-col sm:flex-row items-start sm:items-center gap-6">
            <div class="w-24 h-24 rounded-3xl bg-white/95 flex items-center justify-center text-[#FF6B6B] text-5xl font-bold shadow-2xl">
              {{ (club.name || '?').charAt(0) }}
            </div>
            <div class="flex-1">
              <h1 class="text-3xl lg:text-4xl font-bold mb-2">{{ club.name }}</h1>
              <div class="flex flex-wrap items-center gap-2">
                <span class="px-3 py-1 rounded-full bg-white/25 text-sm font-medium">{{ club.category }}</span>
                <span class="px-3 py-1 rounded-full bg-white/25 text-sm">社长 {{ club.leader }}</span>
                <span
                  class="px-3 py-1 rounded-full text-sm font-medium"
                  :class="full ? 'bg-white/90 text-[#D8543A]' : 'bg-[#4ECDC4] text-white'"
                >
                  {{ full ? '已招满' : '招新中' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 简介区 -->
        <div class="bg-white p-8 lg:p-10">
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
            <div class="rounded-2xl p-5 bg-[#FF6B6B]/8 border border-[#FF6B6B]/15">
              <div class="text-xs text-[var(--text-secondary)] mb-1">已加入</div>
              <div class="text-2xl font-bold text-[#FF6B6B]">{{ club.currentMembers ?? 0 }}</div>
              <div class="text-xs text-[var(--text-secondary)] mt-1">/ {{ club.maxMembers ?? '∞' }} 上限</div>
            </div>
            <div class="rounded-2xl p-5 bg-[#4ECDC4]/8 border border-[#4ECDC4]/15">
              <div class="text-xs text-[var(--text-secondary)] mb-1">剩余名额</div>
              <div class="text-2xl font-bold text-[#2E8B7C]">
                {{ Math.max((club.maxMembers ?? 0) - (club.currentMembers ?? 0), 0) }}
              </div>
              <div class="text-xs text-[var(--text-secondary)] mt-1">速来抢位</div>
            </div>
            <div class="rounded-2xl p-5 bg-[#FFD86B]/15 border border-[#FFD86B]/30">
              <div class="text-xs text-[var(--text-secondary)] mb-1">分类</div>
              <div class="text-2xl font-bold text-[#8A6500]">{{ club.category }}</div>
              <div class="text-xs text-[var(--text-secondary)] mt-1">活动频繁</div>
            </div>
          </div>

          <div class="mb-8">
            <h3 class="text-lg font-bold mb-3 flex items-center gap-2">
              <span class="w-1 h-5 rounded brand-hero"></span>
              社团简介
            </h3>
            <p class="text-[var(--text-primary)] leading-relaxed whitespace-pre-wrap">
              {{ club.intro || '该社团暂无简介。' }}
            </p>
          </div>

          <!-- 报名按钮 -->
          <button
            class="w-full h-14 rounded-2xl text-white font-semibold text-base transition-all shadow-lg"
            :class="(full || alreadyApplied)
              ? 'bg-gray-300 cursor-not-allowed'
              : 'brand-hero hover:shadow-2xl hover:-translate-y-0.5'"
            :disabled="full || alreadyApplied"
            @click="applyDialog = true"
          >
            {{ buttonText }}
          </button>
        </div>
      </div>

      <!-- 公告 -->
      <div class="bg-white rounded-2xl p-8 shadow-md border border-[#F2EAE0]">
        <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
          <span class="w-1 h-5 rounded brand-mint"></span>
          最新公告
          <span class="text-sm font-normal text-[var(--text-secondary)]">({{ notices.length }})</span>
        </h3>
        <el-empty v-if="notices.length === 0" description="暂无公告" :image-size="80" />
        <div v-else class="space-y-4">
          <div
            v-for="n in notices"
            :key="n.id"
            class="p-4 rounded-xl bg-[#FFF9F5] border border-[#F2EAE0] hover:border-[#4ECDC4]/40 transition-colors"
          >
            <div class="flex items-start justify-between gap-3 mb-2">
              <div class="font-semibold text-[var(--text-primary)]">{{ n.title }}</div>
              <span class="text-xs text-[var(--text-secondary)] flex-shrink-0">{{ n.createdAt }}</span>
            </div>
            <p class="text-sm text-[var(--text-secondary)] leading-relaxed whitespace-pre-wrap">{{ n.content }}</p>
          </div>
        </div>
      </div>
    </main>

    <!-- 报名弹窗 -->
    <el-dialog v-model="applyDialog" title="社团报名" width="500px">
      <p class="text-sm text-[var(--text-secondary)] mb-4">
        填写一段 10-200 字的报名理由,让社长看到你的特别之处 ✨
      </p>
      <el-form :model="applyForm" :rules="applyRules" ref="applyFormRef" label-position="top">
        <el-form-item label="报名理由" prop="reason">
          <el-input
            v-model="applyForm.reason"
            type="textarea"
            :rows="6"
            maxlength="200"
            show-word-limit
            placeholder="我想加入是因为..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">提交报名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '@/utils/api'

const router = useRouter()
const route = useRoute()
const clubId = Number(route.params.id)

const club = ref(null)
const notices = ref([])
const loading = ref(false)

const applyDialog = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({ reason: '' })
const applyRules = {
  reason: [
    { required: true, message: '请填写报名理由', trigger: 'blur' },
    { min: 10, max: 200, message: '10-200 字', trigger: 'blur' },
  ],
}
const submitting = ref(false)
const alreadyApplied = ref(false)

const full = computed(() => club.value && (club.value.currentMembers ?? 0) >= (club.value.maxMembers ?? Infinity))
const buttonText = computed(() => {
  if (full.value) return '社团已招满'
  if (alreadyApplied.value) return '你已报名,等待审批中'
  return '✨ 我要报名 ✨'
})

async function loadDetail() {
  loading.value = true
  try {
    const resp = await api.get('/club/detail', { params: { id: clubId } })
    const d = resp.data
    // 容错:detail 可能直接是 club,也可能是 {club, notices}
    if (d && d.club) {
      club.value = d.club
      notices.value = d.notices || []
    } else {
      club.value = d
      notices.value = []
    }

    // 拉一次"我的报名",判断是否已报
    try {
      const mineResp = await api.get('/enrollment/mine')
      const list = mineResp.data || []
      alreadyApplied.value = Array.isArray(list) && list.some((e) => e.clubId === clubId)
    } catch (_) { /* ignore */ }
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await api.post('/enrollment/apply', { clubId, reason: applyForm.reason })
    ElMessage.success('报名成功,等待审批 🎉')
    applyDialog.value = false
    applyForm.reason = ''
    alreadyApplied.value = true
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(loadDetail)
</script>
