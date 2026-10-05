<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { pending, review } from '../../api/enrollment'
const query = reactive({page:1,size:10})
const { data, loading, error, load } = useLoad(() => pending(query), {records:[],total:0})
const selected = ref(null)
const remark = ref('')
const action = ref('pass')
const saving = ref(false)
async function submit() {
  if (remark.value.trim().length < 5) return ElMessage.warning('审核意见至少 5 个字')
  saving.value = true
  try { await review({id:selected.value.id,action:action.value,remark:remark.value.trim()}); selected.value = null; ElMessage.success('审核完成'); await load() }
  catch { /* 请求封装显示错误 */ }
  finally { saving.value = false }
}
onMounted(load)
</script>
<template>
  <WorkspaceLayout title="报名审批" admin>
    <el-button @click="load" :loading="loading">刷新待审列表</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table :data="data.records" v-loading="loading" empty-text="暂无待审报名">
      <el-table-column prop="studentName" label="学生" /><el-table-column prop="clubName" label="社团" /><el-table-column prop="reason" label="报名理由" min-width="220" /><el-table-column prop="applyTime" label="申请时间" min-width="180" />
      <el-table-column label="操作"><template #default="{row}"><el-button @click="selected=row; remark=''; action='pass'">审核</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="data.total" layout="prev, pager, next" @current-change="load" />
    <el-dialog :model-value="!!selected" @close="selected=null" title="审核报名" width="min(520px,95vw)">
      <el-form @submit.prevent="submit" label-position="top">
        <el-form-item label="审核结果"><el-radio-group v-model="action"><el-radio value="pass">通过</el-radio><el-radio value="reject">拒绝</el-radio></el-radio-group></el-form-item>
        <el-form-item label="审核意见（至少 5 个字）"><el-input v-model="remark" type="textarea" maxlength="200" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving">确认审核</el-button>
      </el-form>
    </el-dialog>
  </WorkspaceLayout>
</template>
