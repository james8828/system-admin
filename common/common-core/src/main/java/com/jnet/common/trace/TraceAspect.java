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
 * <p>
 * 记录方法执行的链路日志，统一使用 [Trace] [Aspect] 前缀
 * </p>
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceAspect {

    /**
     * 定义切点：Service 层方法
     */
    @Pointcut("execution(* com..service..*(..)) || execution(* com..controller..*(..))")
    public void tracePointcut() {
    }

    /**
     * 环绕通知：记录方法执行时间和 TraceId
     * <p>
     * 统一日志格式：[Trace] [Aspect] [{traceId}] {操作}
     * </p>
     */
    @Around("tracePointcut()")
    public Object aroundTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        String traceId = TraceContext.getOrGenerateTraceId();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            log.debug("[Trace] [Aspect] [{}] Start executing {}.{}", traceId, className, methodName);
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.debug("[Trace] [Aspect] [{}] Completed executing {}.{} in {}ms", traceId, className, methodName, (endTime - startTime));
            return result;
        } catch (Throwable e) {
            long endTime = System.currentTimeMillis();
            log.error("[Trace] [Aspect] [{}] Error executing {}.{} after {}ms - {}", 
                    traceId, className, methodName, (endTime - startTime), e.getMessage(), e);
            throw e;
        }
    }
}
