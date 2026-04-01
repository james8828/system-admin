/**
 * 全局 TypeScript 类型声明
 */

// API 响应基础结构
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页查询参数
export interface PageQuery {
  pageNum: number
  pageSize: number
}

// 分页响应
export interface PageResult<T> {
  rows: T[]
  total: number
  pageNum: number
  pageSize: number
}

// 用户信息
export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  gender?: number
  deptId?: number
  roleIds?: number[]
}

// 角色信息
export interface RoleInfo {
  roleId: number
  roleName: string
  roleCode: string
  remark?: string
}

// 菜单/权限信息
export interface MenuInfo {
  menuId: number
  parentId: number
  menuName: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  type: number // 0=目录 1=菜单 2=按钮 3=接口
  visible: number
  sort: number
  children?: MenuInfo[]
}

// Token 信息
export interface TokenInfo {
  access_token: string
  token_type: string
  expires_in: number
  refresh_token?: string
  scope?: string
}

// 登录参数
export interface LoginParams {
  username: string
  password: string
  code?: string
  uuid?: string
  grant_type?: string
  client_id?: string
  client_secret?: string
}
