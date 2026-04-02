import { get, post, put, del, oauthPost } from '@/utils/request'
import type { ApiResponse, PageQuery, PageResult, UserInfo, RoleInfo, MenuInfo } from '@/types'

/**
 * 用户管理 API
 */
export const userApi = {
  /**
   * 分页查询用户列表
   */
  pageUser: (params: PageQuery & Partial<UserInfo>) => 
    get<PageResult<UserInfo>>('/system/user/page', params),
  
  /**
   * 根据 ID 查询用户
   */
  getUserById: (userId: number) => 
    get<UserInfo>(`/system/user/${userId}`),
  
  /**
   * 根据用户名查询用户
   */
  getUserByUsername: (username: string) => 
    get<UserInfo>(`/system/user/username/${username}`),
  
  /**
   * 新增用户
   */
  addUser: (data: UserInfo) => 
    post<boolean>('/system/user', data),
  
  /**
   * 修改用户
   */
  updateUser: (data: UserInfo) => 
    put<boolean>('/system/user', data),
  
  /**
   * 删除用户
   */
  deleteUser: (userId: number) => 
    del<boolean>(`/system/user/${userId}`),
  
  /**
   * 重置密码
   */
  resetPassword: (userId: number, password: string) => 
    post<boolean>(`/system/user/${userId}/resetPwd`, password),
  
  /**
   * 启用/禁用用户
   */
  enableUser: (userId: number, enabled: boolean) => 
    put<boolean>(`/system/user/${userId}/enable`, enabled)
}

/**
 * 角色管理 API
 */
export const roleApi = {
  /**
   * 分页查询角色列表
   */
  pageRole: (params: PageQuery & Partial<RoleInfo>) => 
    get<PageResult<RoleInfo>>('/system/role/page', params),
  
  /**
   * 根据 ID 查询角色
   */
  getRoleById: (roleId: number) => 
    get<RoleInfo>(`/system/role/${roleId}`),
  
  /**
   * 查询所有角色
   */
  listRoles: () => 
    get<RoleInfo[]>('/system/role/list'),
  
  /**
   * 新增角色
   */
  addRole: (data: RoleInfo) => 
    post<boolean>('/system/role', data),
  
  /**
   * 修改角色
   */
  updateRole: (data: RoleInfo) => 
    put<boolean>('/system/role', data),
  
  /**
   * 删除角色
   */
  deleteRole: (roleId: number) => 
    del<boolean>(`/system/role/${roleId}`),
  
  /**
   * 分配菜单权限
   */
  assignMenus: (roleId: number, menuIds: number[]) => 
    post<boolean>(`/system/role/menu/${roleId}`, menuIds),
  
  /**
   * 分配用户
   */
  assignUsers: (roleId: number, userIds: number[]) => 
    post<boolean>(`/system/role/user/${roleId}`, userIds)
}

/**
 * 菜单管理 API
 */
export const menuApi = {
  /**
   * 获取菜单树
   */
  getMenuTree: () => 
    get<MenuInfo[]>('/system/menu/tree'),
  
  /**
   * 根据 ID 查询菜单
   */
  getMenuById: (menuId: number) => 
    get<MenuInfo>(`/system/menu/${menuId}`),
  
  /**
   * 新增菜单
   */
  addMenu: (data: MenuInfo) => 
    post<boolean>('/system/menu', data),
  
  /**
   * 修改菜单
   */
  updateMenu: (data: MenuInfo) => 
    put<boolean>('/system/menu', data),
  
  /**
   * 删除菜单
   */
  deleteMenu: (menuId: number) => 
    del<boolean>(`/system/menu/${menuId}`),
  
  /**
   * 获取用户权限菜单
   */
  getUserMenus: (userId: number) => 
    get<MenuInfo[]>(`/system/menu/user/${userId}`)
}

/**
 * OAuth2 认证 API
 */
export const authApi = {
  /**
   * 获取访问令牌（授权码模式/PKCE）
   */
  getToken: (data: any) => 
    oauthPost<any>('/oauth2/token', data),
  
  /**
   * 刷新令牌
   */
  refreshToken: (refreshToken: string) => 
    oauthPost<any>('/oauth2/token', {
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
      client_id: 'jnet-pkce-client'
    }),
  
  /**
   * 撤销令牌
   * 调用 system 模块的撤销接口，会删除数据库和 Redis 中的授权数据
   * DELETE /api/system/oauth2/authorization/revoke
   * 注意：后端从 SecurityContext 中自动提取 token，不需要前端传递参数
   */
  revokeToken: () => 
    del('/system/oauth2/authorization/revoke')
}
