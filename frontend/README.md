# JNet Admin Vue - 前端项目

基于 **Vue 3 + TypeScript + Element Plus** 的企业级后台管理系统前端解决方案。

## 📋 技术栈

- **框架**: Vue 3.5+ (Composition API)
- **构建工具**: Vite 6.x
- **UI 组件库**: Element Plus 2.x
- **状态管理**: Pinia 3.x
- **路由**: Vue Router 4.x
- **HTTP 客户端**: Axios
- **图表库**: ECharts 6.x
- **时间处理**: Day.js
- **CSS 预处理**: Sass
- **自动导入**: unplugin-auto-import / unplugin-vue-components

## 🚀 快速开始

### 环境要求

- Node.js >= 18.x
- npm >= 9.x

### 安装依赖

```bash
cd frontend
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173/

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 📁 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口定义
│   │   └── index.ts     # 统一导出所有 API
│   ├── layouts/         # 布局组件
│   │   └── index.vue    # 主布局（侧边栏 + 顶栏）
│   ├── router/          # 路由配置
│   │   └── index.ts     # 路由定义和守卫
│   ├── stores/          # Pinia 状态管理
│   │   └── user.ts      # 用户状态管理
│   ├── types/           # TypeScript 类型定义
│   │   └── index.ts     # 全局类型声明
│   ├── utils/           # 工具函数
│   │   └── request.ts   # Axios 封装
│   ├── views/           # 页面组件
│   │   ├── Dashboard/   # 首页
│   │   ├── Error/       # 错误页面
│   │   ├── System/      # 系统管理
│   │   │   ├── User/    # 用户管理
│   │   │   ├── Role/    # 角色管理
│   │   │   └── Menu/    # 菜单管理
│   │   └── Login.vue    # 登录页
│   ├── App.vue          # 根组件
│   └── main.ts          # 入口文件
├── package.json         # 项目依赖
├── tsconfig.json        # TypeScript 配置
├── vite.config.ts       # Vite 配置
└── README.md            # 项目文档
```

## 🔑 核心功能

### 1. 认证与授权

- OAuth2 密码模式登录
- JWT Token 自动管理
- 401/403 响应拦截
- 登录状态过期自动跳转

### 2. 权限控制

- 基于角色的访问控制（RBAC）
- 菜单级权限控制
- 按钮级权限控制
- 动态路由加载

### 3. 用户管理

- 用户列表查询
- 用户增删改查
- 用户状态管理
- 密码重置

### 4. 角色管理

- 角色列表查询
- 角色增删改查
- 权限分配
- 用户分配

### 5. 菜单管理

- 树形菜单展示
- 菜单增删改查
- 权限标识配置

## 💡 开发指南

### API 调用示例

```typescript
import { userApi } from '@/api'

// GET 请求
const res = await userApi.getUserById(1)

// POST 请求
await userApi.addUser(userData)

// PUT 请求
await userApi.updateUser(userData)

// DELETE 请求
await userApi.deleteUser(userId)
```

### 权限判断

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 检查单个权限
if (userStore.hasPermission('system:user:list')) {
  // 有权限
}

// 检查多个权限（满足其一）
if (userStore.hasAnyPermission(['system:user:list', 'system:user:create'])) {
  // 有任一权限
}

// 检查所有权限
if (userStore.hasAllPermissions(['system:user:list', 'system:user:create'])) {
  // 同时拥有所有权限
}
```

### 状态管理

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 获取用户信息
console.log(userStore.userInfo)
console.log(userStore.username)
console.log(userStore.nickname)

// 设置 Token
userStore.setToken(accessToken, refreshToken)

// 登出
userStore.logout()
```

## 🔧 配置说明

### Vite 配置 (vite.config.ts)

- **代理配置**: `/api` 代理到后端服务 `http://localhost:8080`
- **自动导入**: 自动导入 Vue、Pinia、Element Plus 组件
- **代码分割**: 优化打包体积
- **Gzip 压缩**: 自动生成.gz 文件

### 环境变量

创建 `.env` 文件配置环境变量：

```env
# 开发环境
VITE_APP_BASE_API=/api
VITE_APP_TITLE=JNet 管理系统

# 生产环境
VITE_APP_BASE_API=https://api.example.com
VITE_APP_TITLE=JNet 管理系统 - 生产
```

## 📝 开发规范

### 命名规范

- **组件名**: PascalCase（如 `UserManage.vue`）
- **变量名**: camelCase
- **常量名**: UPPER_SNAKE_CASE
- **文件名**: camelCase 或 PascalCase

### 代码风格

- 使用 Composition API
- 使用 `<script setup>` 语法
- TypeScript 严格模式
- ESLint + Prettier 格式化

## 🎯 后续规划

- [ ] 完善表单验证
- [ ] 添加更多业务组件
- [ ] 国际化支持
- [ ] 主题切换
- [ ] 暗黑模式
- [ ] 单元测试
- [ ] E2E 测试

## 📄 License

MIT
