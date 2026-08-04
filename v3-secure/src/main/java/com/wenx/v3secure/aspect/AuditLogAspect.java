package com.wenx.v3secure.aspect;

import com.wenx.v3secure.annotation.AuditLog;
import com.wenx.v3secure.audit.AuditLogEntry;
import com.wenx.v3secure.audit.AuditLogSink;
import com.wenx.v3secure.user.UserDetail;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作审计切面（D3）
 * 拦截 {@code @AuditLog} 标注方法，捕获操作者/请求上下文/结果，经 {@code AuditLogSink} 写入审计表。
 * 审计记录不影响主流程：Sink 实现方负责异步/独立事务；无 Sink 实现时静默跳过。
 *
 * @author wenx
 */
@Slf4j
@Aspect
@Component
@Order(2)
public class AuditLogAspect {

    private final ObjectProvider<AuditLogSink> auditLogSinkProvider;

    public AuditLogAspect(ObjectProvider<AuditLogSink> auditLogSinkProvider) {
        this.auditLogSinkProvider = auditLogSinkProvider;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable throwable = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            record(joinPoint, auditLog, result, throwable, System.currentTimeMillis() - start);
        }
    }

    private void record(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                        Object result, Throwable throwable, long executeTime) {
        try {
            AuditLogSink sink = auditLogSinkProvider.getIfAvailable();
            if (sink == null) {
                return;
            }
            sink.record(buildEntry(joinPoint, auditLog, result, throwable, executeTime));
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        }
    }

    private AuditLogEntry buildEntry(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                                     Object result, Throwable throwable, long executeTime) {
        UserDetail operator = resolveOperator();

        AuditLogEntry.AuditLogEntryBuilder builder = AuditLogEntry.builder()
                .operatorId(operator != null ? operator.getId() : null)
                .operatorName(operator != null ? operator.getUsername() : null)
                .action(auditLog.action())
                .resourceType(auditLog.resourceType())
                .executeTime(executeTime)
                .success(throwable == null)
                // D7：主线程捕获链路 ID（Micrometer 写入 MDC 的 key 为 traceId）
                .traceId(org.slf4j.MDC.get("traceId"));

        // 请求上下文（存在 Web 上下文时）
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            var request = attributes.getRequest();
            builder.requestUri(request.getRequestURI())
                    .requestMethod(request.getMethod())
                    .ipAddress(extractClientIp(request))
                    .userAgent(request.getHeader("User-Agent"));
        }

        // 参数（脱敏 password，避免审计泄漏凭据）
        builder.requestParams(sanitizeParams(joinPoint));
        return builder.build();
    }

    private UserDetail resolveOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof UserDetail userDetail) {
            return userDetail;
        }
        return null;
    }

    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String sanitizeParams(ProceedingJoinPoint joinPoint) {
        try {
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                String name = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                Object arg = args[i];
                sb.append("\"").append(name).append("\":");
                if (arg == null) {
                    sb.append("null");
                } else if (isSensitive(name)) {
                    sb.append("\"***\"");
                } else if (isPrimitiveLike(arg)) {
                    // 基础类型输出值
                    sb.append("\"").append(String.valueOf(arg).replace("\"", "\\\"")).append("\"");
                } else {
                    // 对象（DTO 等）只输出类名，避免 toString 泄漏敏感字段（如密码）
                    sb.append("\"").append(arg.getClass().getSimpleName()).append("\"");
                }
            }
            sb.append("}");
            String json = sb.toString();
            return json.length() > 1000 ? json.substring(0, 1000) : json;
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean isPrimitiveLike(Object arg) {
        return arg instanceof String || arg instanceof Number || arg instanceof Boolean
                || arg instanceof Character;
    }

    private boolean isSensitive(String name) {
        String lower = name.toLowerCase();
        return lower.contains("password") || lower.contains("secret") || lower.contains("token")
                || lower.contains("credential");
    }
}
