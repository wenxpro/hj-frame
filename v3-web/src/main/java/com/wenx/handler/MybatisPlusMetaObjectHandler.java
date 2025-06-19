package com.wenx.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.wenx.v3secure.utils.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 公共字段填充
 *
 * @author wenx
 */
@Slf4j
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        
        try {
            Date now = new Date();
            
            // 填充创建时间
            if (metaObject.hasSetter("createTime")) {
                this.strictInsertFill(metaObject, "createTime", Date.class, now);
            }
            
            // 填充修改时间
            if (metaObject.hasSetter("modifyTime")) {
                this.strictInsertFill(metaObject, "modifyTime", Date.class, now);
            }
            
            // 版本号
            if (metaObject.hasSetter("version")) {
                this.strictInsertFill(metaObject, "version", Integer.class, 1);
            }
            
            // 删除标识
            if (metaObject.hasSetter("deleted")) {
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }
            
            // 创建者和修改者
            Long userId = getCurrentUserId();
            if (userId != null) {
                if (metaObject.hasSetter("createBy")) {
                    this.strictInsertFill(metaObject, "createBy", Long.class, userId);
                }
                if (metaObject.hasSetter("modifyBy")) {
                    this.strictInsertFill(metaObject, "modifyBy", Long.class, userId);
                }
            }
            
            // 如果是组织相关实体，填充组织ID
            if (metaObject.hasSetter("orgId")) {
                Long orgId = LoginUser.getOrgId();
                if (orgId != null) {
                    this.strictInsertFill(metaObject, "orgId", Long.class, orgId);
                }
            }
            
        } catch (Exception e) {
            log.warn("插入填充失败: {}", e.getMessage());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        
        try {
            // 填充修改时间
            if (metaObject.hasSetter("modifyTime")) {
                this.strictUpdateFill(metaObject, "modifyTime", Date.class, new Date());
            }
            
            // 修改者
            Long userId = getCurrentUserId();
            if (userId != null && metaObject.hasSetter("modifyBy")) {
                this.strictUpdateFill(metaObject, "modifyBy", Long.class, userId);
            }
            
            // 版本号自增（MyBatis Plus的乐观锁插件会自动处理）
            
        } catch (Exception e) {
            log.warn("更新填充失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取当前用户ID
     * 从安全上下文中获取当前登录用户的ID
     * 
     * @return 当前用户ID，如果无法获取则返回null
     */
    protected Long getCurrentUserId() {
        try {
            // 从LoginUser工具类获取当前用户ID
            Long userId = LoginUser.getUserId();
            if (userId != null) {
                log.debug("获取到当前用户ID: {}", userId);
                return userId;
            }
            
            // 如果是超级管理员，返回特定ID
            if (LoginUser.isSuperAdmin()) {
                log.debug("当前用户是超级管理员");
                return 1L; // 超级管理员默认ID
            }
            
            return null;
        } catch (Exception e) {
            log.debug("获取当前用户ID失败，可能是未登录状态: {}", e.getMessage());
            return null;
        }
    }
}