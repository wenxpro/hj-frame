package com.wenx.v3secure.audit;

import lombok.Builder;
import lombok.Data;

/**
 * 审计日志条目（D3）
 * 切面捕获的审计数据载体，由 {@code AuditLogSink} 实现方持久化。
 *
 * @author wenx
 */
@Data
@Builder
public class AuditLogEntry {

    private Long operatorId;
    private String operatorName;
    /** 所属租户（P0-2 review 修复：主线程构建时捕获，sink 异步落库用） */
    private Long tenantId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String requestUri;
    private String requestMethod;
    private String requestParams;
    private Integer responseStatus;
    private String ipAddress;
    private String userAgent;
    private Long executeTime;
    private Boolean success;

    /** 链路追踪ID（D7：主线程从 MDC 捕获，随审计落库供 Jaeger 回溯） */
    private String traceId;
}
