<template>
  <AdminLayout>
    <div class="p-6 lg:p-10">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold">社团管理</h1>
          <p class="text-sm text-[var(--text-secondary)] mt-1">维护社团信息 · 新增、编辑、下架</p>
        </div>
        <el-button type="primary" size="large" @click="openAdd">
          <el-icon class="mr-1"><Plus /></el-icon>
          新增社团
        </el-button>
      </div>

      <div class="bg-white rounded-2xl p-6 shadow-md border border-[#F2EAE0]">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="社团名称" min-width="180">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <div class="w-9 h-9 rounded-xl brand-hero flex items-center justify-center text-white font-bold text-sm">
                  {{ (row.name || '?').charAt(0) }}
                </div>
                <span class="font-medium">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" width="110">
            <template #default="{ row }">
              <el-tag effect="plain">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="leader" label="社长" width="100" />
          <el-table-column label="人数" width="140">
            <template #default="{ row }">
              <span class="font-medium">{{ row.currentMembers ?? 0 }}</span>
              <span class="text-[var(--text-secondary)]"> / {{ row.maxMembers ?? '∞' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="isActive(row)" type="success" effect="light">在线</el-tag>
              <el-tag v-else type="info" effect="light">已下架</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" :disabled="!isActive(row)" @click="remove(row)">下架</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && list.length === 0" description="还没有社团,点上方新增" />
      </div>

      <!-- 弹窗 -->
      <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增社团' : '编辑社团'" width="560px">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="社团名称" prop="name">
            <el-input v-model="form.name" placeholder="例如:吉他社" />
          </el-form-item>
          <div class="grid grid-cols-2 gap-4">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
                <el-option label="学术" value="学术" />
                <el-option label="体育" value="体育" />
                <el-option label="文艺" value="文艺" />
                <el-option label="公益" value="公益" />
              </el-select>
            </el-form-item>
            <el-form-item label="社长" prop="leader">
              <el-input v-model="form.leader" placeholder="负责人姓名" />
            </el-form-item>
          </div>
          <el-form-item label="上限人数" prop="maxMembers">
            <el-input-number v-model="form.maxMembers" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="社团简介">
            <el-input v-model="form.intro" type="textarea" :rows="4" placeholder="一句话介绍这个社团" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminLayout from './AdminLayout.vue'
import api from '@/utils/api'

const list = ref([])
const loading = ref(false)
const submitting = ref(false)

const isActive = (r) => r.isActive == null ? true : !!r.isActive

const dialogVisible = ref(false)
const dialogMode = ref('add')
const formRef = ref(null)
const form = ref({ id: null, name: '', category: '', intro: '', leader: '', maxMembers: 30 })
const rules = {
  name:       [{ required: true, message: '请输入社团名称', trigger: 'blur' }],
  category:   [{ required: true, message: '请选择分类', trigger: 'change' }],
  leader:     [{ required: true, message: '请输入社长', trigger: 'blur' }],
  maxMembers: [{ required: true, type: 'number', min: 1, max: 999, message: '1-999', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    const resp = await api.get('/club/list', { params: { size: 100 } })
    const d = resp.data
    list.value = Array.isArray(d) ? d : (d?.records || [])
  } catch (_) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  dialogMode.value = 'add'
  form.value = { id: null, name: '', category: '', intro: '', leader: '', maxMembers: 30 }
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  form.value = { ...row }
  dialogVisible.value = true
}

async function submit() {
  if (submitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialogMode.value === 'add') {
      await api.post('/club', form.value)
      ElMessage.success('新增成功')
    } else {
      await api.put('/club', form.value)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadList()
  } catch (_) { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认下架「${row.name}」?下架后学生看不到,但历史报名记录保留。`,
      '下架确认',
      { type: 'warning' }
    )
    await api.delete('/club', { params: { id: row.id } })
    ElMessage.success('已下架')
    loadList()
  } catch (_) { /* cancel or already toasted */ }
}

onMounted(loadList)
</script>
