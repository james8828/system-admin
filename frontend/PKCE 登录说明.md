# OAuth2 PKCE 登录模式说明

## 📋 什么是 PKCE

PKCE（Proof Key for Code Exchange）是 OAuth2 的一种授权模式，主要用于公共客户端（如单页应用、移动端应用）。

### 与密码模式的区别

**密码模式（已废弃）**：
- ❌ 前端直接处理用户密码，存在安全风险
- ❌ 不符合 OAuth2 最佳实践
- ❌ 后端不再支持

**PKCE 模式（推荐）**：
- ✅ 前端不接触用户密码，由授权服务器处理
- ✅ 更安全的授权码流程
- ✅ 防止授权码拦截攻击
- ✅ 符合现代 OAuth2 标准

## 🔄 PKCE 授权流程

```
1. 用户在登录页输入用户名密码
   ↓
2. 前端生成 code_verifier 和 code_challenge
   ↓
3. 跳转到授权服务器 /oauth2/authorize
   ↓
4. 用户在授权服务器登录并授权
   ↓
5. 授权服务器重定向到 /callback?code=xxx&state=xxx
   ↓
6. 前端使用 code + code_verifier 换取 token
   ↓
7. 存储 token，完成登录
```

## 🔧 前端配置

### OAuth2 客户端配置

```typescript
const OAUTH_CONFIG = {
  clientId: 'jnet-pkce-client',
  redirectUri: window.location.origin + '/callback',
  scope: 'openid profile email', // 使用标准的 OIDC scopes
  authorizationEndpoint: '/oauth2/authorize',
  tokenEndpoint: '/oauth2/token'
}
```

### PKCE 关键代码

#### 1. 生成 code_verifier

```typescript
function generateCodeVerifier(): string {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return Array.from(array)
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}
```

#### 2. 计算 code_challenge

```typescript
async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(verifier)
  const digest = await crypto.subtle.digest('SHA-256', data)
  
  // Base64URL 编码
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')
}
```

#### 3. 发起授权请求

```typescript
const params = new URLSearchParams({
  response_type: 'code',
  client_id: OAUTH_CONFIG.clientId,
  redirect_uri: OAUTH_CONFIG.redirectUri,
  scope: OAUTH_CONFIG.scope,
  state: Math.random().toString(36).substring(2, 15),
  code_challenge: codeChallenge,
  code_challenge_method: 'S256'
})

window.location.href = `/oauth2/authorize?${params.toString()}`
```

#### 4. 回调处理

```typescript
// 在 Callback.vue 中
const code = route.query.code
const state = route.query.state

// 验证 state
if (state !== sessionStorage.getItem('pkce_state')) {
  throw new Error('Invalid state')
}

// 获取 code_verifier
const codeVerifier = sessionStorage.getItem('pkce_code_verifier')

// 使用授权码换取令牌（PKCE 模式不需要 client_secret）
const tokenRes = await authApi.getToken({
  grant_type: 'authorization_code',
  code: code,
  client_id: OAUTH_CONFIG.clientId,
  redirect_uri: OAUTH_CONFIG.redirectUri,
  code_verifier: codeVerifier
})
```

## 📁 相关文件

- `src/views/Login.vue` - 登录页面（发起 PKCE 流程）
- `src/views/Callback.vue` - 回调页面（处理授权码）
- `src/router/index.ts` - 路由配置（添加 callback 路由）
- `src/api/index.ts` - API 接口（getToken 方法）

## ⚠️ 注意事项

### 1. 回调地址配置

确保后端 OAuth2 客户端配置中包含回调地址：
```
http://localhost:5173/callback
```

### 2. State 参数验证

必须验证 state 参数，防止 CSRF 攻击：
```typescript
// 存储 state
sessionStorage.setItem('pkce_state', state)

// 验证 state
if (state !== sessionStorage.getItem('pkce_state')) {
  throw new Error('Invalid state')
}
```

### 3. Code Verifier 存储

code_verifier 存储在 sessionStorage 中，回调完成后需要清除：
```typescript
sessionStorage.removeItem('pkce_code_verifier')
sessionStorage.removeItem('pkce_state')
```

### 4. 安全性

- ✅ 使用 HTTPS（生产环境必需）
- ✅ 使用 crypto.getRandomValues() 生成随机数
- ✅ 使用 SHA-256 计算 challenge
- ✅ 验证 state 参数
- ✅ 及时清除临时数据

## 🎯 测试步骤

### 1. 启动前端

```bash
cd frontend
npm run dev
```

访问：http://localhost:5173/

### 2. 测试登录流程

1. 打开浏览器开发者工具（Network 标签）
2. 访问登录页面
3. 输入用户名和密码
4. 点击登录
5. 观察跳转过程：
   - `/login` → `/oauth2/authorize?...` → 授权页面 → `/callback?code=xxx&state=xxx` → `/`

### 3. 检查 Storage

在 Application → Session Storage 中查看：
- `pkce_code_verifier`
- `pkce_state`

登录后应该自动清除。

### 4. 检查 Token

在 Application → Local Storage 中查看：
- `access_token`
- `refresh_token`

## 🔍 调试技巧

### 如果登录失败

1. **检查控制台错误**
   ```javascript
   console.error('Callback handling failed:', error)
   ```

2. **检查 Network 请求**
   - 查看 `/oauth2/token` 请求的参数
   - 确认 grant_type=authorization_code
   - 确认包含 code_verifier

3. **检查回调地址**
   - 确认 redirect_uri 与后端配置一致
   - 确认包含 http://或 https://

4. **检查 State 验证**
   - 确认 sessionStorage 中的数据存在
   - 确认 state 参数匹配

## 📚 参考资料

- [OAuth 2.0 PKCE RFC 7636](https://tools.ietf.org/html/rfc7636)
- [OAuth 2.0 授权码模式最佳实践](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)
- [Spring Authorization Server 文档](https://docs.spring.io/spring-authorization-server/reference/)

---

**提示**：如果遇到跨域问题，请确保后端 CORS 配置允许前端地址访问。
