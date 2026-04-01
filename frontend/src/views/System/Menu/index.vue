<template>
  <div class="menu-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增菜单
          </el-button>
        </div>
      </template>
      
      <!-- 表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        row-key="menuId"
        :tree-props="{ children: 'children' }"
        border
      >
        <el-table-column prop="menuName" label="菜单名称" />
        <el-table-column prop="icon" label="图标" width="100">
          <template #default="{ row }">
            <el-icon v-if="row.icon">
              <component :name="row.icon" />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.type === 0">目录</el-tag>
            <el-tag v-else-if="row.type === 1" type="success">菜单</el-tag>
            <el-tag v-else-if="row.type === 2" type="warning">按钮</el-tag>
            <el-tag v-else-if="row.type === 3" type="danger">接口</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由地址" />
        <el-table-column prop="perms" label="权限标识" />
        <el-table-column prop="sort" label="显示顺序" width="100" />
        <el-table-column prop="visible" label="可见" width="100">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { menuApi } from '@/api'
import type { MenuInfo } from '@/types'

const loading = ref(false)
const tableData = ref<MenuInfo[]>([])

const getList = async () => {
  loading.value = true
  try {
    const res = await menuApi.getMenuTree()
    if (res.data) {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('Failed to get menu list:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  ElMessage.info('功能开发中...')
}

const handleEdit = (row: MenuInfo) => {
  ElMessage.info(`编辑菜单：${row.menuName}`)
}

const handleDelete = async (row: MenuInfo) => {
  try {
    await ElMessageBox.confirm(`确定要删除菜单"${row.menuName}"吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await menuApi.deleteMenu(row.menuId)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Delete failed:', error)
    }
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.menu-manage {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
