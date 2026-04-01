<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-description">抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f0f2f5;
  
  .error-content {
    text-align: center;
    
    .error-code {
      font-size: 120px;
      font-weight: bold;
      color: #409EFF;
      margin: 0;
    }
    
    .error-title {
      font-size: 32px;
      color: #333;
      margin: 20px 0;
    }
    
    .error-description {
      font-size: 16px;
      color: #666;
      margin: 20px 0 40px;
    }
  }
}
</style>
