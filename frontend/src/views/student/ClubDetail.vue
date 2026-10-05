<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { detailClub } from '../../api/club'
import { apply } from '../../api/enrollment'
const route = useRoute()
const { data, loading, error, load } = useLoad(() => detailClub(route.params.id))
const reason = ref('')
const submitting = ref(false)
const submitted = ref(false)
watch(() => route.params.id, () => { submitted.value = false; reason.value = ''; load() }, { immediate: true })
async function submit() {
  if (reason.value.trim().length < 10) return ElMessage.warning('报名理由至少 10 个字')
  submitting.value = true
  try { await apply({ clubId: Number(route.params.id), reason: reason.value.trim() }); submitted.value = true; ElMessage.success('报名已提交，请等待审核') }
  catch { /* 请求封装显示错误 */ }
  finally { submitting.value = false }
}
</script>
<template>
  <WorkspaceLayout title="社团详情">
    <el-button @click="$router.push('/student/home')">返回社团列表</el-button>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading" v-if="data" class="section-stack">
      <el-card><h2>{{ data.club.name }}</h2><el-tag>{{ data.club.category }}</el-tag>
        <p>{{ data.club.intro || '暂无简介' }}</p>
        <p>负责人：{{ data.club.leader }} · {{ data.club.currentMembers }}/{{ data.club.maxMembers }} 人</p>
      </el-card>
      <el-card><h2>提交报名</h2>
        <el-form @submit.prevent="submit">
          <el-form-item label="报名理由"><el-input v-model="reason" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="请填写 10–200 字的报名理由" /></el-form-item>
          <el-button type="primary" native-type="submit" :loading="submitting" :disabled="submitted || data.club.isActive !== 1 || data.club.currentMembers >= data.club.maxMembers">{{ submitted ? '已提交报名' : '提交报名' }}</el-button>
        </el-form>
      </el-card>
      <el-card><h2>最新公告</h2>
        <article v-for="notice in data.notices" :key="notice.id"><h3>{{ notice.title }}</h3><p class="preserve-lines">{{ notice.content }}</p><small>{{ notice.createdAt }}</small></article>
        <el-empty v-if="!data.notices?.length" description="暂无公告" />
      </el-card>
    </div>
  </WorkspaceLayout>
</template>
