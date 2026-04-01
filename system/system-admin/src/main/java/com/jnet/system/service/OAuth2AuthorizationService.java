package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.OAuth2Authorization;

import java.util.List;

/**
 * OAuth2 授权 Service 接口
 */
public interface OAuth2AuthorizationService extends IService<OAuth2Authorization> {

    /**
     * 分页查询授权列表
     */
    PageResult<OAuth2Authorization> pageAuthorization(PageQuery pageQuery, OAuth2Authorization authorization);

    /**
     * 根据 ID 查询授权
     */
    OAuth2Authorization getAuthorizationById(String id);

    /**
     * 根据客户端 ID 查询授权列表
     */
    List<OAuth2Authorization> getAuthorizationsByClientId(String clientId);

    /**
     * 根据用户标识查询授权列表
     */
    List<OAuth2Authorization> getAuthorizationsByPrincipal(String principalName);

    /**
     * 撤销授权
     */
    Boolean revokeAuthorization(String id);

    /**
     * 根据 Access Token 撤销授权
     */
    Boolean revokeByAccessToken(String accessTokenValue);

    /**
     * 根据 Refresh Token 撤销授权
     */
    Boolean revokeByRefreshToken(String refreshTokenValue);

    /**
     * 根据 Token Value 和类型查询授权
     */
    OAuth2Authorization getAuthorizationByToken(String tokenValue, String tokenType);

    /**
     * 清理过期的授权
     */
    Integer cleanupExpiredAuthorizations();
}
