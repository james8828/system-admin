<template>
  <div class="oauth-redirect-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>正在跳转至登录页面...</span>
        </div>
      </template>
      <div class="redirect-content">
        <el-icon class="loading-icon" :size="50"><Loading /></el-icon>
        <p>即将跳转到 OAuth2 授权页面</p>
        <p class="hint">请稍候...</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { generateCodeVerifier, generateCodeChallenge } from '@/utils/pkce'

const router = useRouter()

// OAuth2 PKCE 配置
const OAUTH_CONFIG = {
  clientId: 'jnet-pkce-client',
  redirectUri: window.location.origin + '/callback',
  scope: 'openid profile email',
  authorizationEndpoint: '/oauth2/authorize'
}

onMounted(async () => {
  console.log('=== OAuthRedirect 页面加载 ===')
  console.log('时间戳:', Date.now())
  
  try {
    // 1. 生成 code_verifier 和 code_challenge
    const codeVerifier = generateCodeVerifier()
    const codeChallenge = await generateCodeChallenge(codeVerifier)
    
    // 2. 生成随机 state
    const state = Math.random().toString(36).substring(2, 15) + Date.now().toString(36)
    
    // 3. 生成 authorization_id（用于 Redis 存储 OAuth2 参数）
    const authorizationId = 'auth_' + Math.random().toString(36).substring(2, 15) + Date.now().toString(36)
    
    // 4. 存储 PKCE 数据到 sessionStorage（包括时间戳）
    sessionStorage.setItem('pkce_code_verifier', codeVerifier)
    sessionStorage.setItem('pkce_state', state)
    sessionStorage.setItem('pkce_authorization_id', authorizationId)
    sessionStorage.setItem('pkce_timestamp', Date.now().toString())
    
    // 在浏览器控制台输出完整的运行参数
    console.log('\n=== PKCE 参数已生成 ===')
    console.log('code_verifier:', codeVerifier)
    console.log('code_challenge:', codeChallenge)
    console.log('state:', state)
    console.log('authorization_id:', authorizationId)
    console.log('redirect_uri:', OAUTH_CONFIG.redirectUri)
    console.log('client_id:', OAUTH_CONFIG.clientId)
    console.log('scope:', OAUTH_CONFIG.scope)
    console.log('=====================\n')

    // 等待 100ms 确保日志已经输出到控制台
    await new Promise(resolve => setTimeout(resolve, 100))
    
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
    
    console.log('跳转至授权端点:', `${OAUTH_CONFIG.authorizationEndpoint}?${params.toString()}`)
    
    // 6. 跳转到后端授权页面（后端会展示登录页）
    window.location.href = `${OAUTH_CONFIG.authorizationEndpoint}?${params.toString()}`
    
  } catch (error) {
    console.error('OAuth2 redirect error:', error)
    ElMessage.error('跳转失败，请重试')
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
})
</script>

<style scoped lang="scss">
.oauth-redirect-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  
  .el-card {
    width: 400px;
  }
  
  .redirect-content {
    text-align: center;
    padding: 40px 20px;
    
    .loading-icon {
      animation: spin 1s linear infinite;
    }
    
    p {
      margin-top: 20px;
      color: #666;
      
      &.hint {
        font-size: 12px;
        color: #999;
        margin-top: 10px;
      }
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
