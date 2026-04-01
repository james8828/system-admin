# PKCE 登录测试指南

## 🔍 问题诊断

### 错误现象
```
http://localhost:5173/oauth2/authorize?... 
[gateway] Completed 401 UNAUTHORIZED
```

### 根本原因
网关的 `AuthGlobalFilter` 拦截了 `/oauth2/authorize` 请求，因为：
1. ❌ 没有正确放行 OAuth2 相关路径
2. ❌ 可能缺少 Token 或 Token 无效

## ✅ 解决方案

### 方案 1：通过网关访问（推荐）

前端代码修改为使用相对路径：

```typescript
// Login.vue 中
window.location.href = `/oauth2/authorize?${params.toString()}`
```

**优点**：
- ✅ 统一通过网关管理
- ✅ CORS 问题自动解决
- ✅ 符合微服务架构

**配置说明**：

1. **Vite 代理配置** (`vite.config.ts`)：
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    },
    '/oauth2': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

2. **Gateway 路由配置** (`application.yml`)：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: oauth2
          uri: lb://jnet-auth
          predicates:
            - Path=/oauth2/**
          filters:
            - StripPrefix=0
```

3. **Gateway 过滤器放行** (`AuthGlobalFilter.java`)：
```java
if (path.startsWith("/api/auth") || path.startsWith("/oauth2") || 
    path.equals("/login") || path.equals("/logout")) {
    return chain.filter(exchange);
}
```

### 方案 2：直接访问 Auth 服务（不推荐）

```typescript
// 直接访问 auth 服务端口（如 8082）
window.location.href = `http://localhost:8082/oauth2/authorize?${params.toString()}`
```

**缺点**：
- ❌ 需要处理跨域问题
- ❌ 绕过网关，不符合架构规范
- ❌ 生产环境不可用

## 🧪 测试步骤

### 步骤 1：启动所有服务

```bash
# 1. 启动 Nacos（如果未启动）
# 2. 启动 Redis（如果未启动）

# 3. 启动网关
cd gateway/gateway-filter
mvn spring-boot:run

# 4. 启动认证服务
cd auth/auth-core
mvn spring-boot:run

# 5. 启动系统服务
cd system/system-admin
mvn spring-boot:run

# 6. 启动前端
cd frontend
npm run dev
```

### 步骤 2：清除浏览器缓存

打开浏览器控制台执行：
```javascript
sessionStorage.clear()
localStorage.clear()
```

刷新页面。

### 步骤 3：测试登录流程

1. **访问登录页**: http://localhost:5173/login

2. **点击登录按钮**（已预填充 admin/admin123）

3. **观察跳转过程**：
   ```
   /login 
   → /oauth2/authorize?response_type=code&client_id=jnet-pkce-client&scope=openid%20profile%20email&...
   → [网关接收请求]
   → [路由到 jnet-auth 服务]
   → [显示登录授权页面]
   ```

4. **在授权页面**：
   - 输入用户名：`admin`
   - 输入密码：`admin123`
   - 点击授权

5. **观察回调**：
   ```
   → /callback?code=xxx&state=xxx
   → [Callback.vue 处理]
   → POST /oauth2/token (换取 access_token)
   → [存储 token]
   → / (首页)
   ```

### 步骤 4：检查网络请求

打开浏览器 DevTools → Network 标签：

**应该看到以下请求**：

1. **授权请求**
   ```
   GET /oauth2/authorize?response_type=code&client_id=jnet-pkce-client&redirect_uri=...
   Status: 302 Found (或 200 OK 显示登录页)
   ```

2. **登录授权**
   ```
   POST /login (或其他登录端点)
   Status: 302 Found
   ```

3. **回调处理**
   ```
   GET /callback?code=xxx&state=xxx
   Status: 302 Found
   ```

4. **令牌换取**
   ```
   POST /oauth2/token
   Body: grant_type=authorization_code&code=xxx&client_id=jnet-pkce-client&redirect_uri=...&code_verifier=xxx
   Status: 200 OK
   Response: {"access_token":"xxx","refresh_token":"xxx",...}
   ```

## ⚠️ 常见问题

### 问题 1：401 UNAUTHORIZED

**原因**：网关拦截了 `/oauth2/authorize` 请求

**解决方法**：
```java
// AuthGlobalFilter.java 中确保放行
if (path.startsWith("/oauth2")) {
    return chain.filter(exchange);
}
```

### 问题 2：CORS 错误

**错误信息**：
```
Access to fetch at 'http://localhost:8080/oauth2/...' from origin 'http://localhost:5173' has been blocked by CORS policy
```

**解决方法**：

1. **Gateway CORS 配置**：
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
```

2. **Vite 代理配置**（已有）

### 问题 3：invalid_scope

**错误信息**：
```
error=invalid_scope&error_description=OAuth 2.0 Parameter: scope
```

**原因**：后端数据库中没有配置对应的 scope

**解决方法**：

在 `oauth2_registered_client` 表中添加或更新：

```sql
UPDATE oauth2_registered_client 
SET scopes = 'openid,profile,email' 
WHERE client_id = 'jnet-pkce-client';
```

或者插入新记录：

```sql
INSERT INTO oauth2_registered_client (
  id, client_id, client_secret,
  authorization_grant_types,
  scopes,
  redirect_uris
) VALUES (
  'pkce-client-id',
  'jnet-pkce-client',
  '{noop}jnet-secret',
  'authorization_code,refresh_token',
  'openid,profile,email',
  'http://localhost:5173/callback'
);
```

### 问题 4：重定向 URI 不匹配

**错误信息**：
```
error=redirect_uri_mismatch
```

**原因**：前端传递的 redirect_uri 与数据库中配置的不一致

**解决方法**：

确保数据库中配置的 redirect_uri 包含完整的协议和路径：

```sql
UPDATE oauth2_registered_client 
SET redirect_uris = 'http://localhost:5173/callback' 
WHERE client_id = 'jnet-pkce-client';
```

## 📊 成功的标志

✅ 能够顺利跳转到授权页面  
✅ 登录后能够成功回调  
✅ 能够获取到 access_token  
✅ 能够访问受保护的 API  

## 🔗 相关文件

- `frontend/src/views/Login.vue` - 登录页面
- `frontend/src/views/Callback.vue` - 回调页面
- `gateway/gateway-filter/src/main/java/com/jnet/gateway/filter/AuthGlobalFilter.java` - 网关认证过滤器
- `gateway/gateway-filter/src/main/resources/application.yml` - 网关配置
