<script setup>
import { onMounted } from 'vue'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { mine } from '../../api/enrollment'
const { data, loading, error, load } = useLoad(mine, [])
onMounted(load)
</script>
<template>
  <WorkspaceLayout title="我的报名">
    <el-button @click="load" :loading="loading">刷新</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table :data="data" v-loading="loading" empty-text="暂无报名记录">
      <el-table-column prop="clubName" label="社团" />
      <el-table-column prop="reason" label="报名理由" min-width="200" />
      <el-table-column label="状态"><template #default="{row}"><el-tag>{{ {1:'待审核',2:'已通过',3:'已拒绝'}[row.status] || '未知' }}</el-tag></template></el-table-column>
      <el-table-column prop="reviewRemark" label="审核意见" />
      <el-table-column prop="applyTime" label="报名时间" min-width="180" />
    </el-table>
  </WorkspaceLayout>
</template>
