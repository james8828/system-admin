package com.jnet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.system.entity.OAuth2Authorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * OAuth2 授权 Mapper 接口
 */
@Mapper
public interface OAuth2AuthorizationMapper extends BaseMapper<OAuth2Authorization> {

    /**
     * 根据 Token 查询授权信息
     *
     * @param tokenValue Token 值
     * @param tokenType Token 类型（access_token, refresh_token, code）
     * @return OAuth2Authorization 对象
     */
    OAuth2Authorization selectByToken(@Param("tokenValue") String tokenValue,
                                      @Param("tokenType") String tokenType);
}
