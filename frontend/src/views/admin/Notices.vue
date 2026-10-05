<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { listNotice, publishNotice } from '../../api/notice'
import { listClub } from '../../api/club'
const { data, loading, error, load } = useLoad(() => listNotice(), [])
const clubs = ref([])
const dialog = ref(false)
const saving = ref(false)
const form = ref({clubId:null,title:'',content:''})
async function open() {
  try {
    const all = []; let page = 1; let total = 0
    do { const response = (await listClub({page:page++,size:100})).data; all.push(...response.records); total = response.total; if (!response.records.length) break } while(all.length < total)
    clubs.value = all; form.value = {clubId:null,title:'',content:''}; dialog.value = true
  } catch { /* 请求封装显示错误 */ }
}
async function publish() {
  if (!form.value.clubId || !form.value.title.trim() || !form.value.content.trim()) return ElMessage.warning('请选择社团并填写标题和内容')
  saving.value = true
  try { await publishNotice(form.value); dialog.value = false; ElMessage.success('公告已发布'); await load() }
  catch { /* 请求封装显示错误 */ }
  finally { saving.value = false }
}
onMounted(load)
</script>
<template>
  <WorkspaceLayout title="公告管理" admin>
    <el-button type="primary" @click="open">发布公告</el-button><el-button @click="load">刷新</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table :data="data" v-loading="loading" empty-text="暂无公告"><el-table-column prop="clubName" label="社团" /><el-table-column prop="title" label="标题" /><el-table-column prop="content" label="内容" min-width="260" /><el-table-column prop="createdAt" label="发布时间" min-width="180" /></el-table>
    <el-dialog v-model="dialog" title="发布公告" width="min(560px,95vw)">
      <el-form @submit.prevent="publish" label-position="top">
        <el-form-item label="社团"><el-select v-model="form.clubId" filterable><el-option v-for="club in clubs" :key="club.id" :label="club.name" :value="club.id" /></el-select></el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="100" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving">发布</el-button>
      </el-form>
    </el-dialog>
  </WorkspaceLayout>
</template>
