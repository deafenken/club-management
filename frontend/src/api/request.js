import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 120000  // AI接口可能较慢
})

// 请求拦截器：自动携带JWT token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 防止多个请求同时 401/403 时重复弹窗和重复跳转
let redirecting = false

// 响应拦截器：统一错误处理
request.interceptors.response.use(
  res => {
    if (res.data.code === 200) return res.data
    ElMessage.error(res.data.msg || '请求失败')
    return Promise.reject(res.data)
  },
  err => {
    const status = err.response?.status
    // 未登录 / 令牌过期：清除本地会话并跳转登录页（不再弹出刺眼的错误码）
    if (status === 401 || status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!redirecting && !location.pathname.startsWith('/login')) {
        redirecting = true
        ElMessage.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      }
      return Promise.reject(err)
    }
    const detail = err.response
      ? `${err.response.status} ${err.response.statusText} [${err.config?.method?.toUpperCase() || '?'} ${err.config?.url || '?'}]`
      : (err.code === 'ECONNABORTED' ? '请求超时' : `网络连接失败 [${err.config?.url || '?'}]`)
    ElMessage.error(detail)
    console.error('Request error:', detail, err)
    return Promise.reject(err)
  }
)

export default request
