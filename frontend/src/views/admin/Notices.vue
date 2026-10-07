<template>
  <AdminLayout>
    <div class="p-6 lg:p-10">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold">公告管理</h1>
          <p class="text-sm text-[var(--text-secondary)] mt-1">面向各社团发布通知 · 已发布 {{ list.length }} 条</p>
        </div>
        <el-button type="primary" size="large" @click="openAdd">
          <el-icon class="mr-1"><Plus /></el-icon>
          发布新公告
        </el-button>
      </div>

      <div class="bg-white rounded-2xl p-6 shadow-md border border-[#F2EAE0]">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="所属社团" min-width="160">
            <template #default="{ row }">
              <el-tag effect="plain" round>{{ row.clubName || '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="正文" min-width="280" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="text-[var(--text-secondary)]">{{ row.content }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="发布时间" width="180" />
          <template #empty>
            <div class="py-10 text-[var(--text-secondary)]">
              还没有公告,点右上角发布第一条吧
            </div>
          </template>
        </el-table>
      </div>
    </div>

    <!-- 发布公告弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="发布新公告"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="pt-2">
        <el-form-item label="社团" prop="clubId">
          <el-select v-model="form.clubId" placeholder="选择社团" class="!w-full">
            <el-option
              v-for="c in clubOptions"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="64"
            show-word-limit
            placeholder="一句话说清楚"
          />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="公告正文..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminLayout from './AdminLayout.vue'
import api from '@/utils/api'

const list = ref([])
const clubOptions = ref([])
const loading = ref(false)
const submitting = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await api.get('/notice/list')
    list.value = res.data || []
  } catch (_) { /* 请求封装已提示 */
  } finally {
    loading.value = false
  }
}

async function loadClubs() {
  try {
    const res = await api.get('/club/list', { params: { size: 100 } })
    clubOptions.value = (res.data?.records || []).filter((c) => c.isActive)
  } catch (_) { /* 请求封装已提示 */ }
}

const dialogVisible = ref(false)
const formRef = ref(null)
const form = ref({ clubId: null, title: '', content: '' })

const rules = {
  clubId:  [{ required: true, message: '请选择社团', trigger: 'change' }],
  title:   [{ required: true, max: 64, message: '标题 1-64 字', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

function openAdd() {
  form.value = { clubId: null, title: '', content: '' }
  dialogVisible.value = true
}

async function submit() {
  if (submitting.value) return
  if (!await formRef.value.validate().catch(() => false)) return
  submitting.value = true
  try {
    await api.post('/notice', form.value)
    ElMessage.success('发布成功')
    dialogVisible.value = false
    await loadList()
  } catch (_) { /* 请求封装已提示 */ }
  finally { submitting.value = false }
}

onMounted(() => {
  loadList()
  loadClubs()
})
</script>
