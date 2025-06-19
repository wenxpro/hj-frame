package com.wenx.v3seata.aspect;

import com.wenx.v3seata.annotation.DistributedTransaction;
import com.wenx.v3seata.util.SeataTransactionHolder;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Seata 分布式事务切面
 * 处理 @DistributedTransaction 注解
 * 
 * @author v3-cloud
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class SeataTransactionalAspect {
    
    private final SeataTransactionHolder transactionHolder;
    
    /**
     * 处理 @DistributedTransaction 注解
     */
    @Around("@annotation(distributedTransaction)")
    public Object handleDistributedTransaction(ProceedingJoinPoint joinPoint, 
                                             DistributedTransaction distributedTransaction) throws Throwable {
        
        // 如果忽略全局事务，直接执行
        if (distributedTransaction.ignoreGlobalTransaction()) {
            return joinPoint.proceed();
        }
        
        // 获取方法信息
        var signature = (MethodSignature) joinPoint.getSignature();
        var method = signature.getMethod();
        var transactionName = getTransactionName(distributedTransaction, method);
        
        // 检查是否已经在事务中
        var xid = RootContext.getXID();
        if (xid != null) {
            log.debug("Already in global transaction, XID: {}", xid);
            return joinPoint.proceed();
        }
        
        // 开启全局事务
        var globalTransaction = GlobalTransactionContext.getCurrentOrCreate();
        
        try {
            // 开始事务
            globalTransaction.begin(distributedTransaction.timeoutMills(), transactionName);
            var newXid = RootContext.getXID();
            log.info("Start distributed transaction: {}, XID: {}", transactionName, newXid);
            
            // 保存事务信息
            transactionHolder.setXid(newXid);
            transactionHolder.setTransactionName(transactionName);
            
            // 执行业务方法
            var result = joinPoint.proceed();
            
            // 提交事务
            globalTransaction.commit();
            log.info("Commit distributed transaction: {}, XID: {}", transactionName, newXid);
            
            return result;
            
        } catch (Exception e) {
            // 判断是否需要回滚
            if (shouldRollback(e, distributedTransaction)) {
                rollbackTransaction(globalTransaction, transactionName, e);
            }
            throw e;
            
        } finally {
            // 清理事务信息
            transactionHolder.clear();
            RootContext.unbind();
        }
    }
    
    /**
     * 处理 @GlobalTransactional 注解的增强
     */
    @Around("@annotation(globalTransactional)")
    public Object enhanceGlobalTransactional(ProceedingJoinPoint joinPoint, 
                                           GlobalTransactional globalTransactional) throws Throwable {
        
        var xid = RootContext.getXID();
        var isTransactionInitiator = xid == null;
        
        if (xid != null) {
            // 已在事务中，记录信息
            log.debug("Execute GlobalTransactional method in transaction, XID: {}", xid);
            if (!transactionHolder.hasTransaction()) {
                transactionHolder.setXid(xid);
            }
        }
        
        try {
            return joinPoint.proceed();
        } finally {
            if (isTransactionInitiator) {
                // 如果是事务发起者，清理信息
                transactionHolder.clear();
            }
        }
    }
    
    /**
     * 获取事务名称
     */
    private String getTransactionName(DistributedTransaction annotation, java.lang.reflect.Method method) {
        return annotation.name().isBlank() 
            ? "%s.%s".formatted(method.getDeclaringClass().getSimpleName(), method.getName())
            : annotation.name();
    }
    
    /**
     * 判断是否需要回滚
     */
    private boolean shouldRollback(Exception e, DistributedTransaction annotation) {
        // 检查不回滚的异常
        var noRollbackMatch = Arrays.stream(annotation.noRollbackFor())
            .anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(e.getClass()));
        
        if (noRollbackMatch) {
            return false;
        }
        
        // 检查需要回滚的异常
        return Arrays.stream(annotation.rollbackFor())
            .anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(e.getClass()));
    }
    
    /**
     * 回滚事务
     */
    private void rollbackTransaction(GlobalTransaction transaction, String transactionName, Exception cause) {
        try {
            transaction.rollback();
            log.error("Rollback distributed transaction: {}, XID: {}, cause: {}", 
                    transactionName, RootContext.getXID(), cause.getMessage());
        } catch (Exception rollbackEx) {
            log.error("Failed to rollback distributed transaction: {}, XID: {}", 
                    transactionName, RootContext.getXID(), rollbackEx);
        }
    }
} 