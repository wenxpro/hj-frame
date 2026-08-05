package com.wenx.permission.plugin;

import com.wenx.v3secure.user.UserDetail;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 租户隔离拦截器用例（批次 Z1：fail-closed + 写路径注入）
 * 覆盖：SELECT/UPDATE/DELETE 注入、超管/未登录跳过、非白名单表跳过、已含 tenant_id 跳过、注入失败抛错
 */
class TenantInnerInterceptorTest {

    private TenantInnerInterceptor interceptor;

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        interceptor = new TenantInnerInterceptor();
    }

    private void loginAs(Long tenantId, int superAdmin) {
        UserDetail user = new UserDetail();
        user.setId(100L);
        user.setUsername("tester");
        user.setSuperAdmin(superAdmin);
        user.setTenantId(tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private MappedStatement mockMs(SqlCommandType type, String sql) {
        MappedStatement ms = mock(MappedStatement.class);
        when(ms.getSqlCommandType()).thenReturn(type);
        when(ms.getId()).thenReturn("test.Mapper.select");
        BoundSql boundSql = new BoundSql(new Configuration(), sql, List.of(), null);
        when(ms.getBoundSql(null)).thenReturn(boundSql);
        when(ms.getBoundSql("arg")).thenReturn(boundSql);
        return ms;
    }

    // ============ buildTenantSql 纯函数 ============

    @Test
    void selectWithWhereInjected() {
        String sql = "SELECT * FROM sys_user WHERE id = 1";
        assertEquals("SELECT * FROM sys_user WHERE (tenant_id = 2) AND id = 1",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void selectWithoutWhereAppended() {
        String sql = "SELECT * FROM sys_user";
        assertEquals("SELECT * FROM sys_user WHERE tenant_id = 2",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void selectWithOrderByInjected() {
        String sql = "SELECT * FROM sys_user ORDER BY id DESC";
        assertEquals("SELECT * FROM sys_user WHERE tenant_id = 2 ORDER BY id DESC",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void updateInjected() {
        String sql = "UPDATE sys_user SET status = 0 WHERE id = 1";
        assertEquals("UPDATE sys_user SET status = 0 WHERE (tenant_id = 2) AND id = 1",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void deleteInjected() {
        String sql = "DELETE FROM sys_user_role WHERE user_id = 1";
        assertEquals("DELETE FROM sys_user_role WHERE (tenant_id = 2) AND user_id = 1",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void nonTenantTableUntouched() {
        String sql = "SELECT * FROM sys_menu WHERE id = 1";
        assertEquals(sql, TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void existingTenantColumnInSelectListStillInjected() {
        // H2 修复：SELECT 列清单含 tenant_id 列名 ≠ 已有条件，必须注入（此前整句匹配误判跳过 → 隔离失效）
        String sql = "SELECT id, name, tenant_id, code FROM sys_user WHERE deleted = 0";
        assertEquals("SELECT id, name, tenant_id, code FROM sys_user WHERE (tenant_id = 2) AND deleted = 0",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void existingTenantColumnInWhereSkipped() {
        // WHERE 子句已含 tenant_id 条件（业务自控），避免重复注入
        String sql = "SELECT * FROM sys_user WHERE tenant_id = 2 AND id = 1";
        assertEquals(sql, TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void updateExistingTenantColumnSkipped() {
        String sql = "UPDATE sys_user SET status = 0 WHERE tenant_id = 2 AND id = 1";
        assertEquals(sql, TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void sysRoleGetsGlobalRoleExemption() {
        // H1 修复：sys_role 注入 (tenant_id = X OR is_builtin = 1)——种子角色全局可见
        String sql = "SELECT * FROM sys_role WHERE deleted = 0";
        assertEquals("SELECT * FROM sys_role WHERE ((tenant_id = 2 OR is_builtin = 1)) AND deleted = 0",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void sysRoleUpdateWithTenantConditionInjected() {
        String sql = "UPDATE sys_role SET status = 0 WHERE id = 1";
        assertEquals("UPDATE sys_role SET status = 0 WHERE ((tenant_id = 2 OR is_builtin = 1)) AND id = 1",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    @Test
    void sysUserNotMatchSysUserRole() {
        // 词边界：sys_user 不误命中 sys_user_role
        String sql = "SELECT * FROM sys_user_role WHERE user_id = 1";
        assertEquals("SELECT * FROM sys_user_role WHERE (tenant_id = 2) AND user_id = 1",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }

    // ============ 拦截器上下文（SELECT） ============

    @Test
    void beforeQueryTenantUserInjected() throws Exception {
        loginAs(2L, 0);
        MappedStatement ms = mockMs(SqlCommandType.SELECT, "SELECT * FROM sys_user WHERE id = 1");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeQuery(mock(Executor.class), ms, null,
                mock(RowBounds.class), mock(ResultHandler.class), boundSql);

        assertEquals("SELECT * FROM sys_user WHERE (tenant_id = 2) AND id = 1", boundSql.getSql());
    }

    @Test
    void beforeQuerySuperAdminSkipped() throws Exception {
        loginAs(2L, 1);
        MappedStatement ms = mockMs(SqlCommandType.SELECT, "SELECT * FROM sys_user WHERE id = 1");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeQuery(mock(Executor.class), ms, null,
                mock(RowBounds.class), mock(ResultHandler.class), boundSql);

        assertEquals("SELECT * FROM sys_user WHERE id = 1", boundSql.getSql());
    }

    @Test
    void beforeQueryAnonymousSkipped() throws Exception {
        MappedStatement ms = mockMs(SqlCommandType.SELECT, "SELECT * FROM sys_user WHERE id = 1");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeQuery(mock(Executor.class), ms, null,
                mock(RowBounds.class), mock(ResultHandler.class), boundSql);

        assertEquals("SELECT * FROM sys_user WHERE id = 1", boundSql.getSql());
    }

    @Test
    void beforeQueryNonTenantTableSkipped() throws Exception {
        loginAs(2L, 0);
        MappedStatement ms = mockMs(SqlCommandType.SELECT, "SELECT * FROM sys_menu WHERE id = 1");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeQuery(mock(Executor.class), ms, null,
                mock(RowBounds.class), mock(ResultHandler.class), boundSql);

        assertEquals("SELECT * FROM sys_menu WHERE id = 1", boundSql.getSql());
    }

    // ============ 拦截器上下文（UPDATE/DELETE 写路径） ============

    @Test
    void beforeUpdateInjected() throws Exception {
        loginAs(3L, 0);
        MappedStatement ms = mockMs(SqlCommandType.UPDATE, "UPDATE task SET status = 1 WHERE id = 5");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeUpdate(mock(Executor.class), ms, null);

        assertEquals("UPDATE task SET status = 1 WHERE (tenant_id = 3) AND id = 5", boundSql.getSql());
    }

    @Test
    void beforeDeleteInjected() throws Exception {
        loginAs(3L, 0);
        MappedStatement ms = mockMs(SqlCommandType.DELETE, "DELETE FROM sys_user_role WHERE user_id = 5");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeUpdate(mock(Executor.class), ms, null);

        assertEquals("DELETE FROM sys_user_role WHERE (tenant_id = 3) AND user_id = 5", boundSql.getSql());
    }

    @Test
    void beforeUpdateInsertSkipped() throws Exception {
        loginAs(3L, 0);
        MappedStatement ms = mockMs(SqlCommandType.INSERT, "INSERT INTO sys_user (username) VALUES ('x')");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeUpdate(mock(Executor.class), ms, null);

        assertEquals("INSERT INTO sys_user (username) VALUES ('x')", boundSql.getSql());
    }

    @Test
    void beforeUpdateSuperAdminSkipped() throws Exception {
        loginAs(3L, 1);
        MappedStatement ms = mockMs(SqlCommandType.UPDATE, "UPDATE task SET status = 1 WHERE id = 5");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeUpdate(mock(Executor.class), ms, null);

        assertEquals("UPDATE task SET status = 1 WHERE id = 5", boundSql.getSql());
    }

    @Test
    void beforeUpdateNonTenantTableSkipped() throws Exception {
        loginAs(3L, 0);
        MappedStatement ms = mockMs(SqlCommandType.UPDATE, "UPDATE sys_menu SET name = 'x' WHERE id = 1");
        BoundSql boundSql = ms.getBoundSql(null);

        interceptor.beforeUpdate(mock(Executor.class), ms, null);

        assertEquals("UPDATE sys_menu SET name = 'x' WHERE id = 1", boundSql.getSql());
    }

    // ============ fail-closed ============

    @Test
    void buildTenantSqlAlwaysInjectsForTenantTable() {
        // fail-closed：命中白名单表必然注入成功（无 WHERE/ORDER BY/LIMIT 时末尾追加）
        String sql = "SELECT * FROM sys_user";
        String result = TenantInnerInterceptor.buildTenantSql(sql, 2L);
        assertEquals("SELECT * FROM sys_user WHERE tenant_id = 2", result);
    }

    @Test
    void buildTenantSqlWithCaseInsensitiveWhere() {
        // 大小写混用 WHERE 仍注入
        String sql = "SELECT * FROM sys_user where id = 1";
        assertEquals("SELECT * FROM sys_user WHERE (tenant_id = 2) AND id = 1",
                TenantInnerInterceptor.buildTenantSql(sql, 2L));
    }
}
