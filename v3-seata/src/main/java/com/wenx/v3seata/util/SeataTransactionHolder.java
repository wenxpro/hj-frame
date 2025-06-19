package com.wenx.v3seata.util;

import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Seata 事务信息持有者
 * 使用 ThreadLocal 存储当前线程的事务信息
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Component
public class SeataTransactionHolder {
    
    private static final ThreadLocal<TransactionInfo> TRANSACTION_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置事务 ID
     */
    public void setXid(@NonNull String xid) {
        var info = getOrCreateInfo();
        info.xid = xid;
    }
    
    /**
     * 获取事务 ID
     */
    @Nullable
    public String getXid() {
        var info = TRANSACTION_HOLDER.get();
        return info != null ? info.xid : null;
    }
    
    /**
     * 设置事务名称
     */
    public void setTransactionName(@NonNull String name) {
        var info = getOrCreateInfo();
        info.transactionName = name;
    }
    
    /**
     * 获取事务名称
     */
    @Nullable
    public String getTransactionName() {
        var info = TRANSACTION_HOLDER.get();
        return info != null ? info.transactionName : null;
    }
    
    /**
     * 检查是否存在事务信息
     */
    public boolean hasTransaction() {
        return TRANSACTION_HOLDER.get() != null;
    }
    
    /**
     * 获取事务信息的只读视图
     */
    @Nullable
    public TransactionSnapshot getSnapshot() {
        var info = TRANSACTION_HOLDER.get();
        return info != null ? new TransactionSnapshot(info.xid, info.transactionName) : null;
    }
    
    /**
     * 清除事务信息
     */
    public void clear() {
        TRANSACTION_HOLDER.remove();
    }
    
    /**
     * 获取或创建事务信息
     */
    private TransactionInfo getOrCreateInfo() {
        var info = TRANSACTION_HOLDER.get();
        if (info == null) {
            info = new TransactionInfo();
            TRANSACTION_HOLDER.set(info);
        }
        return info;
    }
    
    /**
     * 事务信息（内部使用）
     */
    private static class TransactionInfo {
        String xid;
        String transactionName;
    }
    
    /**
     * 事务信息快照（只读）
     */
    public record TransactionSnapshot(String xid, String transactionName) {}
} 