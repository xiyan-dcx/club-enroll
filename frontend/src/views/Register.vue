<template>
  <div class="min-h-screen flex items-center justify-center p-6 relative overflow-hidden bg-[var(--bg-base)]">
    <!-- 背景装饰 -->
    <div class="absolute top-10 left-10 w-72 h-72 rounded-full brand-hero opacity-25 blur-3xl"></div>
    <div class="absolute bottom-10 right-10 w-80 h-80 rounded-full brand-mint opacity-25 blur-3xl"></div>

    <div class="relative w-full max-w-lg">
      <div class="text-center mb-6">
        <div class="inline-flex items-center gap-2 mb-3">
          <div class="w-11 h-11 rounded-xl brand-hero flex items-center justify-center text-white font-bold shadow-lg">社</div>
          <span class="text-xl font-semibold text-[var(--text-primary)]">校园社团报名通</span>
        </div>
        <h2 class="text-3xl font-bold mb-1">创建新账号</h2>
        <p class="text-[var(--text-secondary)]">几分钟搞定,开启社团之旅 ✨</p>
      </div>

      <div class="glass-card p-8">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="onSubmit"
        >
          <div class="grid grid-cols-2 gap-4">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="3-32 位" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="你的名字" />
            </el-form-item>
          </div>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="≥ 6 位" />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            native-type="submit"
            class="!w-full !h-12 !text-base !font-semibold !mt-2"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form>

        <p class="text-center text-sm text-[var(--text-secondary)] mt-6">
          已有账号?
          <span class="text-[#FF6B6B] font-semibold cursor-pointer hover:underline" @click="router.push('/login')">
            返回登录
          </span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度 3-32 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^\d{11}$/, message: '11 位数字', trigger: 'blur' },
  ],
}

async function onSubmit() {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await api.post('/auth/register', { ...form })
    ElMessage.success('注册成功,请登录')
    router.replace('/login')
  } catch (err) {
    // 拦截器已弹消息
  } finally {
    loading.value = false
  }
}
</script>
