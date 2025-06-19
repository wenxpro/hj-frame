package com.wenx.handler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wenx.v3secure.utils.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据权限处理器
 * 
 * @author wenx
 * @description 处理数据权限相关的查询条件
 */
@Slf4j
@Component
public class DataScopeHandler {
    
    /**
     * 应用数据权限到查询条件
     * 
     * @param wrapper 查询条件包装器
     * @param orgIdColumn 组织ID字段名
     * @param userIdColumn 用户ID字段名
     */
    public <T> void applyDataScope(QueryWrapper<T> wrapper, 
                                   String orgIdColumn, 
                                   String userIdColumn) {
        // 超级管理员拥有所有数据权限
        if (LoginUser.isSuperAdmin()) {
            log.debug("超级管理员，拥有所有数据权限");
            return;
        }
        
        // 获取用户数据权限范围
        List<Long> dataScopeList = LoginUser.getDataScopeList();
        
        if (dataScopeList == null || dataScopeList.isEmpty()) {
            // 如果没有数据权限配置，默认只能看自己的数据
            Long userId = LoginUser.getUserId();
            if (userId != null && userIdColumn != null) {
                wrapper.eq(userIdColumn, userId);
                log.debug("用户只能查看自己的数据，userId: {}", userId);
            }
        } else {
            // 根据数据权限范围过滤
            if (orgIdColumn != null) {
                wrapper.in(orgIdColumn, dataScopeList);
                log.debug("按组织权限过滤数据，orgIds: {}", dataScopeList);
            }
        }
    }
    
    /**
     * 检查用户是否有权限访问指定组织的数据
     * 
     * @param orgId 组织ID
     * @return true-有权限，false-无权限
     */
    public boolean hasOrgPermission(Long orgId) {
        if (orgId == null) {
            return true;
        }
        
        // 超级管理员拥有所有权限
        if (LoginUser.isSuperAdmin()) {
            return true;
        }
        
        // 检查用户的组织权限
        List<Long> dataScopeList = LoginUser.getDataScopeList();
        if (dataScopeList == null || dataScopeList.isEmpty()) {
            // 如果没有配置数据权限，检查是否是自己的组织
            return orgId.equals(LoginUser.getOrgId());
        }
        
        return dataScopeList.contains(orgId);
    }
    
    /**
     * 检查用户是否有权限访问指定用户的数据
     * 
     * @param targetUserId 目标用户ID
     * @return true-有权限，false-无权限
     */
    public boolean hasUserPermission(Long targetUserId) {
        if (targetUserId == null) {
            return true;
        }
        
        // 超级管理员拥有所有权限
        if (LoginUser.isSuperAdmin()) {
            return true;
        }
        
        // 自己的数据总是有权限
        Long currentUserId = LoginUser.getUserId();
        if (targetUserId.equals(currentUserId)) {
            return true;
        }
        
        // 如果有全部数据权限
        if (LoginUser.hasAllDataScope()) {
            return true;
        }
        
        // 其他情况暂时返回false，具体逻辑根据业务需求调整
        return false;
    }
    
    /**
     * 获取当前用户的数据权限SQL片段
     * 
     * @param orgIdColumn 组织ID字段名
     * @param userIdColumn 用户ID字段名
     * @return SQL片段
     */
    public String getDataScopeSql(String orgIdColumn, String userIdColumn) {
        // 超级管理员不需要数据权限过滤
        if (LoginUser.isSuperAdmin()) {
            return "";
        }
        
        List<Long> dataScopeList = LoginUser.getDataScopeList();
        
        if (dataScopeList == null || dataScopeList.isEmpty()) {
            // 只能看自己的数据
            Long userId = LoginUser.getUserId();
            if (userId != null && userIdColumn != null) {
                return String.format(" AND %s = %d", userIdColumn, userId);
            }
            return " AND 1=0"; // 没有任何权限
        } else {
            // 按组织权限过滤
            if (orgIdColumn != null && !dataScopeList.isEmpty()) {
                String orgIds = dataScopeList.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                return String.format(" AND %s IN (%s)", orgIdColumn, orgIds);
            }
            return "";
        }
    }
} 