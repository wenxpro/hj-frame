package com.wenx.permission.context;

import com.wenx.v3secure.utils.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据权限上下文管理器 - 重构版
 * 使用ThreadLocal管理当前线程的数据权限条件
 * 支持权限验证与SQL注入分离的架构设计
 * 
 * @author wenx
 * @since 1.0.0
 */
@Slf4j
public class DataPermissionContextHolder {
    
    /**
     * 权限条件信息存储 - 每个线程独立
     * Key: 表名, Value: 权限条件信息
     */
    private static final ThreadLocal<Map<String, PermissionConditionInfo>> PERMISSION_CONDITIONS = new ThreadLocal<>();
    
    /**
     * 权限验证状态 - 标记当前线程是否已完成权限验证
     */
    private static final ThreadLocal<Boolean> PERMISSION_VERIFIED = new ThreadLocal<>();
    
    /**
     * 预定义权限条件模板
     */
    private static final Map<String, String> CONDITION_TEMPLATES = new ConcurrentHashMap<>();
    
    /**
     * 占位符匹配模式
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    
    static {
        // 初始化预定义权限条件模板
        initConditionTemplates();
    }
    
    /**
     * 初始化预定义权限条件模板
     * 根据后端权限模型的实际字段名进行配置
     */
    private static void initConditionTemplates() {
        // 用户范围：只能查看自己创建的数据（修复字段名）
        CONDITION_TEMPLATES.put("USER_SCOPE", "create_by = #{userId}");
        
        // 部门范围：只能查看本部门的数据（已修复字段名）
        CONDITION_TEMPLATES.put("DEPT_SCOPE", "department_id = #{deptId}");
        
        // 部门及下级：可以查看本部门及下级部门的数据（已修复字段名）
        CONDITION_TEMPLATES.put("DEPT_AND_SUB", "department_id IN (#{deptIds})");
        
        // 组织范围：只能查看本组织的数据（根据实际业务需求调整）
        CONDITION_TEMPLATES.put("ORG_SCOPE", "org_id = #{orgId}");
        
        // 数据所有者：只能查看自己拥有的数据（修复字段名，与USER_SCOPE一致）
        CONDITION_TEMPLATES.put("OWNER_SCOPE", "create_by = #{userId}");
        
        // ========== 时间级权限模板 ==========
        
        // 当日数据权限
        CONDITION_TEMPLATES.put("TODAY_SCOPE", "DATE(create_time) = CURDATE()");
        
        // 本周数据权限
        CONDITION_TEMPLATES.put("WEEK_SCOPE", "YEARWEEK(create_time) = YEARWEEK(NOW())");
        
        // 本月数据权限
        CONDITION_TEMPLATES.put("MONTH_SCOPE", "DATE_FORMAT(create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')");
        
        // 本年数据权限
        CONDITION_TEMPLATES.put("YEAR_SCOPE", "YEAR(create_time) = YEAR(NOW())");
        
        // 近30天数据权限
        CONDITION_TEMPLATES.put("LAST_30_DAYS", "create_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)");
        
        // 近90天数据权限
        CONDITION_TEMPLATES.put("LAST_90_DAYS", "create_time >= DATE_SUB(NOW(), INTERVAL 90 DAY)");
        
        // 工作时间权限（9-18点，工作日）
        CONDITION_TEMPLATES.put("WORK_HOURS", "HOUR(NOW()) BETWEEN 9 AND 18 AND WEEKDAY(NOW()) < 5");
        
        // 可修改时间窗口（创建后24小时内可修改）
        CONDITION_TEMPLATES.put("EDITABLE_WINDOW", "create_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)");
    }
    
    /**
     * 设置表的权限条件信息
     * 
     * @param tableName 表名
     * @param conditionInfo 权限条件信息
     */
    public static void setConditionInfo(String tableName, PermissionConditionInfo conditionInfo) {
        if (!StringUtils.hasText(tableName) || conditionInfo == null) {
            return;
        }
        
        Map<String, PermissionConditionInfo> conditions = getConditionInfoMap();
        conditions.put(tableName.toLowerCase(), conditionInfo);
        
        log.debug("设置表 {} 的权限条件信息: {}", tableName, conditionInfo);
    }
    
    /**
     * 获取表的权限条件信息
     * 
     * @param tableName 表名
     * @return 权限条件信息
     */
    public static PermissionConditionInfo getConditionInfo(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return null;
        }
        
        Map<String, PermissionConditionInfo> conditions = getConditionInfoMap();
        return conditions.get(tableName.toLowerCase());
    }
    
    /**
     * 获取表的最终SQL条件（兼容旧接口）
     * 
     * @param tableName 表名
     * @return 权限条件SQL
     */
    public static String getCondition(String tableName) {
        PermissionConditionInfo conditionInfo = getConditionInfo(tableName);
        return conditionInfo != null ? conditionInfo.getFinalCondition() : null;
    }
    
    /**
     * 设置简单的权限条件（兼容旧接口）
     * 
     * @param tableName 表名
     * @param condition 权限条件SQL
     */
    public static void setCondition(String tableName, String condition) {
        if (!StringUtils.hasText(tableName) || !StringUtils.hasText(condition)) {
            return;
        }
        
        PermissionConditionInfo conditionInfo = PermissionConditionInfo.builder()
                .tableName(tableName)
                .finalCondition(condition)
                .status(PermissionConditionInfo.VerificationStatus.VERIFIED)
                .build();
                
        setConditionInfo(tableName, conditionInfo);
    }
    
    /**
     * 获取所有权限条件信息
     * 
     * @return 所有权限条件信息
     */
    public static Map<String, PermissionConditionInfo> getAllConditionInfos() {
        return getConditionInfoMap();
    }
    
    /**
     * 获取所有权限条件（兼容旧接口）
     * 
     * @return 所有权限条件
     */
    public static Map<String, String> getAllConditions() {
        Map<String, PermissionConditionInfo> conditionInfos = getConditionInfoMap();
        Map<String, String> conditions = new HashMap<>();
        
        conditionInfos.forEach((tableName, conditionInfo) -> {
            if (conditionInfo != null && StringUtils.hasText(conditionInfo.getFinalCondition())) {
                conditions.put(tableName, conditionInfo.getFinalCondition());
            }
        });
        
        return conditions;
    }
    
    /**
     * 权限验证状态管理
     */
    public static void setPermissionVerified(boolean verified) {
        PERMISSION_VERIFIED.set(verified);
    }
    
    public static boolean isPermissionVerified() {
        Boolean verified = PERMISSION_VERIFIED.get();
        return verified != null && verified;
    }
    
    /**
     * 获取当前线程的权限条件信息Map
     * 
     * @return 权限条件信息Map
     */
    private static Map<String, PermissionConditionInfo> getConditionInfoMap() {
        Map<String, PermissionConditionInfo> conditions = PERMISSION_CONDITIONS.get();
        if (conditions == null) {
            conditions = new HashMap<>();
            PERMISSION_CONDITIONS.set(conditions);
        }
        return conditions;
    }
    
    /**
     * 检查是否有权限条件
     * 
     * @return 是否有权限条件
     */
    public static boolean hasConditions() {
        Map<String, PermissionConditionInfo> conditionInfos = getConditionInfoMap();
        return conditionInfos.values().stream()
                .anyMatch(info -> info != null && StringUtils.hasText(info.getFinalCondition()));
    }
    
    /**
     * 清除当前线程的所有权限条件
     */
    public static void clear() {
        PERMISSION_CONDITIONS.remove();
        PERMISSION_VERIFIED.remove();
        log.debug("清除当前线程的权限条件");
    }
    
    /**
     * 获取权限条件模板
     * 
     * @param templateName 模板名称
     * @return 条件模板
     */
    public static String getConditionTemplate(String templateName) {
        return CONDITION_TEMPLATES.get(templateName);
    }
    
    /**
     * 构建权限条件
     * 将模板中的占位符替换为实际值
     * 安全策略：当无法获取有效权限条件时，使用拒绝访问条件防止全表扫描
     * 
     * @param template 条件模板
     * @return 构建后的条件
     */
    public static String buildCondition(String template) {
        if (!StringUtils.hasText(template)) {
            // 没有模板时，返回拒绝访问条件，防止全表扫描
            log.warn("没有权限条件模板，应用拒绝访问策略");
            return getDenyAccessCondition();
        }
        
        // 替换占位符
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        boolean hasInvalidPlaceholder = false;
        
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String value = getPlaceholderValue(placeholder);
            
            if (value != null) {
                // 确保SQL语法正确，数字类型不加引号，字符串类型加引号
                String replacement = formatSqlValue(value, placeholder);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // 安全策略：当无法获取占位符值时，应用拒绝访问条件
                log.warn("占位符 {} 无法获取值，应用拒绝访问策略防止全表扫描", placeholder);
                hasInvalidPlaceholder = true;
                break; // 退出循环，不继续处理
            }
        }
        
        // 如果有无效占位符，返回拒绝访问条件，防止数据泄露
        if (hasInvalidPlaceholder) {
            return getDenyAccessCondition();
        }
        
        matcher.appendTail(result);
        
        String condition = result.toString();
        log.debug("构建权限条件: {} -> {}", template, condition);
        
        return condition;
    }
    
    /**
     * 获取拒绝访问条件
     * 当无法构建有效权限条件时，使用此条件确保用户无法访问任何数据
     * 防止全表扫描和数据泄露的安全机制
     * 
     * @return 拒绝访问的SQL条件
     */
    private static String getDenyAccessCondition() {
        // 使用永远不成立的条件，确保不会返回任何数据
        return "1 = 0";
    }
    
    /**
     * 格式化SQL值，确保语法正确
     * 
     * @param value 原始值
     * @param placeholder 占位符名称
     * @return 格式化后的SQL值
     */
    private static String formatSqlValue(String value, String placeholder) {
        // 数字类型的占位符不需要引号
        if (isNumericPlaceholder(placeholder)) {
            return value;
        }
        
        // 如果是列表类型（如deptIds），不需要额外引号
        if (placeholder.endsWith("Ids") || placeholder.endsWith("ids")) {
            return value;
        }
        
        // 字符串类型需要单引号
        return "'" + value.replace("'", "''") + "'";
    }
    
    /**
     * 判断是否为数字类型的占位符
     */
    private static boolean isNumericPlaceholder(String placeholder) {
        return placeholder.equals("userId") || 
               placeholder.equals("deptId") ||
               placeholder.equals("departmentId");
    }
    
    /**
     * 获取占位符对应的值
     * 
     * @param placeholder 占位符名称
     * @return 占位符值
     */
    private static String getPlaceholderValue(String placeholder) {
        if (!LoginUser.isAuthenticated()) {
            return null;
        }
        
        switch (placeholder) {
            case "userId":
                Long userId = LoginUser.getUserId();
                return userId != null ? userId.toString() : null;
                
            case "username":
                return LoginUser.getUsername();
                
            case "deptId":
            case "departmentId":
                Long deptId = LoginUser.getDepartmentId();
                return deptId != null ? deptId.toString() : null;

            case "deptIds":
                // 获取用户所在部门及其子部门ID列表
                return getDeptAndSubDeptIds();

            default:
                log.warn("未知的占位符: {}", placeholder);
                return null;
        }
    }
    
    /**
     * 获取用户所在部门及其子部门ID列表
     * 
     * @return 部门ID列表字符串，格式如：1,2,3
     */
    private static String getDeptAndSubDeptIds() {
        Long deptId = LoginUser.getDepartmentId();
        if (deptId == null) {
            return null;
        }
        return deptId.toString();
    }
    
    /**
     * 添加自定义权限条件模板
     * 
     * @param templateName 模板名称
     * @param template 模板内容
     */
    public static void addConditionTemplate(String templateName, String template) {
        if (StringUtils.hasText(templateName) && StringUtils.hasText(template)) {
            CONDITION_TEMPLATES.put(templateName, template);
            log.debug("添加权限条件模板: {} -> {}", templateName, template);
        }
    }
    
    /**
     * 移除权限条件模板
     * 
     * @param templateName 模板名称
     */
    public static void removeConditionTemplate(String templateName) {
        if (StringUtils.hasText(templateName)) {
            CONDITION_TEMPLATES.remove(templateName);
            log.debug("移除权限条件模板: {}", templateName);
        }
    }
    
    /**
     * 获取所有权限条件模板
     * 
     * @return 所有模板
     */
    public static Map<String, String> getAllConditionTemplates() {
        return new HashMap<>(CONDITION_TEMPLATES);
    }
}