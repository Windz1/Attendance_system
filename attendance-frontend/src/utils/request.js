import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  // 开发环境为 /api；构建后为 /attendance/api，避免与同域其他系统冲突。
  baseURL: `${import.meta.env.BASE_URL}api`,
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
  (res) => {
    if (res.config.responseType === 'blob' || res.config.responseType === 'arraybuffer') {
      return res.data
    }
    const data = res.data
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || 'Error'))
    }
    return data.data
  },
  (err) => {
    ElMessage.error(err.response?.data?.message || err.message)
    return Promise.reject(err)
  }
)

export default service
