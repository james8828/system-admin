package com.jnet.system.controller;

import com.jnet.common.result.Result;
import com.jnet.common.security.utils.SecurityContextUtils;
import com.jnet.system.api.dto.OAuth2AuthorizationDTO;
import com.jnet.system.entity.OAuth2Authorization;
import com.jnet.system.service.OAuth2AuthorizationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth2 授权控制器
 * 提供授权数据的 CRUD 接口供 auth 服务调用
 */
@Slf4j
@RestController
@RequestMapping("/api/system/oauth2/authorization")
public class OAuth2AuthorizationController {

    @Resource
    private OAuth2AuthorizationService oauth2AuthorizationService;

    /**
     * 保存或更新 OAuth2 授权
     *
     * @param dto 授权数据传输对象
     * @return 保存结果
     */
    @PostMapping
    public Result<Void> saveAuthorization(@RequestBody OAuth2AuthorizationDTO dto) {
        try {
            log.info("Saving OAuth2 authorization: clientId={}, principal={}",
                dto.getRegisteredClientId(), dto.getPrincipalName());

            // 检查是否已存在
            OAuth2Authorization existing = oauth2AuthorizationService.getById(dto.getId());

            if (existing != null) {
                // 更新
                updateAuthorization(existing, dto);
                oauth2AuthorizationService.updateById(existing);
                log.info("Updated existing authorization: {}", dto.getId());
            } else {
                // 新增
                OAuth2Authorization entity = new OAuth2Authorization();
                BeanUtils.copyProperties(dto, entity);
                oauth2AuthorizationService.save(entity);
                log.info("Created new authorization: {}", dto.getId());
            }

            return Result.success();

        } catch (Exception e) {
            log.error("Failed to save OAuth2 authorization", e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询授权详情
     */
    @GetMapping("/{id}")
    public Result<OAuth2AuthorizationDTO> getAuthorizationById(@PathVariable("id") String id) {
        try {
            OAuth2Authorization entity = oauth2AuthorizationService.getAuthorizationById(id);
            if (entity == null) {
                return Result.success(null);
            }

            OAuth2AuthorizationDTO dto = new OAuth2AuthorizationDTO();
            BeanUtils.copyProperties(entity, dto);

            return Result.success(dto);

        } catch (Exception e) {
            log.error("Failed to get authorization by ID", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据 Token 查询授权
     */
    @GetMapping("/by-token")
    public Result<OAuth2AuthorizationDTO> getAuthorizationByToken(
            @RequestParam("tokenValue") String tokenValue,
            @RequestParam("tokenType") String tokenType) {
        try {
            log.debug("Querying authorization by token: type={}", tokenType);

            OAuth2Authorization entity = oauth2AuthorizationService.getAuthorizationByToken(tokenValue, tokenType);
            if (entity == null) {
                return Result.success(null);
            }

            OAuth2AuthorizationDTO dto = new OAuth2AuthorizationDTO();
            BeanUtils.copyProperties(entity, dto);

            return Result.success(dto);

        } catch (Exception e) {
            log.error("Failed to get authorization by token", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 撤销授权
     */
    @DeleteMapping()
    public Result<Void> revokeAuthorization() {
        try {
            log.info("Revoking authorization name :{}",SecurityContextUtils.extractUsernameFromSecurityContext());
            String token = SecurityContextUtils.extractTokenFromSecurityContext();
            Boolean success = oauth2AuthorizationService.revokeAuthorization(token);
            return Boolean.TRUE.equals(success) ? Result.success() : Result.error("撤销失败");
        } catch (Exception e) {
            log.error("Failed to revoke authorization", e);
            return Result.error("撤销失败：" + e.getMessage());
        }
    }

    /**
     * 更新授权实体
     */
    private void updateAuthorization(OAuth2Authorization entity, OAuth2AuthorizationDTO dto) {
        // 只更新非空字段
        if (dto.getRegisteredClientId() != null) {
            entity.setRegisteredClientId(dto.getRegisteredClientId());
        }
        if (dto.getPrincipalName() != null) {
            entity.setPrincipalName(dto.getPrincipalName());
        }
        if (dto.getAuthorizationGrantType() != null) {
            entity.setAuthorizationGrantType(dto.getAuthorizationGrantType());
        }
        if (dto.getAuthorizedScopes() != null) {
            entity.setAuthorizedScopes(dto.getAuthorizedScopes());
        }
        if (dto.getAttributes() != null) {
            entity.setAttributes(dto.getAttributes());
        }
        if (dto.getState() != null) {
            entity.setState(dto.getState());
        }
        
        // 授权码信息
        if (dto.getAuthorizationCodeValue() != null) {
            entity.setAuthorizationCodeValue(dto.getAuthorizationCodeValue());
            entity.setAuthorizationCodeIssuedAt(dto.getAuthorizationCodeIssuedAt());
            entity.setAuthorizationCodeExpiresAt(dto.getAuthorizationCodeExpiresAt());
            entity.setAuthorizationCodeMetadata(dto.getAuthorizationCodeMetadata());
        }
        
        // Access Token 信息
        if (dto.getAccessTokenValue() != null) {
            entity.setAccessTokenValue(dto.getAccessTokenValue());
            entity.setAccessTokenIssuedAt(dto.getAccessTokenIssuedAt());
            entity.setAccessTokenExpiresAt(dto.getAccessTokenExpiresAt());
            entity.setAccessTokenMetadata(dto.getAccessTokenMetadata());
            entity.setAccessTokenType(dto.getAccessTokenType());
            entity.setAccessTokenScopes(dto.getAccessTokenScopes());
        }
        
        // OIDC ID Token 信息
        if (dto.getOidcIdTokenValue() != null) {
            entity.setOidcIdTokenValue(dto.getOidcIdTokenValue());
            entity.setOidcIdTokenIssuedAt(dto.getOidcIdTokenIssuedAt());
            entity.setOidcIdTokenExpiresAt(dto.getOidcIdTokenExpiresAt());
            entity.setOidcIdTokenMetadata(dto.getOidcIdTokenMetadata());
        }
        
        // Refresh Token 信息
        if (dto.getRefreshTokenValue() != null) {
            entity.setRefreshTokenValue(dto.getRefreshTokenValue());
            entity.setRefreshTokenIssuedAt(dto.getRefreshTokenIssuedAt());
            entity.setRefreshTokenExpiresAt(dto.getRefreshTokenExpiresAt());
            entity.setRefreshTokenMetadata(dto.getRefreshTokenMetadata());
        }
        
        // User Code 信息
        if (dto.getUserCodeValue() != null) {
            entity.setUserCodeValue(dto.getUserCodeValue());
            entity.setUserCodeIssuedAt(dto.getUserCodeIssuedAt());
            entity.setUserCodeExpiresAt(dto.getUserCodeExpiresAt());
            entity.setUserCodeMetadata(dto.getUserCodeMetadata());
        }
        
        // Device Code 信息
        if (dto.getDeviceCodeValue() != null) {
            entity.setDeviceCodeValue(dto.getDeviceCodeValue());
            entity.setDeviceCodeIssuedAt(dto.getDeviceCodeIssuedAt());
            entity.setDeviceCodeExpiresAt(dto.getDeviceCodeExpiresAt());
            entity.setDeviceCodeMetadata(dto.getDeviceCodeMetadata());
        }
    }
}
