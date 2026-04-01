# 微服务链路追踪使用指南

## 概述

已在 `common-core` 模块中添加了完整的链路追踪功能，所有微服务无需额外代码即可使用。

## 核心组件

### 1. TraceContext - 线程上下文
- **位置**: `com.jnet.common.trace.TraceContext`
- **功能**: 在线程中存储和获取 TraceId
- **方法**:
  - `generateTraceId()` - 生成新的 TraceId（16 位 UUID）
  - `setTraceId(String traceId)` - 设置 TraceId
  - `getTraceId()` - 获取当前线程的 TraceId
  - `getOrGenerateTraceId()` - 获取或生成 TraceId
  - `clear()` - 清理 ThreadLocal

### 2. TraceFilter - Servlet 过滤器
- **位置**: `com.jnet.common.trace.TraceFilter`
- **功能**: 
  - 为每个 HTTP 请求生成或提取 TraceId
  - 从请求头 `X-Trace-Id` 中读取上游服务的 TraceId
  - 响应完成后自动清理
- **优先级**: 最高（确保在其他过滤器之前执行）

### 3. TraceFeignInterceptor - Feign 拦截器
- **位置**: `com.jnet.common.trace.TraceFeignInterceptor`
- **功能**: 
  - 在 Feign 请求中传递 TraceId
  - 自动添加到请求头 `X-Trace-Id`
  - 如果没有 TraceId 则生成新的

### 4. TraceAspect - AOP 切面
- **位置**: `com.jnet.common.trace.TraceAspect`
- **功能**: 
  - 拦截 Service 和 Controller 层方法
  - 记录方法执行时间和 TraceId
  - 异常时记录详细错误信息
- **切点**: `execution(* com..service..*(..)) || execution(* com..controller..*(..))`

### 5. TraceAutoConfiguration - 自动配置
- **位置**: `com.jnet.common.trace.TraceAutoConfiguration`
- **功能**: Spring Boot 自动配置类
- **条件**: `jnet.trace.enabled=true`（默认启用）

## 配置说明

### application.yml 配置项

```yaml
jnet:
  trace:
    enabled: true              # 是否启用链路追踪（默认：true）
    headerName: X-Trace-Id     # TraceId 请求头名称（默认：X-Trace-Id）
    logMethodExecution: true   # 是否记录方法执行日志（默认：true）
    logLevel: DEBUG            # 日志级别：DEBUG, INFO（默认：DEBUG）
```

### 日志级别配置

```yaml
logging:
  level:
    com.jnet.common.trace: DEBUG
```

## 使用示例

### 1. Gateway 层面（已集成）

Gateway 会自动生成 TraceId 并传递给下游服务：

```java
// GlobalLoggingFilter.java 已实现
// 每个请求都会包含 [TraceId:xxx] 前缀的日志
log.info("[TraceId:{}] >>> 收到请求：{} {}", traceId, method, path);
```

### 2. Service 层方法（自动拦截）

无需任何代码修改，AOP 会自动拦截：

```java
@Service
public class UserServiceImpl implements UserService {
    
    // 自动记录执行日志（通过 AOP）
    // [TraceId:xxx] Start executing UserServiceImpl.getUserById
    // [TraceId:xxx] Completed executing UserServiceImpl.getUserById in 15ms
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
```

### 3. Feign 调用（自动传递）

Feign Client 会自动传递 TraceId：

```java
@FeignClient(name = "jnet-system")
public interface SysUserFeignClient {
    // 请求头会自动添加 X-Trace-Id
    @GetMapping("/api/system/user/{id}")
    Result<SysUserDTO> getUserById(@PathVariable("id") Long id);
}
```

### 4. 手动获取 TraceId

```java
import com.jnet.common.trace.TraceContext;

// 获取当前请求的 TraceId
String traceId = TraceContext.getTraceId();

// 获取或生成 TraceId
String traceId = TraceContext.getOrGenerateTraceId();

// 手动设置 TraceId（特殊场景）
TraceContext.setTraceId("custom-trace-id");
```

## 日志输出示例

### Gateway 日志
```
[TraceId:a1b2c3d4e5f6g7h8] >>> 收到请求：POST /oauth2/token | IP: 127.0.0.1
[TraceId:a1b2c3d4e5f6g7h8] <<< 请求完成：POST /oauth2/token | 状态：200 | 耗时：156ms
```

### Auth Service 日志
```
[TraceId:a1b2c3d4e5f6g7h8] Start executing OAuth2ServiceImpl.saveAuthorization
[TraceId:a1b2c3d4e5f6g7h8] Completed executing OAuth2ServiceImpl.saveAuthorization in 23ms
```

### System Admin Service 日志
```
[TraceId:a1b2c3d4e5f6g7h8] >>> Feign request [http://jnet-auth/oauth2/token] adding TraceId: a1b2c3d4e5f6g7h8
[TraceId:a1b2c3d4e5f6g7h8] Start executing UserServiceImpl.getUserById
[TraceId:a1b2c3d4e5f6g7h8] Completed executing UserServiceImpl.getUserById in 12ms
```

## 链路追踪流程

```
客户端请求
    ↓
Gateway (生成 TraceId: xxx)
    ↓ (请求头：X-Trace-Id: xxx)
Auth Service (提取 TraceId: xxx)
    ↓ (AOP 记录方法执行)
    ↓ (Feign 调用传递 TraceId)
System Admin Service (提取 TraceId: xxx)
    ↓ (AOP 记录方法执行)
    ↓
返回响应
```

## 优势

1. **无侵入性**: 公共包自动配置，业务代码无需修改
2. **全链路**: 从 Gateway 到最终 Service 完整追踪
3. **性能监控**: 记录每个方法的执行时间
4. **问题定位**: 通过 TraceId 快速定位跨服务问题
5. **日志关联**: 同一请求的所有日志都有相同 TraceId

## 注意事项

1. **ThreadLocal 清理**: Filter 和 Aspect 都会自动清理，防止内存泄漏
2. **异步场景**: 异步线程需要手动传递 TraceId
3. **性能影响**: 生产环境可考虑关闭方法执行日志（`logMethodExecution: false`）
4. **日志级别**: 建议使用 DEBUG 级别，避免生产环境日志过多

## 禁用链路追踪

```yaml
jnet:
  trace:
    enabled: false
```
