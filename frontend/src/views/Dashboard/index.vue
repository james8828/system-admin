<template>
  <div class="dashboard-container">
    <el-card>
      <h2>欢迎使用 JNet 管理系统</h2>
      <p>基于 Spring Security + Vue 3 的企业级权限管理框架</p>
      
      <el-row :gutter="20" style="margin-top: 40px">
        <el-col :span="6">
          <el-statistic title="用户总数" :value="1200">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="角色总数" :value="50">
            <template #prefix>
              <el-icon><Avatar /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="菜单总数" :value="128">
            <template #prefix>
              <el-icon><Menu /></el-icon>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="6">
          <el-statistic title="在线用户" :value="89">
            <template #prefix>
              <el-icon><VideoCamera /></el-icon>
            </template>
          </el-statistic>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { User, Avatar, Menu, VideoCamera } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.dashboard-container {
  h2 {
    margin-bottom: 10px;
  }
  
  p {
    color: #666;
  }
}
</style>
