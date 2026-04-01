<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">JNet 管理系统</h2>
      
      <!-- OAuth2 快捷登录按钮 -->
      <el-button
          type="success"
          size="large"
          style="width: 100%; margin-bottom: 20px;"
          @click="handleOAuth2Login"
      >
        <el-icon style="margin-right: 8px;"><UserFilled /></el-icon>
        OAuth2 快捷登录
      </el-button>
      
      <el-divider>或者使用账号密码</el-divider>
      
      <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button
              type="primary"
              :loading="loading"
              size="large"
              style="width: 100%"
              @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, onMounted} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import {ElMessage} from 'element-plus'
import {authApi} from '@/api'
import {useUserStore} from '@/stores/user'
import type {FormInstance, FormRules} from 'element-plus'
import {generateCodeVerifier, generateCodeChallenge} from '@/utils/pkce'
import {UserFilled} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)

// OAuth2 PKCE 配置
const OAUTH_CONFIG = {
  clientId: 'jnet-pkce-client',
  redirectUri: window.location.origin + '/callback',
  scope: 'openid profile email', // 使用标准的 OIDC scopes
  authorizationEndpoint: '/oauth2/authorize',
  tokenEndpoint: '/oauth2/token'
}

const loginForm = reactive({
  username: 'admin',
  password: 'admin123'
})

const loginRules: FormRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码长度不能小于 6 位', trigger: 'blur'}
  ]
}

/**
 * OAuth2 快捷登录（跳转到 OAuthRedirect 页面）
 */
const handleOAuth2Login = () => {
  console.log('=== 用户点击 OAuth2 快捷登录 ===')
  router.push('/')
}

/**
 * 存储 PKCE 验证信息到 sessionStorage
 */
function storePkceData(codeVerifier: string, state: string) {
  sessionStorage.setItem('pkce_code_verifier', codeVerifier)
  sessionStorage.setItem('pkce_state', state)
}

/**
 * 发起 PKCE 授权流程
 */
async function initiatePKCEFlow(username: string, password: string) {
  try {
    // 1. 生成 code_verifier 和 code_challenge
    const codeVerifier = generateCodeVerifier()
    const codeChallenge = await generateCodeChallenge(codeVerifier)

    // 2. 生成随机 state
    const state = Math.random().toString(36).substring(2, 15)

    // 3. 生成 authorization_id（用于 Redis 存储 OAuth2 参数）
    const authorizationId = 'auth_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36)

    // 4. 存储 PKCE 数据
    storePkceData(codeVerifier, state)
    sessionStorage.setItem('pkce_authorization_id', authorizationId)
    sessionStorage.setItem('pkce_timestamp', Date.now().toString())

    // 5. 构建授权请求 URL
    const params = new URLSearchParams({
      response_type: 'code',
      client_id: OAUTH_CONFIG.clientId,
      redirect_uri: OAUTH_CONFIG.redirectUri,
      scope: OAUTH_CONFIG.scope,
      state: state,
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
      authorization_id: authorizationId  // 新增：用于 Redis 存储
    })

    // 跳转到授权页面
    window.location.href = `/oauth2/authorize?${params.toString()}`

  } catch (error) {
    console.error('PKCE flow error:', error)
    ElMessage.error('登录失败')
    loading.value = false
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true

      try {
        // 使用 PKCE 模式登录
        await initiatePKCEFlow(loginForm.username, loginForm.password)

        // 注意：实际登录后会通过回调页面处理 token 存储
        // 这里不需要立即存储 token，等待回调处理

      } catch (error) {
        console.error('Login failed:', error)
        ElMessage.error('登录失败')
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  // 检查是否有 OAuth2 相关参数在 URL 中（回调处理）
  const code = route.query.code
  const state = route.query.state

  if (code && state) {
    // 这是 OAuth2 回调，应该跳转到 callback 页面处理
    router.push({
      path: '/callback',
      query: {
        code: code as string,
        state: state as string
      }
    })
  }
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-box {
    width: 400px;
    padding: 40px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 10px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);

    .login-title {
      margin: 0 0 30px;
      text-align: center;
      font-size: 28px;
      color: #333;
      font-weight: 600;
    }

    .login-form {
      .el-form-item {
        margin-bottom: 20px;
      }
    }
  }
}
</style>
