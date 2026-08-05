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
 * <p>对租户隔离表（白名单）的 SELECT / UPDATE / DELETE 自动注入 {@code tenant_id = X} 条件：
 * <ul>
 *   <li>超管：跳过（全量视角，与数据权限超管放行一致）</li>
 *   <li>平台用户/未登录：跳过（平台请求走平台数据源，无租户上下文）</li>
 *   <li>普通租户用户：按 {@link LoginUser#getTenantId()} 强制隔离（含写路径，防跨租户 IDOR 改删）</li>
 * </ul>
 *
 * <p>INSERT 侧由 {@code MybatisPlusMetaObjectHandler} 自动填充 tenant_id。
 * 注：动态数据源按租户路由为预留能力（P1.4 决策：默认共享库隔离）。
 *
 * <p>Z1.2：fail-closed——涉及白名单表时注入失败抛异常，不再静默放行（宁可失败不可泄露）。
 * Z1.3：beforeUpdate 覆盖 UPDATE/DELETE 写路径（此前仅 SELECT 被拦截）。
 * 已知限制：纯正则匹配（子查询/字符串字面量可能误匹配、JOIN 只注入一次），换 JSqlParser 语义解析列为 D9 触发项。
 *
 * @author wenx
 */
@Slf4j
public class TenantInnerInterceptor implements InnerInterceptor {

    /** 租户隔离表白名单（不在白名单的表不注入，如全局表/平台表/关联展示表） */
    private static final Set<String> TENANT_TABLES = Set.of(
            "sys_user", "sys_role", "sys_department", "sys_team", "task",
            "sys_user_role", "sys_user_team", "sys_operation_log"
    );

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 忽略标记（与数据权限拦截器一致）
        if (InterceptorIgnoreHelper.willIgnoreTenantLine(ms.getId())) {
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
        String newSql = buildTenantSql(originalSql, tenantId);
        if (!newSql.equals(originalSql)) {
            mpBoundSql.sql(newSql);
            log.debug("租户条件注入完成(SELECT): tenantId={}", tenantId);
        }
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        // 忽略标记（与数据权限拦截器一致）
        if (InterceptorIgnoreHelper.willIgnoreTenantLine(ms.getId())) {
            return;
        }
        // INSERT 由 MetaObjectHandler 填充，UPDATE/DELETE 注入租户条件（Z1.3 写路径隔离）
        SqlCommandType type = ms.getSqlCommandType();
        if (type != SqlCommandType.UPDATE && type != SqlCommandType.DELETE) {
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

        PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(ms.getBoundSql(parameter));
        String originalSql = mpBoundSql.sql();
        String newSql = buildTenantSql(originalSql, tenantId);
        if (!newSql.equals(originalSql)) {
            mpBoundSql.sql(newSql);
            log.debug("租户条件注入完成({}): tenantId={}", type, tenantId);
        }
    }

    /**
     * 对命中租户隔离表的 SQL 注入 {@code tenant_id = X} 条件。
     * 未命中白名单表 / WHERE 子句已含 tenant_id 条件时原样返回。
     * 命中白名单表但注入失败 → 抛异常（fail-closed，宁可失败不可泄露）。
     *
     * <p>注意：只在 WHERE 子句部分检测 tenant_id（Z-Review H2：SELECT 列清单
     * 如 {@code SELECT id,name,tenant_id,...} 也含该词，整句匹配会导致误判跳过、
     * 租户隔离整体失效——本次修复后隔离恢复生效）。
     *
     * <p>sys_role 特殊处理（Z-Review H1）：种子角色（is_builtin=1）为全局共享模板
     * （tenant_id=1），租户 X 会话内必须可见（角色分配/权限链路依赖），注入
     * {@code (tenant_id = X OR is_builtin = 1)}；租户自定义角色按租户隔离。
     *
     * @param sql      原始 SQL
     * @param tenantId 租户 ID
     * @return 注入后的 SQL
     */
    public static String buildTenantSql(String sql, Long tenantId) {
        String tenantTable = findTenantTable(sql);
        if (tenantTable == null) {
            return sql;
        }
        if (whereClauseHasTenantColumn(sql)) {
            log.debug("WHERE 子句已含 tenant_id 条件，跳过注入: table={}", tenantTable);
            return sql;
        }
        try {
            return addTenantCondition(sql, tenantId, tenantTable);
        } catch (Exception e) {
            throw new IllegalStateException("租户条件注入失败: table=" + tenantTable + ", " + e.getMessage(), e);
        }
    }

    /**
     * 检测 SQL 的 WHERE 子句（含 UPDATE 的 WHERE）是否已含 tenant_id 条件。
     * 只匹配 {@code tenant_id} 后紧跟运算符/括号的写法（如 {@code tenant_id = 2}、
     * {@code tenant_id IN (...)}），避开 SELECT 列清单（{@code id,name,tenant_id,code}）。
     */
    private static boolean whereClauseHasTenantColumn(String sql) {
        // 取最后一个 WHERE 之后的部分（UPDATE/DELETE/SELECT 的过滤条件区）
        int whereIndex = -1;
        int fromIndex = 0;
        String upperSql = sql.toUpperCase();
        while (true) {
            int idx = upperSql.indexOf("WHERE ", fromIndex);
            if (idx < 0) {
                break;
            }
            whereIndex = idx;
            fromIndex = idx + 6;
        }
        if (whereIndex < 0) {
            return false;
        }
        String wherePart = sql.substring(whereIndex + 5);
        // tenant_id 后跟 空白/运算符/左括号 → 视为条件引用（列清单后是逗号，不匹配）
        return Pattern.compile("(?is)\\btenant_id\\b\\s*(=|IN|IS|<>|!=|<=|>=|<|>|\\()").matcher(wherePart).find();
    }

    /**
     * 查找 SQL 中命中的第一个租户隔离表（词边界匹配，避免 sys_user 误命中 sys_user_role 等）
     */
    private static String findTenantTable(String sql) {
        for (String table : TENANT_TABLES) {
            if (Pattern.compile("(?is)\\b" + Pattern.quote(table) + "\\b").matcher(sql).find()) {
                return table;
            }
        }
        return null;
    }

    /**
     * 在 SQL 中注入 tenant_id 条件（与 DataPermissionInnerInterceptor 同模式）
     * Z11：与 whereClauseHasTenantColumn 一致定位最后一个 WHERE（外层过滤条件），
     * 此前检测取最后一个、注入 replaceFirst 第一个——子查询 SQL 会注入错位。
     */
    private static String addTenantCondition(String sql, Long tenantId, String table) {
        // H1：sys_role 的种子角色为全局共享模板，租户会话内需可见；租户自定义角色按租户隔离
        String condition = "sys_role".equals(table)
                ? "(tenant_id = " + tenantId + " OR is_builtin = 1)"
                : "tenant_id = " + tenantId;
        String upperSql = sql.toUpperCase();
        int lastWhere = lastIndexOfKeyword(upperSql, " WHERE ");
        if (lastWhere >= 0) {
            // 在最后一个 WHERE 关键字处插入（关键字替换为规范大写，与检测口径一致）
            return sql.substring(0, lastWhere) + " WHERE (" + condition + ") AND " + sql.substring(lastWhere + 7);
        }
        if (upperSql.contains(" ORDER BY ")) {
            return sql.replaceFirst("(?is)\\s+ORDER\\s+BY", " WHERE " + condition + " ORDER BY");
        }
        if (upperSql.contains(" GROUP BY ")) {
            return sql.replaceFirst("(?is)\\s+GROUP\\s+BY", " WHERE " + condition + " GROUP BY");
        }
        if (upperSql.contains(" LIMIT ")) {
            return sql.replaceFirst("(?is)\\s+LIMIT", " WHERE " + condition + " LIMIT");
        }
        return sql.trim() + " WHERE " + condition;
    }

    /**
     * 取关键字最后一次出现的位置（与 whereClauseHasTenantColumn 的扫描口径一致）
     */
    private static int lastIndexOfKeyword(String upperSql, String keyword) {
        int last = -1;
        int fromIndex = 0;
        while (true) {
            int idx = upperSql.indexOf(keyword, fromIndex);
            if (idx < 0) {
                break;
            }
            last = idx;
            fromIndex = idx + keyword.length();
        }
        return last;
    }
}
