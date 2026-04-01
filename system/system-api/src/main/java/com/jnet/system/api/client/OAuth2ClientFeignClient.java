package com.jnet.system.api.client;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.system.api.dto.OAuth2ClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth2 客户端服务 Feign 客户端
 */
@FeignClient(name = "jnet-system-admin", contextId = "oauth2ClientClient")
public interface OAuth2ClientFeignClient {

    /**
     * 分页查询客户端列表
     */
    @GetMapping("/api/system/oauth2/client/page")
    Result<PageResult<OAuth2ClientDTO>> pageClient(@SpringQueryMap PageQuery pageQuery, @SpringQueryMap OAuth2ClientDTO client);

    /**
     * 根据 ID 查询客户端
     */
    @GetMapping("/api/system/oauth2/client/{id}")
    Result<OAuth2ClientDTO> getClientById(@PathVariable("id") String id);

    /**
     * 根据 ClientId 查询客户端
     */
    @GetMapping("/api/system/oauth2/client/byClientId/{clientId}")
    Result<OAuth2ClientDTO> getClientByClientId(@PathVariable("clientId") String clientId);

    /**
     * 创建客户端
     */
    @PostMapping("/api/system/oauth2/client")
    Result<Void> createClient(@RequestBody OAuth2ClientDTO dto);

    /**
     * 更新客户端
     */
    @PutMapping("/api/system/oauth2/client")
    Result<Void> updateClient(@RequestBody OAuth2ClientDTO dto);

    /**
     * 删除客户端
     */
    @DeleteMapping("/api/system/oauth2/client/{id}")
    Result<Void> deleteClient(@PathVariable("id") String id);

    /**
     * 刷新客户端密钥
     */
    @PostMapping("/api/system/oauth2/client/refresh-secret/{clientId}")
    Result<String> refreshSecret(@PathVariable("clientId") String clientId);

}
