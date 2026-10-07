<template>
  <div class="min-h-screen bg-[var(--bg-base)]">
    <!-- 顶栏 -->
    <header class="bg-white/80 backdrop-blur-md border-b border-[#F2EAE0] sticky top-0 z-30">
      <div class="max-w-5xl mx-auto px-6 lg:px-8 h-16 flex items-center justify-between">
        <button class="flex items-center gap-1.5 text-[var(--text-primary)] hover:text-[#FF6B6B] transition-colors" @click="router.replace('/student/home')">
          <el-icon><ArrowLeft /></el-icon>
          <span class="font-medium">返回首页</span>
        </button>
        <span class="font-semibold">我的报名</span>
        <div class="w-20"></div>
      </div>
    </header>

    <main class="max-w-5xl mx-auto px-6 lg:px-8 py-8">
      <!-- 状态汇总卡 -->
      <div class="grid grid-cols-3 gap-4 mb-6">
        <div class="bg-white rounded-2xl p-5 border border-[#F2EAE0] shadow-sm">
          <div class="text-xs text-[var(--text-secondary)] mb-1">待审批</div>
          <div class="text-3xl font-bold text-[#F2B600]">{{ counts.pending }}</div>
        </div>
        <div class="bg-white rounded-2xl p-5 border border-[#F2EAE0] shadow-sm">
          <div class="text-xs text-[var(--text-secondary)] mb-1">已通过</div>
          <div class="text-3xl font-bold text-[#2E8B7C]">{{ counts.passed }}</div>
        </div>
        <div class="bg-white rounded-2xl p-5 border border-[#F2EAE0] shadow-sm">
          <div class="text-xs text-[var(--text-secondary)] mb-1">已拒绝</div>
          <div class="text-3xl font-bold text-[#D8543A]">{{ counts.rejected }}</div>
        </div>
      </div>

      <div class="bg-white rounded-2xl p-6 lg:p-8 shadow-md border border-[#F2EAE0]">
        <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
          <span class="w-1 h-5 rounded brand-hero"></span>
          报名记录
        </h3>

        <el-table :data="records" v-loading="loading" stripe style="width: 100%">
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="clubName" label="社团名" min-width="160" />
          <el-table-column prop="category" label="分类" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.category || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applyTime" label="报名时间" width="180" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewRemark" label="审批备注" min-width="200">
            <template #default="{ row }">
              <span class="text-[var(--text-secondary)]">{{ row.reviewRemark || '—' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && records.length === 0" description="还没有报名记录,去首页看看心仪的社团吧!" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import api from '@/utils/api'

const router = useRouter()
const records = ref([])
const loading = ref(false)

const statusText = (s) => ({ 1: '待审批', 2: '已通过', 3: '已拒绝' }[s] || '未知')
const statusType = (s) => ({ 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')

const counts = computed(() => {
  const c = { pending: 0, passed: 0, rejected: 0 }
  for (const r of records.value) {
    if (r.status === 1) c.pending++
    else if (r.status === 2) c.passed++
    else if (r.status === 3) c.rejected++
  }
  return c
})

async function load() {
  loading.value = true
  try {
    const resp = await api.get('/enrollment/mine')
    records.value = resp.data || []
  } catch (_) { /* 请求封装已提示 */
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
