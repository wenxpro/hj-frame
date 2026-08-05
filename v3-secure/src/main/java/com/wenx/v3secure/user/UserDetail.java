package com.wenx.v3secure.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.wenx.v3secure.enums.PlatformRoleType;
import com.wenx.v3secure.enums.SystemRoleType;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户信息
 *
 * @author wenx
 * @description
 */
@Data
public class UserDetail implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String avatar;
    private Integer gender;
    private String email;
    private String mobile;
    private Long departmentId;
    /** 所属租户 ID（P2：多租户隔离，运行时经 sys_user 加载） */
    private Long tenantId;
    private Integer status;
    private Integer superAdmin;

    /** 用户所在团队 ID 集（H1：团队数据权限上下文，运行时经缓存服务加载） */
    private List<Long> teamIds;

    /**
     * 数据权限范围
     * <p>
     * null：表示全部数据权限
     */
    private List<Long> dataScopeList;

    /** 数据权限生效类型（ALL/DEPT/TENANT/BUSINESS/OWNER），由用户角色 data_scope 运行时计算 */
    private String dataScopeType;
    /**
     * 帐户是否过期
     */
    private boolean isAccountNonExpired = true;
    /**
     * 帐户是否被锁定
     */
    private boolean isAccountNonLocked = true;
    /**
     * 密码是否过期
     */
    private boolean isCredentialsNonExpired = true;
    /**
     * 帐户是否可用
     */
    private boolean isEnabled = true;
    /**
     * 拥有权限集合
     * 类型用 List（允许空 ArrayList）：授权对象序列化进 oauth2_authorization 表时，
     * Security Jackson allowlist 仅允许 ArrayList 等，Set.of() 的 ImmutableCollections$SetN 反序列化会失败
     */
    private List<String> authoritySet;
    
    /**
     * 是否为平台用户
     * true: 平台用户，false: 系统用户
     */
    private boolean isPlatformUser = false;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // review P1-6：裸 new UserDetail()（authoritySet 为 null）时避免 NPE
        if (authoritySet == null) {
            return java.util.Collections.emptyList();
        }
        return authoritySet.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isAccountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.isAccountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isCredentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }
    
    /**
     * 检查用户是否拥有指定的平台角色
     */
    public boolean hasPlatformRole(PlatformRoleType roleType) {
        if (authoritySet == null || roleType == null) {
            return false;
        }
        return authoritySet.contains("ROLE_" + roleType.getCode().toUpperCase());
    }
    
    /**
     * 检查用户是否拥有指定角色代码的平台角色
     */
    public boolean hasPlatformRole(String roleCode) {
        if (authoritySet == null || roleCode == null) {
            return false;
        }
        return authoritySet.contains("ROLE_" + roleCode.toUpperCase());
    }
    
    /**
     * 获取用户的平台角色类型
     */
    public PlatformRoleType getPlatformRoleType() {
        if (authoritySet == null) {
            return null;
        }
        
        for (PlatformRoleType roleType : PlatformRoleType.values()) {
            if (hasPlatformRole(roleType)) {
                return roleType;
            }
        }
        return null;
    }
    
    /**
     * 检查用户是否拥有指定的系统角色
     */
    public boolean hasSystemRole(SystemRoleType roleType) {
        if (authoritySet == null || roleType == null) {
            return false;
        }
        return authoritySet.contains("ROLE_" + roleType.getCode().toUpperCase());
    }
    
    /**
     * 检查用户是否拥有指定角色代码的系统角色
     */
    public boolean hasSystemRole(String roleCode) {
        if (authoritySet == null || roleCode == null) {
            return false;
        }
        return authoritySet.contains("ROLE_" + roleCode.toUpperCase());
    }
    
    /**
     * 获取用户的系统角色类型
     */
    public SystemRoleType getSystemRoleType() {
        if (authoritySet == null) {
            return null;
        }
        
        for (SystemRoleType roleType : SystemRoleType.values()) {
            if (hasSystemRole(roleType)) {
                return roleType;
            }
        }
        return null;
    }
    

}