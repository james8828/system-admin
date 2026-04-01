<template>
  <div class="app-container">
    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
        <div class="logo">
          <span v-show="!isCollapse">JNet</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :unique-opened="true"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <el-sub-menu v-if="route.children && route.children.length > 1" :index="route.path">
              <template #title>
                <el-icon v-if="route.meta?.icon">
                  <component :name="route.meta.icon" />
                </el-icon>
                <span>{{ route.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="child.path.startsWith('/') ? child.path : `${route.path}/${child.path}`"
              >
                <el-icon v-if="child.meta?.icon">
                  <component :name="child.meta.icon" />
                </el-icon>
                <span>{{ child.meta?.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item
              v-else
              :index="route.redirect || (route.children && route.children[0].path)"
            >
              <el-icon v-if="route.meta?.icon">
                <component :name="route.meta.icon" />
              </el-icon>
              <span>{{ route.meta?.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <!-- 顶部导航 -->
        <el-header class="header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="toggleCollapse">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentRoute">{{ currentRoute.meta?.title }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="36" :src="userStore.avatar" />
                <span class="username">{{ userStore.nickname || userStore.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 内容区域 -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { Expand, Fold } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const menuRoutes = computed(() => {
  return router.options.routes.filter(route => 
    route.path !== '/login' && 
    route.path !== '/callback' && 
    route.path !== '/' && 
    route.path !== '/:pathMatch(.*)*'
  )
})
const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route.matched.find(r => r.path === route.path))

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    // TODO: 跳转到个人中心
    ElMessage.info('功能开发中...')
  }
}
</script>

<style scoped lang="scss">
.app-container {
  height: 100vh;
  
  .el-container {
    height: 100%;
  }
  
  .sidebar {
    background-color: #304156;
    transition: width 0.28s;
    
    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 20px;
      font-weight: bold;
    }
    
    .el-menu {
      border-right: none;
    }
  }
  
  .header {
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    
    .header-left {
      display: flex;
      align-items: center;
      
      .collapse-btn {
        font-size: 20px;
        cursor: pointer;
        margin-right: 20px;
        
        &:hover {
          color: #409EFF;
        }
      }
    }
    
    .header-right {
      .user-info {
        display: flex;
        align-items: center;
        cursor: pointer;
        
        .username {
          margin-left: 10px;
        }
      }
    }
  }
  
  .main-content {
    background-color: #f0f2f5;
    padding: 20px;
  }
}
</style>
