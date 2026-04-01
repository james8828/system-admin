<template>
  <div class="callback-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>登录处理中...</span>
        </div>
      </template>
      <div class="callback-content">
        <el-icon class="loading-icon" :size="50"><Loading /></el-icon>
        <p>正在完成登录，请稍候...</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { Loading } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// OAuth2 PKCE 配置
const OAUTH_CONFIG = {
  clientId: 'jnet-pkce-client',
  redirectUri: window.location.origin + '/callback',
  scope: 'openid profile email'
  // PKCE 模式不需要 client_secret
}

onMounted(async () => {
  // debugger
  const code = route.query.code as string
  const state = route.query.state as string
  
  if (!code || !state) {
    ElMessage.error('无效的授权回调')
    router.push('/login')
    return
  }
  
  await handleCallback(code, state)
})

/**
 * 处理 OAuth2 回调
 */
async function handleCallback(code: string, state: string) {
  try {
    console.log('Callback received - code:', code ? 'exists' : 'missing')
    console.log('Callback received - state:', state)
    
    // 验证 state
    const storedState = sessionStorage.getItem('pkce_state')
    const timestamp = sessionStorage.getItem('pkce_timestamp')
    console.log('Stored state from sessionStorage:', storedState)
    console.log('Stored timestamp:', timestamp)
    
    if (!storedState) {
      throw new Error('PKCE state not found in sessionStorage. Session may have expired or been cleared.')
    }
    
    // 检查是否过期（30 分钟）
    if (timestamp) {
      const elapsed = Date.now() - parseInt(timestamp)
      const thirtyMinutes = 30 * 60 * 1000
      console.log('PKCE 数据检查 - 已过去时间:', elapsed, 'ms', '(', Math.floor(elapsed / 1000 / 60), '分钟', ')')
      
      if (elapsed > thirtyMinutes) {
        console.warn('❌ PKCE 数据已过期！已过去:', elapsed, 'ms', '超过限制:', thirtyMinutes, 'ms')
        sessionStorage.removeItem('pkce_code_verifier')
        sessionStorage.removeItem('pkce_state')
        sessionStorage.removeItem('pkce_timestamp')
        throw new Error('PKCE verification data expired. Please try logging in again.')
      }
      console.log('✅ PKCE 数据有效 - 剩余时间:', Math.floor((thirtyMinutes - elapsed) / 1000 / 60), '分钟')
    }
    
    if (state !== storedState) {
      console.error('State mismatch! Expected:', storedState, 'Got:', state)
      throw new Error('Invalid state parameter - CSRF attack detected or session issue')
    }
    
    // 获取 code_verifier
    const codeVerifier = sessionStorage.getItem('pkce_code_verifier')
    console.log('\n')
    console.log('╔═══════════════════════════════════════════════════════════╗')
    console.log('║                  Token 请求参数信息                        ║')
    console.log('╠═══════════════════════════════════════════════════════════╣')
    console.log(`║ grant_type:           authorization_code                 ║`)
    console.log(`║ code:                 ${code.padEnd(45)} ║`)
    console.log(`║ client_id:            ${OAUTH_CONFIG.clientId.padEnd(45)} ║`)
    console.log(`║ redirect_uri:         ${OAUTH_CONFIG.redirectUri.padEnd(45)} ║`)
    console.log(`║ code_verifier:        ${codeVerifier.padEnd(45)} ║`)
    console.log('╚═══════════════════════════════════════════════════════════╝')
    console.log('\n')
    
    if (!codeVerifier) {
      throw new Error('Code verifier not found in sessionStorage')
    }
    
    // 使用授权码换取令牌（PKCE 模式不需要 client_secret）
    const params = new URLSearchParams({
      grant_type: 'authorization_code',
      code: code,
      client_id: OAUTH_CONFIG.clientId,
      redirect_uri: OAUTH_CONFIG.redirectUri,
      code_verifier: codeVerifier
    })
    
    const tokenRes = await authApi.getToken(params)
    
    if (tokenRes.data) {
      // 保存 token
      userStore.setToken(
        tokenRes.data.access_token,
        tokenRes.data.refresh_token
      )
      
      // 清除 PKCE 数据
      sessionStorage.removeItem('pkce_code_verifier')
      sessionStorage.removeItem('pkce_state')
      sessionStorage.removeItem('pkce_timestamp')
      
      ElMessage.success('登录成功')
      
      // 跳转到首页
      router.push('/home')
    }
  } catch (error) {
    console.error('Callback handling failed:', error)
    ElMessage.error('登录失败')
    
    // 清除 PKCE 数据
    sessionStorage.removeItem('pkce_code_verifier')
    sessionStorage.removeItem('pkce_state')
    sessionStorage.removeItem('pkce_timestamp')
    
    // 跳转回登录页
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  }
}
</script>

<style scoped lang="scss">
.callback-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f0f2f5;
  
  .el-card {
    width: 400px;
  }
  
  .callback-content {
    text-align: center;
    padding: 40px 20px;
    
    .loading-icon {
      animation: spin 1s linear infinite;
    }
    
    p {
      margin-top: 20px;
      color: #666;
    }
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

