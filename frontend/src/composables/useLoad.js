import { ref } from 'vue'
export function useLoad(fetcher, initial = null) {
  const data = ref(initial)
  const loading = ref(false)
  const error = ref('')
  async function load() {
    loading.value = true
    error.value = ''
    try { data.value = (await fetcher()).data }
    catch (e) { error.value = e.response?.data?.message || e.message || '加载失败，请重试' }
    finally { loading.value = false }
  }
  return { data, loading, error, load }
}
