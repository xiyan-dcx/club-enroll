<template>
  <div class="min-h-screen flex bg-[var(--bg-base)]">
    <!-- 左侧 hero 品牌区 -->
    <aside class="hidden lg:flex lg:w-1/2 brand-hero text-white relative overflow-hidden">
      <!-- 装饰圆 -->
      <div class="absolute -top-24 -left-20 w-96 h-96 rounded-full bg-white/15 blur-2xl"></div>
      <div class="absolute bottom-10 -right-16 w-80 h-80 rounded-full bg-[#FFE066]/30 blur-2xl"></div>
      <div class="absolute top-1/3 right-1/4 w-32 h-32 rounded-full bg-[#4ECDC4]/30 blur-xl"></div>

      <div class="relative z-10 flex flex-col justify-between p-14 w-full">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-2xl bg-white/95 flex items-center justify-center text-[#FF6B6B] text-2xl font-bold shadow-lg">社</div>
          <span class="text-xl font-semibold">校园社团报名通</span>
        </div>

        <div class="space-y-6">
          <h1 class="text-5xl font-bold leading-tight">
            找到属于你的<br/>
            <span class="bg-gradient-to-r from-white to-[#FFE066] bg-clip-text text-transparent">第一个社团</span>
          </h1>
          <p class="text-white/85 text-lg leading-relaxed max-w-md">
            128 个学生社团等你来探索 —— 学术、体育、文艺、公益,总有一个让你眼前一亮。
          </p>
          <div class="flex gap-3 pt-2">
            <span class="px-3 py-1.5 rounded-full bg-white/20 text-sm">学术</span>
            <span class="px-3 py-1.5 rounded-full bg-white/20 text-sm">体育</span>
            <span class="px-3 py-1.5 rounded-full bg-white/20 text-sm">文艺</span>
            <span class="px-3 py-1.5 rounded-full bg-white/20 text-sm">公益</span>
          </div>
        </div>

        <div class="text-white/70 text-sm">
          © 2026 校园社团报名通 · 让每一次相遇都不会错过
        </div>
      </div>
    </aside>

    <!-- 右侧登录卡 -->
    <main class="flex-1 flex items-center justify-center p-6 lg:p-12">
      <div class="w-full max-w-md">
        <!-- 移动端 logo -->
        <div class="lg:hidden flex items-center justify-center gap-2 mb-8">
          <div class="w-10 h-10 rounded-xl brand-hero flex items-center justify-center text-white font-bold">社</div>
          <span class="text-xl font-semibold text-[var(--text-primary)]">校园社团报名通</span>
        </div>

        <div class="glass-card p-10">
          <h2 class="text-3xl font-bold mb-2">欢迎回来</h2>
          <p class="text-[var(--text-secondary)] mb-8">登录后开始浏览社团 🎉</p>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            @submit.prevent="onSubmit"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="例如 student1 / 123456"
                :prefix-icon="User"
                autofocus
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                placeholder="6 位及以上"
                :prefix-icon="Lock"
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              :loading="loading"
              native-type="submit"
              class="!w-full !h-12 !text-base !font-semibold"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form>

          <div class="flex items-center gap-3 my-6">
            <div class="flex-1 h-px bg-gray-200"></div>
            <span class="text-xs text-[var(--text-secondary)]">还没有账号?</span>
            <div class="flex-1 h-px bg-gray-200"></div>
          </div>

          <button
            class="w-full h-11 rounded-xl border-2 border-[#FF6B6B]/20 text-[#FF6B6B] font-semibold transition-all hover:bg-[#FF6B6B]/5 hover:border-[#FF6B6B]/40"
            @click="router.push('/register')"
          >
            立即注册新账号
          </button>

          <p class="text-center text-xs text-[var(--text-secondary)] mt-6">
            测试账号:学生 <span class="font-mono">student1或者student2 / 123456</span> · 管理员 <span class="font-mono">admin / 123456</span>
          </p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import api from '@/utils/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

async function onSubmit() {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const resp = await api.post('/auth/login', {
      username: form.username,
      password: form.password,
    })
    const user = resp.data
    localStorage.setItem('user', JSON.stringify(user))
    ElMessage.success(`欢迎,${user.realName || user.username}!`)
    if (user.role === 'admin') {
      router.replace('/admin/dashboard')
    } else {
      router.replace('/student/home')
    }
  } catch (err) {
    // 拦截器已弹消息
  } finally {
    loading.value = false
  }
}
</script>
