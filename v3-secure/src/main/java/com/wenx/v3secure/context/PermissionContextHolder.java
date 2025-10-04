package com.wenx.v3secure.context;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限上下文持有器
 * 用于在当前线程中保存权限验证相关的上下文信息
 * 
 * @author wenx
 * @since 1.0.0
 */
@Slf4j
public class PermissionContextHolder {

    private static final ThreadLocal<PermissionContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置权限上下文
     * 
     * @param context 权限上下文
     */
    public static void setContext(PermissionContext context) {
        CONTEXT_HOLDER.set(context);
        log.debug("设置权限上下文: {}", context);
    }

    /**
     * 获取权限上下文
     * 
     * @return 权限上下文
     */
    public static PermissionContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 判断当前请求是否需要数据权限过滤
     * 
     * @return 是否需要过滤
     */
    public static boolean needsDataPermissionFilter() {
        PermissionContext context = getContext();
        return context != null && context.isRequiresPermissions();
    }

    /**
     * 获取当前请求需要的权限列表
     * 
     * @return 权限列表
     */
    public static List<String> getRequiredPermissions() {
        PermissionContext context = getContext();
        return context != null ? context.getPermissions() : new ArrayList<>();
    }

    /**
     * 清除权限上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
        log.debug("清除权限上下文");
    }

    /**
     * 权限上下文信息
     */
    @Data
    public static class PermissionContext {
        
        /**
         * 是否需要权限验证
         */
        private boolean requiresPermissions;

        /**
         * 需要的权限列表
         */
        private List<String> permissions;

        /**
         * 权限逻辑关系（AND/OR）
         */
        private String logical;

        /**
         * 请求的方法名
         */
        private String methodName;

        /**
         * 请求的类名
         */
        private String className;

        public PermissionContext(boolean requiresPermissions, List<String> permissions, String logical, String methodName, String className) {
            this.requiresPermissions = requiresPermissions;
            this.permissions = permissions != null ? new ArrayList<>(permissions) : new ArrayList<>();
            this.logical = logical;
            this.methodName = methodName;
            this.className = className;
        }

        @Override
        public String toString() {
            return String.format("PermissionContext{requiresPermissions=%s, permissions=%s, logical='%s', method='%s.%s'}", 
                    requiresPermissions, permissions, logical, className, methodName);
        }
    }
}