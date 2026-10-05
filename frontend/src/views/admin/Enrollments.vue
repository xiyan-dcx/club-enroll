<template>
  <AdminLayout>
    <div class="p-6 lg:p-10">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold">报名审批</h1>
          <p class="text-sm text-[var(--text-secondary)] mt-1">
            待审批
            <el-tag type="warning" class="!mx-1" round>{{ list.length }}</el-tag>
            条
          </p>
        </div>
        <el-button @click="loadList">
          <el-icon class="mr-1"><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <div class="bg-white rounded-2xl p-6 shadow-md border border-[#F2EAE0]">
        <el-table :data="list" v-loading="loading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="学生" min-width="160">
            <template #default="{ row }">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-full brand-mint flex items-center justify-center text-white text-sm font-bold">              
		  {{ (row.realName || row.username || 'S').charAt(0)}}		  
                </div>		   
                <div>
                  <div class="font-semibold">{{ row.realName || row.username }}</div>
                  <div class="text-xs text-[var(--text-secondary)]">{{ row.phone || '—' }}</div>
                </div>


		<!--  因为json数据中没有realName，而是studentName
		<div class="w-10 h-10 rounded-full brand-mint flex items-center justify-center text-white text-sm font-bold">              
		  {{ row.studentName }}		  
                </div>		   
                
		-->
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="clubName" label="申请社团" min-width="180" show-overflow-tooltip />
          <el-table-column label="申请理由" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="text-[var(--text-secondary)]">{{ row.reason || '（未填写）' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="applyTime" label="申请时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="review(row, 'pass')">
                通过
              </el-button>
              <el-button size="small" type="danger" @click="review(row, 'reject')">
                拒绝
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div class="py-10 text-[var(--text-secondary)]">
              暂无待审批的报名 🎉
            </div>
          </template>
        </el-table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import AdminLayout from './AdminLayout.vue'
import api from '@/utils/api'

const list = ref([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await api.get('/enrollment/pending')
    // 后端 EnrollmentService.pending() 返回的是分页 Map(records/total/page/size),
    // records 才是真正的数组,直接取 res.data 会把 Map 喂给 el-table 导致渲染异常。
    list.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

async function review(row, decision) {
  const passing = decision === 'pass'

  /*

      passing
        ? `通过 「${row.studentName || row.username}」 报名「${row.clubName}」?可填写欢迎话。`
        : `拒绝 「${row.studentName || row.username}」 报名「${row.clubName}」?请填写原因。`
  */
  try {
    const { value: remark } = await ElMessageBox.prompt(
      passing
        ? `通过 ${row.realName || row.username} 报名「${row.clubName}」?可填写欢迎话。`
        : `拒绝 ${row.realName || row.username} 报名「${row.clubName}」?请填写原因。`,
      passing ? '通过申请' : '拒绝申请',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: passing ? '欢迎加入社团!' : '',
        inputValidator: (val) => {
          if (!passing && (!val || !val.trim())) return '拒绝理由必填'
          return true
        },
        type: passing ? 'success' : 'warning',
      }
    )
    await api.post('/enrollment/review', {
      enrollmentId: row.id,
      decision,
      remark: (remark || '').trim(),
    })
    ElMessage.success(passing ? '已通过' : '已拒绝')
    loadList()
  } catch (_) {
    /* user cancelled */
  }
}

onMounted(loadList)
</script>
