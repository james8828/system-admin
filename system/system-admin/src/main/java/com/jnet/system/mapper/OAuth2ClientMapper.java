package com.jnet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.system.entity.OAuth2Client;
import org.apache.ibatis.annotations.Mapper;

/**
 * OAuth2 客户端 Mapper 接口
 */
@Mapper
public interface OAuth2ClientMapper extends BaseMapper<OAuth2Client> {

}
