import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiResponse } from '@/types'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api', // API 基础路径
  timeout: 30000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 如果返回的状态码不是 200，说明接口有错误
    if (res.code !== 200) {
      // 401: 未授权，需要重新登录
      if (res.code === 401) {
        ElMessageBox.confirm('登录状态已过期，请重新登录', '系统提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          // 清除 token 并跳转登录页
          localStorage.removeItem('access_token')
          localStorage.removeItem('refresh_token')
          window.location.href = '/login'
        })
        return Promise.reject(new Error(res.message || 'Error'))
      }
      
      // 403: 无权限
      if (res.code === 403) {
        ElMessage.error('无权限访问')
        return Promise.reject(new Error(res.message || 'Error'))
      }

      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res
  },
  (error) => {
    console.error('Response error:', error)
    
    if (error.response) {
      switch (error.response.status) {
        case 400:
          ElMessage.error('请求参数错误')
          break
        case 401:
          ElMessage.error('未授权，请重新登录')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求地址不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 502:
          ElMessage.error('网关错误')
          break
        case 503:
          ElMessage.error('服务不可用')
          break
        case 504:
          ElMessage.error('网关超时')
          break
        default:
          ElMessage.error(`连接错误${error.response.status}`)
      }
    } else if (error.request) {
      ElMessage.error('无法连接到服务器')
    } else {
      ElMessage.error('请求出错:' + error.message)
    }
    
    return Promise.reject(error)
  }
)

/**
 * 封装 GET 请求
 */
export function get<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
  return service.get(url, { params })
}

/**
 * 封装 POST 请求
 */
export function post<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
  return service.post(url, data)
}

/**
 * 封装 PUT 请求
 */
export function put<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
  return service.put(url, data)
}

/**
 * 封装 DELETE 请求
 */
export function del<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
  return service.delete(url, { params })
}

export default service

/**
 * 创建 OAuth2 专用的 axios 实例（不使用 /api 前缀）
 */
const oauthService: AxiosInstance = axios.create({
  baseURL: '/', // OAuth2 端点在根路径
  timeout: 30000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
})

// OAuth2 请求拦截器
oauthService.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('OAuth request error:', error)
    return Promise.reject(error)
  }
)

// OAuth2 响应拦截器
oauthService.interceptors.response.use(
  (response: AxiosResponse<any>) => {
    return response
  },
  (error) => {
    console.error('OAuth response error:', error)
    
    if (error.response?.status === 401) {
      ElMessage.error('认证失败，请重新登录')
    } else if (error.response?.status === 400) {
      ElMessage.error('请求参数错误')
    } else {
      ElMessage.error('OAuth 请求失败')
    }
    
    return Promise.reject(error)
  }
)

/**
 * OAuth2 专用的 POST 请求
 */
export function oauthPost<T = any>(url: string, data?: any): Promise<T> {
  return oauthService.post(url, data, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}
