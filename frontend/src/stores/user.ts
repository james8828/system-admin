import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, MenuInfo } from '@/types'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref<UserInfo | null>(null)
  
  // Token 信息
  const accessToken = ref<string>('')
  const refreshToken = ref<string>('')
  
  // 用户菜单
  const userMenus = ref<MenuInfo[]>([])
  
  // 用户权限标识集合
  const permissions = ref<Set<string>>(new Set())
  
  // 计算属性：是否登录
  const isLoggedIn = computed(() => !!accessToken.value)
  
  // 计算属性：用户名
  const username = computed(() => userInfo.value?.username || '')
  
  // 计算属性：昵称
  const nickname = computed(() => userInfo.value?.nickname || '')
  
  // 计算属性：头像
  const avatar = computed(() => userInfo.value?.avatar || '')
  
  /**
   * 设置用户信息
   */
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }
  
  /**
   * 设置 Token
   */
  function setToken(token: string, refresh?: string) {
    accessToken.value = token
    refreshToken.value = refresh || ''
    
    // 存储到 localStorage
    localStorage.setItem('access_token', token)
    if (refresh) {
      localStorage.setItem('refresh_token', refresh)
    }
  }
  
  /**
   * 设置用户菜单
   */
  function setUserMenus(menus: MenuInfo[]) {
    userMenus.value = menus
    
    // 提取权限标识
    const perms: Set<string> = new Set()
    menus.forEach(menu => {
      if (menu.perms && menu.perms.trim()) {
        perms.add(menu.perms)
      }
      if (menu.children) {
        menu.children.forEach(child => {
          if (child.perms && child.perms.trim()) {
            perms.add(child.perms)
          }
        })
      }
    })
    permissions.value = perms
  }
  
  /**
   * 检查是否有某个权限
   */
  function hasPermission(permission: string): boolean {
    // 超级管理员拥有所有权限
    if (userInfo.value?.roleIds?.includes(1)) {
      return true
    }
    return permissions.value.has(permission)
  }
  
  /**
   * 检查是否有某些权限（满足其一）
   */
  function hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(permission => hasPermission(permission))
  }
  
  /**
   * 检查是否有所有权限
   */
  function hasAllPermissions(permissions: string[]): boolean {
    return permissions.every(permission => hasPermission(permission))
  }
  
  /**
   * 登出
   * 调用 /api/system/oauth2/authorization/revoke 撤销 token（会删除数据库记录和 Redis 数据）
   */
  async function logout() {
    try {
      // 撤销 token
      // 后端会从 SecurityContext 中提取 token 并删除数据库和 Redis 中的授权数据
      await authApi.revokeToken()
      console.log('✓ Token revoked successfully')
    } catch (error) {
      console.error('✗ Revoke token error:', error)
      // 即使撤销失败，也要清除本地数据
    }
    
    // 清除本地用户信息和 Token
    userInfo.value = null
    accessToken.value = ''
    refreshToken.value = ''
    userMenus.value = []
    permissions.value = new Set()
    
    // 清除 localStorage
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('pkce_code_verifier')
    localStorage.removeItem('pkce_state')
    localStorage.removeItem('pkce_authorization_id')
    localStorage.removeItem('pkce_timestamp')
  }
  
  return {
    userInfo,
    accessToken,
    refreshToken,
    userMenus,
    permissions,
    isLoggedIn,
    username,
    nickname,
    avatar,
    setUserInfo,
    setToken,
    setUserMenus,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    logout
  }
}, {
  persist: {
    key: 'user-store',
    storage: localStorage,
    paths: ['accessToken', 'refreshToken']
  }
})
