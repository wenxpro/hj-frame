package com.wenx.permission.plugin;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.wenx.v3secure.utils.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 租户隔离拦截器（P2：共享库 + tenant_id 字段逻辑隔离）
 *
 * <p>对租户隔离表（白名单）的 SELECT 自动注入 {@code tenant_id = X} 条件：
 * <ul>
 *   <li>超管：跳过（全量视角，与数据权限超管放行一致）</li>
 *   <li>平台用户/未登录：跳过（平台请求走平台数据源，无租户上下文）</li>
 *   <li>普通租户用户：按 {@link LoginUser#getTenantId()} 强制隔离</li>
 * </ul>
 *
 * <p>INSERT 侧由 {@code MybatisPlusMetaObjectHandler} 自动填充 tenant_id。
 * 注：动态数据源按租户路由为预留能力（P1.4 决策：默认共享库隔离）。
 *
 * @author wenx
 */
@Slf4j
public class TenantInnerInterceptor implements InnerInterceptor {

    /** 租户隔离表白名单（不在白名单的表不注入，如全局表/平台表/关联展示表） */
    private static final Set<String> TENANT_TABLES = Set.of(
            "sys_user", "sys_role", "sys_department", "sys_team", "task",
            "sys_user_role", "sys_user_team"
    );

    private static final Pattern TABLE_PATTERN = Pattern.compile("(?is)\\b(\\w+)\\b");

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 忽略标记（与数据权限拦截器一致）
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
            return;
        }
        // 只处理 SELECT
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            return;
        }
        // 超管跳过（全量视角）
        if (LoginUser.isSuperAdmin()) {
            return;
        }
        // 未登录 / 平台用户（无租户上下文）跳过
        Long tenantId = LoginUser.getTenantId();
        if (tenantId == null) {
            return;
        }

        PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);
        String originalSql = mpBoundSql.sql();
        String tenantTable = findTenantTable(originalSql);
        if (tenantTable == null) {
            return;
        }

        try {
            String newSql = addTenantCondition(originalSql, tenantId);
            if (!newSql.equals(originalSql)) {
                mpBoundSql.sql(newSql);
                log.debug("租户条件注入完成: table={}, tenantId={}", tenantTable, tenantId);
            }
        } catch (Exception e) {
            log.warn("租户条件注入失败: {}", e.getMessage());
        }
    }

    /**
     * 查找 SQL 中命中的第一个租户隔离表（词边界匹配，避免 sys_user 误命中 sys_user_role 等）
     */
    private String findTenantTable(String sql) {
        for (String table : TENANT_TABLES) {
            if (Pattern.compile("(?is)\\b" + Pattern.quote(table) + "\\b").matcher(sql).find()) {
                return table;
            }
        }
        return null;
    }

    /**
     * 在 SQL 中注入 tenant_id 条件（与 DataPermissionInnerInterceptor 同模式）
     */
    private String addTenantCondition(String sql, Long tenantId) {
        String condition = "tenant_id = " + tenantId;
        String upperSql = sql.toUpperCase();
        if (upperSql.contains(" WHERE ")) {
            return sql.replaceFirst("(?i)\\s+WHERE\\s+", " WHERE (" + condition + ") AND ");
        }
        if (upperSql.contains(" ORDER BY ")) {
            return sql.replaceFirst("(?i)\\s+ORDER\\s+BY", " WHERE " + condition + " ORDER BY");
        }
        if (upperSql.contains(" GROUP BY ")) {
            return sql.replaceFirst("(?i)\\s+GROUP\\s+BY", " WHERE " + condition + " GROUP BY");
        }
        if (upperSql.contains(" LIMIT ")) {
            return sql.replaceFirst("(?i)\\s+LIMIT", " WHERE " + condition + " LIMIT");
        }
        return sql.trim() + " WHERE " + condition;
    }
}
