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
    private Long orgId;
    private Integer status;
    private Integer superAdmin;

    /**
     * 数据权限范围
     * <p>
     * null：表示全部数据权限
     */
    private List<Long> dataScopeList;
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
     */
    private Set<String> authoritySet;
    
    /**
     * 是否为平台用户
     * true: 平台用户，false: 系统用户
     */
    private boolean isPlatformUser = false;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
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