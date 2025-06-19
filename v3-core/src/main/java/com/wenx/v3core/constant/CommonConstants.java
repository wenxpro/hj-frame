package com.wenx.v3core.constant;

/**
 * 通用常量
 * 
 * @author wenx
 */
public interface CommonConstants {
    
    /**
     * 成功标记
     */
    Integer SUCCESS = 200;
    
    /**
     * 失败标记
     */
    Integer FAIL = 500;
    
    /**
     * 登录用户
     */
    String LOGIN_USER = "login_user";
    
    /**
     * 用户ID
     */
    String USER_ID = "userId";
    
    /**
     * 用户名
     */
    String USERNAME = "username";
    
    /**
     * 租户ID
     */
    String TENANT_ID = "tenantId";
    
    /**
     * 请求头中的Token
     */
    String HEADER_TOKEN = "Authorization";
    
    /**
     * Token前缀
     */
    String TOKEN_PREFIX = "Bearer ";
    
    /**
     * 删除标记（0-正常，1-删除）
     */
    Integer DEL_FLAG_NORMAL = 0;
    Integer DEL_FLAG_DELETE = 1;
    
    /**
     * 是否（0-否，1-是）
     */
    Integer NO = 0;
    Integer YES = 1;
    
    /**
     * 状态（0-禁用，1-启用）
     */
    Integer STATUS_DISABLE = 0;
    Integer STATUS_ENABLE = 1;
    
    /**
     * 菜单类型（M目录 C菜单 F按钮）
     */
    String MENU_TYPE_DIR = "M";
    String MENU_TYPE_MENU = "C";
    String MENU_TYPE_BUTTON = "F";
    
    /**
     * 布局组件
     */
    String LAYOUT = "Layout";
    String PARENT_VIEW = "ParentView";
    
    /**
     * 缓存key前缀
     */
    String CACHE_PREFIX = "v3:cache:";
    
    /**
     * 系统默认密码
     */
    String DEFAULT_PASSWORD = "123456";
    
    /**
     * 超级管理员ID
     */
    Long SUPER_ADMIN_ID = 1L;
    
    /**
     * 树根节点ID
     */
    Long TREE_ROOT_ID = 0L;
    
    /**
     * 分页默认页码
     */
    Integer DEFAULT_PAGE_NUM = 1;
    
    /**
     * 分页默认大小
     */
    Integer DEFAULT_PAGE_SIZE = 10;
    
    /**
     * 分页最大大小
     */
    Integer MAX_PAGE_SIZE = 100;
} 