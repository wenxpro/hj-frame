package com.wenx.v3secure.audit;

/**
 * 审计日志写入接口（D3）
 * 由业务模块实现（如 v3-system 写 sys_operation_log）；
 * 切面通过 ObjectProvider 注入，未实现方（如网关）则跳过审计。
 *
 * @author wenx
 */
public interface AuditLogSink {

    /**
     * 记录审计日志（实现方负责异步/独立事务，避免影响主流程）
     */
    void record(AuditLogEntry entry);
}
