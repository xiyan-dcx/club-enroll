<script setup>
import { onMounted } from 'vue'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { dashboard } from '../../api/stat'
const { data, loading, error, load } = useLoad(dashboard, {})
const metrics = { clubCount:'社团总数', studentCount:'学生总数', pendingCount:'待审核报名', noticeCount:'公告总数', todayNew:'今日新报名', monthlyPassed:'本月通过' }
onMounted(load)
</script>
<template>
  <WorkspaceLayout title="管理概览" admin>
    <el-button @click="load" :loading="loading">刷新统计</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div class="card-grid" v-loading="loading"><el-card v-for="(label,key) in metrics" :key="key"><el-statistic :title="label" :value="data[key] ?? 0" /></el-card></div>
  </WorkspaceLayout>
</template>
