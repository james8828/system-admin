package com.jnet.common.trace;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 链路追踪 AOP 切面
 * 
 * <p>记录方法执行的链路日志，统一使用 [Trace] [Aspect] 前缀</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>拦截 Service 层和 Controller 层的方法执行</li>
 *     <li>记录方法开始执行的时间</li>
 *     <li>记录方法执行耗时</li>
 *     <li>记录方法执行异常信息</li>
 *     <li>自动获取或生成 TraceId</li>
 * </ul>
 * 
 * <h3>切点定义：</h3>
 * <ul>
 *     <li>Service 层方法：execution(* com..service..*(..))</li>
 *     <li>Controller 层方法：execution(* com..controller..*(..))</li>
 * </ul>
 * 
 * <h4>📝 日志格式：</h4>
 * <ul>
 *   <li>统一使用 [Trace] [Aspect] 前缀</li>
 *   <li>格式：[Trace] [Aspect] [{traceId}] {操作}</li>
 *   <li>便于日志收集和分析</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceAspect {

    /**
     * 定义切点：Service 层和 Controller 层方法
     */
    @Pointcut("execution(* com..service..*(..)) || execution(* com..controller..*(..))")
    public void tracePointcut() {
    }

    /**
     * 环绕通知：记录方法执行时间和 TraceId
     * 
     * <p>统一日志格式：[Trace] [Aspect] [{traceId}] {操作}</p>
     */
    @Around("tracePointcut()")
    public Object aroundTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = TraceContext.getOrGenerateTraceId();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            log.debug("[Trace] [Aspect] [{}] 开始执行 {}.{}", traceId, className, methodName);
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.debug("[Trace] [Aspect] [{}] 执行完成 {}.{}, 耗时 {}ms", traceId, className, methodName, (endTime - startTime));
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            log.error("[Trace] [Aspect] [{}] 执行异常 {}.{}, 耗时 {}ms - {}", 
                    traceId, className, methodName, (endTime - startTime), e.getMessage(), e);
            throw e;
        }
    }
}
