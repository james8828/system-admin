<template>
  <div class="role-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>
      
      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="roleId" label="角色 ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="sort" label="显示顺序" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handlePermission(row)">权限分配</el-button>
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
import { roleApi } from '@/api'
import type { RoleInfo } from '@/types'

const loading = ref(false)
const tableData = ref<RoleInfo[]>([])

const getList = async () => {
  loading.value = true
  try {
    const res = await roleApi.listRoles()
    if (res.data) {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('Failed to get role list:', error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  ElMessage.info('功能开发中...')
}

const handleEdit = (row: RoleInfo) => {
  ElMessage.info(`编辑角色：${row.roleName}`)
}

const handlePermission = (row: RoleInfo) => {
  ElMessage.info(`分配权限：${row.roleName}`)
}

const handleDelete = async (row: RoleInfo) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色"${row.roleName}"吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await roleApi.deleteRole(row.roleId)
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
.role-manage {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
