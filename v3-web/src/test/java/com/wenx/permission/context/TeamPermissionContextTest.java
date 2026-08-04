package com.wenx.permission.context;

import com.wenx.v3secure.user.UserDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 团队数据权限占位符求值用例（H2）
 * 覆盖：TEAM_SCOPE 模板、teamIds 占位符替换、无团队用户安全拒绝
 */
class TeamPermissionContextTest {

    private UserDetail buildUser(List<Long> teamIds) {
        UserDetail user = new UserDetail();
        user.setId(100L);
        user.setUsername("tester");
        user.setSuperAdmin(0);
        user.setDepartmentId(3L);
        user.setTeamIds(teamIds);
        return user;
    }

    private void loginAs(UserDetail user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teamScopeTemplateExists() {
        assertNotNull(DataPermissionContextHolder.getConditionTemplate("TEAM_SCOPE"));
        assertEquals("team_id IN (#{teamIds})",
                DataPermissionContextHolder.getConditionTemplate("TEAM_SCOPE"));
    }

    @Test
    void teamIdsPlaceholderReplaced() {
        loginAs(buildUser(List.of(1L, 2L, 7L)));
        String condition = DataPermissionContextHolder.buildCondition("team_id IN (#{teamIds})");
        assertEquals("team_id IN (1,2,7)", condition);
    }

    @Test
    void teamIdsEmptyRejected() {
        // 无团队用户：占位符无法求值 → 拒绝访问（防全表扫描）
        loginAs(buildUser(List.of()));
        String condition = DataPermissionContextHolder.buildCondition("team_id IN (#{teamIds})");
        assertEquals("1 = 0", condition);
    }

    @Test
    void mixedDeptAndTeam() {
        loginAs(buildUser(List.of(4L, 5L)));
        String condition = DataPermissionContextHolder.buildCondition(
                "(department_id IN (#{deptIds}) OR team_id IN (#{teamIds}))");
        assertEquals("(department_id IN (3) OR team_id IN (4,5))", condition);
    }
}
