<template>
  <div class="min-h-screen bg-[var(--bg-base)]">
    <!-- 顶栏 -->
    <header class="bg-white/80 backdrop-blur-md border-b border-[#F2EAE0] sticky top-0 z-30">
      <div class="max-w-6xl mx-auto px-6 lg:px-8 h-16 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div class="w-9 h-9 rounded-xl brand-hero flex items-center justify-center text-white font-bold">社</div>
          <span class="font-semibold text-lg">青聚 · 社团招新与活动预约</span>
        </div>
        <div class="flex items-center gap-3">
          <RouterLink to="/student/portal" class="text-sm font-medium">活动预约与管理</RouterLink>
          <button
            class="hidden sm:flex items-center gap-1.5 px-4 h-10 rounded-xl text-sm font-medium text-[var(--text-primary)] hover:bg-[#FF6B6B]/10 transition-all"
            @click="router.push('/student/my-enrollment')"
          >
            <el-icon><Document /></el-icon>
            我的报名
          </button>
          <el-dropdown @command="onCmd">
            <span class="flex items-center gap-2 px-3 h-10 rounded-xl cursor-pointer hover:bg-[#FF6B6B]/10 transition-all">
              <div class="w-8 h-8 rounded-full brand-mint flex items-center justify-center text-white text-sm font-bold">
                {{ (user?.realName || 'U').charAt(0) }}
              </div>
              <span class="text-sm font-medium hidden sm:inline">{{ user?.realName || '同学' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="enrollment">我的报名</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- Hero 区 -->
    <section class="brand-hero text-white relative overflow-hidden">
      <div class="absolute -top-10 -left-10 w-72 h-72 rounded-full bg-white/15 blur-3xl"></div>
      <div class="absolute -bottom-10 -right-10 w-80 h-80 rounded-full bg-[#FFE066]/30 blur-3xl"></div>

      <div class="relative max-w-6xl mx-auto px-6 lg:px-8 py-16">
        <h1 class="text-4xl lg:text-5xl font-bold mb-3">找到属于你的社团</h1>
        <p class="text-white/85 text-lg mb-8 max-w-xl">
          校园生活,从加入第一个社团开始 —— 学术、体育、文艺、公益,等你来探索。
        </p>

        <!-- 搜索条 -->
        <div class="bg-white/95 rounded-2xl p-2 flex flex-col sm:flex-row gap-2 max-w-3xl shadow-2xl">
          <el-input
            v-model="filter.keyword"
            placeholder="搜社团名称、关键词..."
            size="large"
            :prefix-icon="Search"
            class="flex-1"
            @keyup.enter="loadData(1)"
          />
          <el-select v-model="filter.category" placeholder="全部分类" size="large" clearable class="!w-full sm:!w-36">
            <el-option label="全部" value="" />
            <el-option label="学术" value="学术" />
            <el-option label="体育" value="体育" />
            <el-option label="文艺" value="文艺" />
            <el-option label="公益" value="公益" />
          </el-select>
          <el-button type="primary" size="large" class="sm:!w-28" @click="loadData(1)">搜索</el-button>
        </div>

        <!-- 分类 chips -->
        <div class="flex flex-wrap gap-2 mt-6">
          <button
            v-for="cat in ['全部', '学术', '体育', '文艺', '公益']"
            :key="cat"
            class="px-4 py-2 rounded-full text-sm font-medium transition-all"
            :class="(filter.category || '全部') === cat
              ? 'bg-white text-[#FF6B6B] shadow-md'
              : 'bg-white/20 text-white hover:bg-white/30'"
            @click="quickFilter(cat)"
          >
            {{ cat }}
          </button>
        </div>
      </div>
    </section>

    <!-- 社团网格 -->
    <main class="max-w-6xl mx-auto px-6 lg:px-8 py-10">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-2xl font-bold">
          热门社团
          <span class="text-sm font-normal text-[var(--text-secondary)] ml-2">共 {{ total }} 个 · 按成员数排序</span>
        </h2>
      </div>

      <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 min-h-[200px]">
        <article
          v-for="c in records"
          :key="c.id"
          class="bg-white rounded-2xl p-6 shadow-md brand-card-hover cursor-pointer border border-[#F2EAE0]"
          @click="router.push(`/student/club/${c.id}`)"
        >
          <div class="flex items-start gap-3 mb-4">
            <div
              class="w-14 h-14 rounded-2xl flex items-center justify-center text-white text-2xl font-bold flex-shrink-0"
              :class="avatarClass(c.category)"
            >
              {{ (c.name || '?').charAt(0) }}
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="text-lg font-bold truncate">{{ c.name }}</h3>
              <span class="inline-block text-xs px-2 py-0.5 rounded-full mt-1" :class="categoryChipClass(c.category)">
                {{ c.category }}
              </span>
            </div>
            <span
              class="text-xs px-2 py-1 rounded-lg font-medium flex-shrink-0"
              :class="isFull(c)
                ? 'bg-[#FF8E72]/15 text-[#D8543A]'
                : 'bg-[#4ECDC4]/15 text-[#2E8B7C]'"
            >
              {{ isFull(c) ? '已满' : '招新中' }}
            </span>
          </div>
          <p class="text-sm text-[var(--text-secondary)] line-clamp-2 min-h-[40px] mb-4 leading-relaxed">
            {{ c.intro || '暂无简介' }}
          </p>
          <div class="flex items-center justify-between text-xs text-[var(--text-secondary)] pt-4 border-t border-[#F2EAE0]">
            <span class="flex items-center gap-1">
              <el-icon><User /></el-icon>
              {{ c.currentMembers ?? 0 }} / {{ c.maxMembers ?? '∞' }}
            </span>
            <span>社长 · {{ c.leader || '—' }}</span>
          </div>
        </article>
      </div>

      <el-empty v-if="!loading && records.length === 0" description="没有匹配的社团,换个关键词试试?" />

      <div v-if="total > size" class="flex justify-center mt-10">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[6, 12, 24]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadData()"
          @size-change="loadData(1)"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, ArrowDown, Search, User } from '@element-plus/icons-vue'
import api from '@/utils/api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || 'null')

const filter = reactive({ keyword: '', category: '' })
const page = ref(1)
const size = ref(12)
const total = ref(0)
const records = ref([])
const loading = ref(false)

const isFull = (c) => (c.currentMembers ?? 0) >= (c.maxMembers ?? Infinity)

const avatarClass = (cat) => ({
  '学术': 'brand-violet',
  '体育': 'brand-mint',
  '文艺': 'brand-hero',
  '公益': 'brand-sun',
}[cat] || 'brand-hero')

const categoryChipClass = (cat) => ({
  '学术': 'bg-[#A78BFA]/15 text-[#7C3AED]',
  '体育': 'bg-[#4ECDC4]/15 text-[#2E8B7C]',
  '文艺': 'bg-[#FF6B6B]/15 text-[#D8543A]',
  '公益': 'bg-[#FFD86B]/25 text-[#8A6500]',
}[cat] || 'bg-gray-100 text-gray-600')

async function loadData(jumpTo) {
  if (jumpTo) page.value = jumpTo
  loading.value = true
  try {
    const resp = await api.get('/club/list', {
      params: {
        keyword: filter.keyword || undefined,
        category: filter.category || undefined,
        page: page.value,
        size: size.value,
      },
    })
    const d = resp.data
    // 后端可能返回 {records, total} 或纯数组,做容错
    if (Array.isArray(d)) {
      records.value = d
      total.value = d.length
    } else {
      records.value = d?.records || []
      total.value = d?.total ?? records.value.length
    }
  } catch (e) {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function quickFilter(cat) {
  filter.category = cat === '全部' ? '' : cat
  loadData(1)
}

async function onCmd(cmd) {
  if (cmd === 'enrollment') {
    router.push('/student/my-enrollment')
    return
  }
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出登录?', '提示', { type: 'warning' })
      try { await api.post('/auth/logout') } catch (_) {}
      localStorage.removeItem('user')
      ElMessage.success('已退出')
      router.replace('/login')
    } catch (_) { /* user cancel */ }
  }
}

onMounted(() => loadData(1))
</script>
