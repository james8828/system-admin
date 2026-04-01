package com.jnet.auth.service;

import com.jnet.system.api.client.SysUserFeignClient;
import com.jnet.system.api.dto.SysUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情服务实现
 * 通过 Feign Client 从 system-admin 服务获取用户信息
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserFeignClient sysUserFeignClient;

    public CustomUserDetailsService(SysUserFeignClient sysUserFeignClient) {
        this.sysUserFeignClient = sysUserFeignClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        try {
            // 通过 Feign Client 查询用户信息
            var result = sysUserFeignClient.getUserByUsername(username);
            
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.warn("User not found: {}", username);
                throw new UsernameNotFoundException("User not found: " + username);
            }
            
            SysUserDTO userDTO = result.getData();
            
            // 检查用户是否启用
            if (userDTO.getEnabled() != null && !userDTO.getEnabled()) {
                log.warn("User is disabled: {}", username);
                throw new UsernameNotFoundException("User is disabled: " + username);
            }
            
            // 构建 Spring Security UserDetails
            return User.withUsername(userDTO.getUserName())
                    .password(userDTO.getPassword())
                    .roles("USER") // 基础角色，实际角色应该从 roleIds 转换
                    .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
                    .disabled(!userDTO.getEnabled())
                    .accountExpired(false)
                    .credentialsExpired(false)
                    .accountLocked(false)
                    .build();
                    
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user by username: {}", username, e);
            throw new UsernameNotFoundException("Failed to load user: " + username, e);
        }
    }
}
