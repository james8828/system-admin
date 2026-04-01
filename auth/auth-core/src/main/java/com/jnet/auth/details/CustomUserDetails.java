package com.jnet.auth.details;

import com.jnet.system.api.dto.SysUserDTO;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 自定义用户详情实现
 * 
 * <p>扩展 Spring Security UserDetails，携带完整的用户信息</p>
 * <p>用于在认证流程中传递 userId、deptId、roleIds 等业务字段</p>
 * 
 * <p>核心字段：</p>
 * <ul>
 *     <li>userId - 用户 ID（主键）</li>
 *     <li>username - 用户名（登录账号）</li>
 *     <li>nickName - 用户昵称</li>
 *     <li>email - 邮箱地址</li>
 *     <li>mobile - 手机号</li>
 *     <li>deptId - 部门 ID</li>
 *     <li>roleIds - 角色 ID 列表</li>
 *     <li>authorities - 权限列表</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Getter
public class CustomUserDetails implements UserDetails {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /** 用户 ID（核心字段） */
    private final Long userId;
    
    /** 用户名（登录账号） */
    private final String username;
    
    /** 密码（加密存储） */
    private final String password;
    
    /** 用户昵称 */
    private final String nickName;
    
    /** 邮箱地址 */
    private final String email;
    
    /** 手机号 */
    private final String mobile;
    
    /** 部门 ID */
    private final Long deptId;
    
    /** 角色 ID 列表 */
    private final List<Long> roleIds;
    
    /** 权限列表（GrantedAuthority） */
    private final Collection<? extends GrantedAuthority> authorities;
    
    /** 账号是否过期 */
    private final boolean accountNonExpired;
    
    /** 凭证是否过期 */
    private final boolean credentialsNonExpired;
    
    /** 账号是否锁定 */
    private final boolean accountNonLocked;
    
    /** 账号是否启用 */
    private final boolean enabled;
    
    /**
     * 构造函数：从 SysUserDTO 构建自定义 UserDetails
     * 
     * <p>将系统用户 DTO 转换为 Spring Security UserDetails 格式</p>
     * <p>自动转换角色 ID 为 GrantedAuthority 格式</p>
     * 
     * @param userDTO 系统用户数据传输对象
     */
    public CustomUserDetails(SysUserDTO userDTO) {
        this.userId = userDTO.getUserId();
        this.username = userDTO.getUserName();
        this.password = userDTO.getPassword();
        this.nickName = userDTO.getNickName();
        this.email = userDTO.getEmail();
        this.mobile = userDTO.getMobile();
        this.deptId = userDTO.getDeptId();
        this.roleIds = userDTO.getRoleIds() != null ? userDTO.getRoleIds() : new ArrayList<>();
        
        // 构建权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (this.roleIds != null && !this.roleIds.isEmpty()) {
            // 将角色 ID 转换为权限标识（ROLE_前缀格式）
            for (Long roleId : this.roleIds) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleId));
            }
        }
        // 添加基础用户权限
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        this.authorities = authorities;
        
        this.accountNonExpired = true;
        this.credentialsNonExpired = true;
        this.accountNonLocked = true;
        this.enabled = userDTO.getEnabled() != null ? userDTO.getEnabled() : false;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }
    
    @Override
    public String getPassword() {
        return this.password;
    }
    
    @Override
    public String getUsername() {
        return this.username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
