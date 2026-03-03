package com.tower.game.aspect;

import com.tower.game.common.annotation.NoLog;
import com.tower.game.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 统一打印 API 入参、出参日志（AOP 切 Controller 方法）。
 * 仅切 com.tower.game.api 包下的接口；带 @NoLog 的方法不打印。
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class ApiLogAspect {

    private static final int MAX_LOG_LENGTH = 2000;

    @Around("execution(* com.tower.game.api..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (signature.getMethod().isAnnotationPresent(NoLog.class)) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        String shortMethod = className + "#" + methodName;

        Object[] args = joinPoint.getArgs();
        String inLog = toLogString(args);
        log.info("API 入参 [{}] {}", shortMethod, truncate(inLog));

        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            log.warn("API 异常 [{}] {}", shortMethod, e.getMessage());
            throw e;
        }

        long cost = System.currentTimeMillis() - start;
        String outLog = toLogString(result);
        log.info("API 出参 [{}] 耗时 {}ms {}", shortMethod, cost, truncate(outLog));
        return result;
    }

    private static String toLogString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JsonUtil.toJsonString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private static String truncate(String s) {
        if (s == null || s.length() <= MAX_LOG_LENGTH) {
            return s;
        }
        return s.substring(0, MAX_LOG_LENGTH) + "...(截断)";
    }
}
