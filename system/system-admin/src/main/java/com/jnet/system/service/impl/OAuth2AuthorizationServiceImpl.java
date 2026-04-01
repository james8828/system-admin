package com.jnet.system.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.mapper.OAuth2AuthorizationMapper;
import com.jnet.system.entity.OAuth2Authorization;

import com.jnet.system.service.OAuth2AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OAuth2 授权 Service 实现类
 */
@Slf4j
@Service
public class OAuth2AuthorizationServiceImpl extends ServiceImpl<OAuth2AuthorizationMapper, OAuth2Authorization> implements OAuth2AuthorizationService {

    @Override
    public PageResult<OAuth2Authorization> pageAuthorization(PageQuery pageQuery, OAuth2Authorization authorization) {
        Page<OAuth2Authorization> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        
        LambdaQueryWrapper<OAuth2Authorization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(authorization.getRegisteredClientId()), 
                   OAuth2Authorization::getRegisteredClientId, authorization.getRegisteredClientId())
                .eq(StrUtil.isNotBlank(authorization.getPrincipalName()), 
                    OAuth2Authorization::getPrincipalName, authorization.getPrincipalName())
                .eq(StrUtil.isNotBlank(authorization.getAuthorizationGrantType()), 
                    OAuth2Authorization::getAuthorizationGrantType, authorization.getAuthorizationGrantType())
                .orderByDesc(OAuth2Authorization::getAccessTokenIssuedAt);
        
        Page<OAuth2Authorization> resultPage = baseMapper.selectPage(page, wrapper);
        
        return PageResult.of(resultPage.getRecords(), resultPage.getTotal(), pageQuery.getPageSize(), pageQuery.getPageNum());
    }

    @Override
    public OAuth2Authorization getAuthorizationById(String id) {
        // 使用自定义的 XML 映射，确保 JSONB 字段正确映射
        return baseMapper.selectById(id);
    }

    @Override
    public List<OAuth2Authorization> getAuthorizationsByClientId(String clientId) {
        LambdaQueryWrapper<OAuth2Authorization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuth2Authorization::getRegisteredClientId, clientId)
                .orderByDesc(OAuth2Authorization::getAccessTokenIssuedAt);
        return list(wrapper);
    }

    @Override
    public List<OAuth2Authorization> getAuthorizationsByPrincipal(String principalName) {
        LambdaQueryWrapper<OAuth2Authorization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuth2Authorization::getPrincipalName, principalName)
                .orderByDesc(OAuth2Authorization::getAccessTokenIssuedAt);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean revokeAuthorization(String id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean revokeByAccessToken(String accessTokenValue) {
        if (StrUtil.isBlank(accessTokenValue)) {
            throw new RuntimeException("Access Token 不能为空");
        }
        
        LambdaQueryWrapper<OAuth2Authorization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuth2Authorization::getAccessTokenValue, accessTokenValue);
        
        OAuth2Authorization authorization = getOne(wrapper);
        if (authorization != null) {
            return removeById(authorization.getId());
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean revokeByRefreshToken(String refreshTokenValue) {
        if (StrUtil.isBlank(refreshTokenValue)) {
            throw new RuntimeException("Refresh Token 不能为空");
        }
        
        LambdaQueryWrapper<OAuth2Authorization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuth2Authorization::getRefreshTokenValue, refreshTokenValue);
        
        OAuth2Authorization authorization = getOne(wrapper);
        if (authorization != null) {
            return removeById(authorization.getId());
        }
        return false;
    }

    @Override
    public OAuth2Authorization getAuthorizationByToken(String tokenValue, String tokenType) {
        if (CharSequenceUtil.isBlank(tokenValue) || CharSequenceUtil.isBlank(tokenType)) {
            return null;
        }
        
        // 使用自定义的 XML 查询，确保 JSONB 字段正确映射
        OAuth2Authorization authorization = baseMapper.selectByToken(tokenValue, tokenType);
        return authorization;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer cleanupExpiredAuthorizations() {
        LocalDateTime now = LocalDateTime.now();
        
        // 查询所有已过期的授权
        LambdaQueryWrapper<OAuth2Authorization> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.lt(OAuth2Authorization::getAccessTokenExpiresAt, now)
                          .or().lt(OAuth2Authorization::getRefreshTokenExpiresAt, now)
                          .or().lt(OAuth2Authorization::getAuthorizationCodeExpiresAt, now)
                          .or().lt(OAuth2Authorization::getUserCodeExpiresAt, now)
                          .or().lt(OAuth2Authorization::getDeviceCodeExpiresAt, now));
        
        List<OAuth2Authorization> expiredList = list(wrapper);
        if (expiredList.isEmpty()) {
            return 0;
        }
        
        // 批量删除过期的授权
        for (OAuth2Authorization authorization : expiredList) {
            removeById(authorization.getId());
        }
        
        return expiredList.size();
    }
}
