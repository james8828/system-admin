package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.api.dto.OAuth2ClientDTO;
import com.jnet.system.entity.OAuth2Client;

/**
 * OAuth2 客户端 Service 接口
 */
public interface OAuth2ClientService extends IService<OAuth2Client> {

    /**
     * 分页查询客户端列表
     */
    PageResult<OAuth2ClientDTO> pageClient(PageQuery pageQuery, OAuth2ClientDTO client);

    /**
     * 根据 ID 查询客户端
     */
    OAuth2ClientDTO getClientById(String id);

    /**
     * 根据 ClientId 查询客户端
     */
    OAuth2ClientDTO getClientByClientId(String clientId);

    /**
     * 创建客户端
     */
    Boolean createClient(OAuth2ClientDTO dto);

    /**
     * 更新客户端
     */
    Boolean updateClient(OAuth2ClientDTO dto);

    /**
     * 删除客户端
     */
    Boolean deleteClient(String id);

    /**
     * 刷新客户端密钥
     */
    String refreshSecret(String clientId);
}
