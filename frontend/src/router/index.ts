import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/callback',
    name: 'Callback',
    component: () => import('@/views/Callback.vue'),
    meta: { title: '登录回调' }
  },
  {
    path: '/',
    name: 'OAuthRedirect',
    component: () => import('@/views/OAuthRedirect.vue'),
    meta: { title: '授权中' }
  },
  {
    path: '/home',
    name: 'HomeLayout',
    component: () => import('@/layouts/index.vue'),
    redirect: '/home/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/index.vue'),
        meta: { title: '首页', icon: 'House' }
      }
    ]
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('@/layouts/index.vue'),
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/System/User/index.vue'),
        meta: { title: '用户管理', icon: 'User', perms: 'system:user:list' }
      },
      {
        path: 'role',
        name: 'RoleManage',
        component: () => import('@/views/System/Role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', perms: 'system:role:list' }
      },
      {
        path: 'menu',
        name: 'MenuManage',
        component: () => import('@/views/System/Menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu', perms: 'system:menu:list' }
      }
    ]
  },
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/Error/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - JNet` : 'JNet'
  
  const userStore = useUserStore()
  const token = localStorage.getItem('access_token')
  
  // 白名单（不需要 token 的页面）
  const whiteList = ['/login', '/callback', '/']
  
  if (token) {
    // 已登录，访问根路径、登录页或回调页时跳转到首页
    if (to.path === '/' || to.path === '/login' || to.path === '/callback') {
      next({ path: '/home', replace: true })
    } else {
      // 判断用户信息是否存在
      if (!userStore.userInfo) {
        try {
          // TODO: 获取用户信息
          // const res = await userApi.getUserInfo()
          // userStore.setUserInfo(res.data)
          
          // TODO: 获取用户菜单
          // const menus = await menuApi.getUserMenus()
          // userStore.setUserMenus(menus.data)
          
          next()
        } catch (error) {
          userStore.logout()
          next(`/login?redirect=${to.path}`)
        }
      } else {
        next()
      }
    }
  } else {
    // 没有 token
    if (whiteList.includes(to.path)) {
      // 允许访问白名单页面
      next()
    } else {
      // 重定向到登录页
      next(`/login?redirect=${to.path}`)
    }
  }
})

export default router
