package com.jnet.auth.service;

import com.jnet.auth.details.CustomUserDetails;
import com.jnet.system.api.client.SysUserFeignClient;
import com.jnet.system.api.dto.SysUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义用户详情服务实现
 * 
 * <p>核心功能：通过 Feign Client 远程调用 system-admin 服务加载用户信息</p>
 * 
 * <p>主要职责：</p>
 * <ul>
 *     <li>实现 Spring Security UserDetailsService 接口</li>
 *     <li>通过用户名查询用户详细信息</li>
 *     <li>检查用户是否启用</li>
 *     <li>返回包含完整用户信息的 CustomUserDetails 对象</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserFeignClient sysUserFeignClient;

    public CustomUserDetailsService(SysUserFeignClient sysUserFeignClient) {
        this.sysUserFeignClient = sysUserFeignClient;
    }

    /**
     * 加载用户详细信息
     * 
     * <p>通过用户名查询用户信息，构建 CustomUserDetails 对象</p>
     * <p>检查用户是否启用，未启用的用户将抛出异常</p>
     * 
     * @param username 用户名（登录账号）
     * @return UserDetails 用户详细信息
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("正在通过用户名加载用户：{}", username);
        
        try {
            // 通过 Feign Client 查询用户信息
            var result = sysUserFeignClient.getUserByUsername(username);
            
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.warn("未找到用户：{}", username);
                throw new UsernameNotFoundException("未找到用户：" + username);
            }
            
            SysUserDTO userDTO = result.getData();
            
            // 检查用户是否启用
            if (userDTO.getEnabled() != null && !userDTO.getEnabled()) {
                log.warn("用户已禁用：{}", username);
                throw new UsernameNotFoundException("用户已禁用：" + username);
            }
            
            // 返回自定义的 UserDetails，包含完整的用户信息
            return new CustomUserDetails(userDTO);
                    
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("加载用户信息失败，用户名：{}", username, e);
            throw new UsernameNotFoundException("加载用户信息失败：" + username, e);
        }
    }
}
