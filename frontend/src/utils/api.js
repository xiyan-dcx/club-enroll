import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// axios 实例 —— 统一基础配置
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,   // 关键:让浏览器跨域请求时带上 JSESSIONID
})

// 响应拦截器:统一处理后端 ApiResponse{code,msg,data} + 401/403
api.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 后端契约:{code:0, msg:'ok', data:...}
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body              // 整体返回,方便老代码用 .data 字段
      }
      ElMessage.error(body.msg || '服务繁忙,请稍后再试')
      return Promise.reject(body)
    }
    return resp
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.msg
    if (status === 401) {
      ElMessage.warning(msg || '未登录或登录已过期')
      localStorage.removeItem('user')
      router.push('/login')
    } else if (status === 403) {
      ElMessage.error(msg || '无权限访问')
    } else {
      ElMessage.error(msg || '网络错误,请稍后再试')
    }
    return Promise.reject(err)
  }
)

export default api
