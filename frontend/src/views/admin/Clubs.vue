<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { listClub, createClub, updateClub, deactivateClub } from '../../api/club'
const query = reactive({ keyword:'', page:1, size:10 })
const { data, loading, error, load } = useLoad(() => listClub(query), {records:[],total:0})
const form = ref({})
const editing = ref(false)
const saving = ref(false)
function edit(row) { form.value = row ? {...row} : {name:'',category:'学术',intro:'',leader:'',maxMembers:30,isActive:1}; editing.value = true }
async function save() {
  if (!form.value.name.trim() || !form.value.leader.trim()) return ElMessage.warning('请填写社团名和负责人')
  saving.value = true
  try { await (form.value.id ? updateClub(form.value) : createClub(form.value)); editing.value = false; ElMessage.success('已保存'); await load() }
  catch { /* 请求封装显示错误 */ }
  finally { saving.value = false }
}
async function deactivate(row) {
  try { await ElMessageBox.confirm(`确定下架“${row.name}”？`, '下架社团', {type:'warning'}); await deactivateClub(row.id); ElMessage.success('已下架'); await load() }
  catch { /* 取消操作或请求错误 */ }
}
onMounted(load)
</script>
<template>
  <WorkspaceLayout title="社团管理" admin>
    <el-form inline class="toolbar" @submit.prevent="query.page=1; load()"><el-input v-model="query.keyword" placeholder="社团名称" clearable aria-label="社团名称" /><el-button native-type="submit">搜索</el-button><el-button type="primary" @click="edit()">新增社团</el-button></el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table :data="data.records" v-loading="loading" empty-text="暂无社团">
      <el-table-column prop="name" label="社团" /><el-table-column prop="category" label="分类" /><el-table-column prop="leader" label="负责人" />
      <el-table-column label="人数"><template #default="{row}">{{ row.currentMembers }}/{{ row.maxMembers }}</template></el-table-column>
      <el-table-column label="状态"><template #default="{row}">{{ row.isActive === 1 ? '招新中' : '已下架' }}</template></el-table-column>
      <el-table-column label="操作" width="180"><template #default="{row}"><el-button size="small" @click="edit(row)">编辑</el-button><el-button size="small" type="danger" :disabled="row.isActive !== 1" @click="deactivate(row)">下架</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="data.total" layout="prev, pager, next" @current-change="load" />
    <el-dialog v-model="editing" :title="form.id ? '编辑社团' : '新增社团'" width="min(560px, 95vw)">
      <el-form label-position="top" @submit.prevent="save">
        <el-form-item label="社团名称" required><el-input v-model="form.name" maxlength="64" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="form.category"><el-option v-for="c in ['学术','体育','文艺','公益']" :key="c" :label="c" :value="c" /></el-select></el-form-item>
        <el-form-item label="负责人" required><el-input v-model="form.leader" maxlength="32" /></el-form-item>
        <el-form-item label="人数上限"><el-input-number v-model="form.maxMembers" :min="Math.max(1,form.currentMembers || 0)" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" maxlength="1000" /></el-form-item>
        <el-button type="primary" native-type="submit" :loading="saving">保存</el-button>
      </el-form>
    </el-dialog>
  </WorkspaceLayout>
</template>
