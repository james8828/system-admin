package com.jnet.system.api.client;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.system.api.dto.OAuth2AuthorizationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OAuth2 授权服务 Feign 客户端
 */
@FeignClient(name = "jnet-system-admin", contextId = "oauth2AuthorizationClient")
public interface OAuth2AuthorizationFeignClient {

    /**
     * 保存或更新 OAuth2 授权
     *
     * @param dto 授权数据传输对象
     * @return 保存结果
     */
    @PostMapping("/api/system/oauth2/authorization")
    Result<Void> saveAuthorization(@RequestBody OAuth2AuthorizationDTO dto);

    /**
     * 分页查询授权列表
     */
    @GetMapping("/api/system/oauth2/authorization/page")
    Result<PageResult<OAuth2AuthorizationDTO>> pageAuthorization(@SpringQueryMap PageQuery pageQuery, @SpringQueryMap OAuth2AuthorizationDTO authorization);

    /**
     * 根据 ID 查询授权详情
     */
    @GetMapping("/api/system/oauth2/authorization/{id}")
    Result<OAuth2AuthorizationDTO> getAuthorizationById(@PathVariable("id") String id);

    /**
     * 根据客户端 ID 查询授权列表
     */
    @GetMapping("/api/system/oauth2/authorization/client/{clientId}")
    Result<List<OAuth2AuthorizationDTO>> getAuthorizationsByClientId(@PathVariable("clientId") String clientId);

    /**
     * 根据用户标识查询授权列表
     */
    @GetMapping("/api/system/oauth2/authorization/principal/{principalName}")
    Result<List<OAuth2AuthorizationDTO>> getAuthorizationsByPrincipal(@PathVariable("principalName") String principalName);

    /**
     * 撤销授权
     */
    @DeleteMapping("/api/system/oauth2/authorization/{id}")
    Result<Void> revokeAuthorization(@PathVariable("id") String id);

    /**
     * 根据 Access Token 撤销授权
     */
    @DeleteMapping("/api/system/oauth2/authorization/by-access-token")
    Result<Void> revokeByAccessToken(@RequestParam("accessTokenValue") String accessTokenValue);

    /**
     * 根据 Refresh Token 撤销授权
     */
    @DeleteMapping("/api/system/oauth2/authorization/by-refresh-token")
    Result<Void> revokeByRefreshToken(@RequestParam("refreshTokenValue") String refreshTokenValue);

    /**
     * 根据 Token Value 和类型查询授权
     */
    @GetMapping("/api/system/oauth2/authorization/by-token")
    Result<OAuth2AuthorizationDTO> getAuthorizationByToken(
        @RequestParam("tokenValue") String tokenValue,
        @RequestParam("tokenType") String tokenType
    );

    /**
     * 清理过期的授权
     */
    @PostMapping("/api/system/oauth2/authorization/cleanup")
    Result<Integer> cleanupExpiredAuthorizations();
}
