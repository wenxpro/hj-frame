package com.wenx.permission.plugin;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.wenx.permission.context.DataPermissionContextHolder;
import com.wenx.permission.context.PermissionConditionInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * MyBatis-Plus数据权限内部拦截器 - 重构版
 * 支持权限验证与SQL注入分离的架构设计
 * 从上下文获取已验证的权限条件，进行延迟SQL注入
 * 
 * @author wenx
 * @since 1.0.0
 */
@Slf4j
public class DataPermissionInnerInterceptor implements InnerInterceptor {
    
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, 
                           RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        
        // 检查是否需要忽略数据权限
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
            log.debug("Mapper {} 被标记为忽略数据权限", ms.getId());
            return;
        }
        
        // 检查权限验证是否已完成
        if (!DataPermissionContextHolder.isPermissionVerified()) {
            log.debug("权限验证未完成，跳过SQL注入");
            return;
        }
        
        // 检查是否有权限条件
        if (!DataPermissionContextHolder.hasConditions()) {
            log.debug("无权限条件，跳过SQL注入");
            return;
        }
        
        // 只处理SELECT语句
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }
        
        PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
        String originalSql = mpBoundSql.sql();
        
        try {
            String newSql = addDataPermissionConditions(originalSql);
            if (!newSql.equals(originalSql)) {
                mpBoundSql.sql(newSql);
                log.debug("SQL权限条件注入完成，原SQL: {}\n新SQL: {}", originalSql, newSql);
            }
        } catch (Exception e) {
            log.warn("数据权限条件注入失败: {}", e.getMessage());
        }
    }
    
    /**
     * 向SQL添加数据权限条件
     * 重构版：优先使用已验证的条件信息
     */
    private String addDataPermissionConditions(String originalSql) {
        try {
            // 获取已验证的权限条件信息
            Map<String, PermissionConditionInfo> conditionInfos = DataPermissionContextHolder.getAllConditionInfos();
            if (conditionInfos.isEmpty()) {
                return originalSql;
            }
            
            String sql = originalSql;
            
            // 逐表处理权限条件
            for (Map.Entry<String, PermissionConditionInfo> entry : conditionInfos.entrySet()) {
                String tableName = entry.getKey();
                PermissionConditionInfo conditionInfo = entry.getValue();
                
                // 只处理验证通过的条件
                if (conditionInfo.getStatus() != PermissionConditionInfo.VerificationStatus.VERIFIED) {
                    continue;
                }
                
                String condition = conditionInfo.getFinalCondition();
                if (StringUtils.hasText(condition) && sql.toLowerCase().contains(tableName.toLowerCase())) {
                    sql = addWhereCondition(sql, condition);
                    log.debug("为表 {} 注入权限条件: {}", tableName, condition);
                }
            }
            
            return sql;
            
        } catch (Exception e) {
            log.warn("数据权限条件注入失败: {}", e.getMessage());
            return originalSql;
        }
    }
    
    /**
     * 在SQL中添加WHERE条件
     */
    private String addWhereCondition(String sql, String condition) {
        String upperSql = sql.toUpperCase();
        
        if (upperSql.contains(" WHERE ")) {
            // 已有WHERE子句，使用AND连接
            return sql.replaceFirst("(?i)\\s+WHERE\\s+", " WHERE (" + condition + ") AND ");
        } else {
            // 没有WHERE子句，添加WHERE
            // 找到合适的位置插入WHERE子句
            if (upperSql.contains(" ORDER BY ")) {
                return sql.replaceFirst("(?i)\\s+ORDER\\s+BY", " WHERE " + condition + " ORDER BY");
            } else if (upperSql.contains(" GROUP BY ")) {
                return sql.replaceFirst("(?i)\\s+GROUP\\s+BY", " WHERE " + condition + " GROUP BY");
            } else if (upperSql.contains(" HAVING ")) {
                return sql.replaceFirst("(?i)\\s+HAVING", " WHERE " + condition + " HAVING");
            } else if (upperSql.contains(" LIMIT ")) {
                return sql.replaceFirst("(?i)\\s+LIMIT", " WHERE " + condition + " LIMIT");
            } else {
                // 在SQL末尾添加WHERE子句
                return sql.trim() + " WHERE " + condition;
            }
        }
    }
}