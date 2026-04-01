package com.jnet.auth.config;

import com.jnet.system.api.client.SysUserFeignClient;
import com.jnet.system.api.dto.SysUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.stream.Collectors;

/**
 * OAuth2 JWT Token 自定义器配置
 * 
 * <p>核心功能：在 JWT Access Token 中添加自定义 claims，使 Resource Server 能够直接从 Token 中获取用户信息</p>
 * 
 * <p>添加的自定义字段：</p>
 * <ul>
 *     <li>userId - 用户 ID（核心字段）</li>
 *     <li>username - 用户名</li>
 *     <li>nickName - 用户昵称</li>
 *     <li>deptId - 部门 ID</li>
 *     <li>roleIds - 角色 ID 列表</li>
 *     <li>email - 邮箱地址</li>
 *     <li>mobile - 手机号</li>
 *     <li>authorities - 权限列表</li>
 * </ul>
 * 
 * <p>工作流程：</p>
 * <ol>
 *     <li>用户认证成功，准备生成 JWT Token</li>
 *     <li>通过 Feign Client 调用 system 服务查询用户详细信息</li>
 *     <li>将用户信息添加到 JWT Token 的 claims 中</li>
 *     <li>Resource Server 解析 Token 时可直接获取这些信息</li>
 * </ol>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Configuration
public class OAuth2TokenCustomizerConfig {

    private final SysUserFeignClient sysUserFeignClient;

    public OAuth2TokenCustomizerConfig(SysUserFeignClient sysUserFeignClient) {
        this.sysUserFeignClient = sysUserFeignClient;
    }

    /**
     * JWT Token 自定义器 Bean
     * 
     * <p>在生成 Access Token 时，自动添加用户信息到 claims 中</p>
     * 
     * @return OAuth2TokenCustomizer 实例
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            // 只在生成 Access Token 时添加自定义 claims
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                Authentication authentication = context.getPrincipal();
                
                log.info("正在为户 [{}] 自定义 JWT Token", authentication.getName());
                
                // 获取用户名
                String username = authentication.getName();
                
                // 通过 Feign Client 查询用户信息
                try {
                    var result = sysUserFeignClient.getUserByUsername(username);
                    
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        SysUserDTO userDTO = result.getData();
                        
                        log.info("查询到用户信息：userId={}, nickName={}", 
                            userDTO.getUserId(), userDTO.getNickName());
                        
                        // 添加自定义 claims
                        context.getClaims().claims((claims) -> {
                            // 添加 userId（核心字段）
                            claims.put("userId", userDTO.getUserId());
                            // 添加昵称
                            claims.put("nickName", userDTO.getNickName());
                            // 添加部门 ID
                            claims.put("deptId", userDTO.getDeptId());
                            // 添加角色 ID 列表
                            if (userDTO.getRoleIds() != null && !userDTO.getRoleIds().isEmpty()) {
                                claims.put("roleIds", userDTO.getRoleIds());
                            }
                            // 添加邮箱
                            if (userDTO.getEmail() != null) {
                                claims.put("email", userDTO.getEmail());
                            }
                            // 添加手机号
                            if (userDTO.getMobile() != null) {
                                claims.put("mobile", userDTO.getMobile());
                            }
                            // 添加用户名
                            claims.put("username", username);
                        });
                    } else {
                        log.warn("未找到用户：{}", username);
                    }
                } catch (Exception e) {
                    log.error("获取用户信息失败，用户名：{}", username, e);
                }
                
                // 添加角色信息到 authorities claim
                context.getClaims().claims((claims) -> {
                    var authorities = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList());
                    if (!authorities.isEmpty()) {
                        claims.put("authorities", authorities);
                    }
                });
            }
        };
    }
}
