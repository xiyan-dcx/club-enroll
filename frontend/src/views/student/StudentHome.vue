<script setup>
import { reactive, onMounted } from 'vue'
import WorkspaceLayout from '../../components/WorkspaceLayout.vue'
import { useLoad } from '../../composables/useLoad'
import { listClub } from '../../api/club'
const query = reactive({ keyword: '', category: '', page: 1, size: 12 })
const { data, loading, error, load } = useLoad(() => listClub(query), { records: [], total: 0 })
function search() { query.page = 1; load() }
onMounted(load)
</script>
<template>
  <WorkspaceLayout title="探索社团">
    <el-form inline @submit.prevent="search" class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索社团名称" clearable aria-label="社团名称" />
      <el-select v-model="query.category" placeholder="全部分类" clearable aria-label="分类">
        <el-option v-for="c in ['学术','体育','文艺','公益']" :key="c" :label="c" :value="c" />
      </el-select>
      <el-button type="primary" native-type="submit">搜索</el-button>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <div v-loading="loading" class="card-grid">
      <el-card v-for="club in data.records" :key="club.id" class="brand-card-hover">
        <el-tag>{{ club.category }}</el-tag><h2>{{ club.name }}</h2>
        <p class="line-clamp-2">{{ club.intro || '暂无简介' }}</p>
        <p>负责人：{{ club.leader }} · {{ club.currentMembers }}/{{ club.maxMembers }} 人</p>
        <el-button type="primary" @click="$router.push('/student/club/' + club.id)">查看详情与报名</el-button>
      </el-card>
    </div>
    <el-empty v-if="!loading && !error && !data.records.length" description="暂无符合条件的社团" />
    <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="data.total" layout="prev, pager, next" @current-change="load" />
  </WorkspaceLayout>
</template>
