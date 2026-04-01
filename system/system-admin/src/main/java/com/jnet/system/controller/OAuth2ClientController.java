package com.jnet.system.controller;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.system.api.dto.OAuth2ClientDTO;
import com.jnet.system.service.OAuth2ClientService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth2 客户端 Controller
 */
@RestController
@RequestMapping("/api/system/oauth2/client")
public class OAuth2ClientController {

    @Resource
    private OAuth2ClientService clientService;

    /**
     * 分页查询客户端列表
     */
    @PreAuthorize("@ss.hasPermi('system:client:list')")
    @GetMapping("/page")
    public Result<PageResult<OAuth2ClientDTO>> pageClient(PageQuery pageQuery, OAuth2ClientDTO client) {
        PageResult<OAuth2ClientDTO> result = clientService.pageClient(pageQuery, client);
        return Result.success(result);
    }

    @PreAuthorize("@ss.hasPermi('system:client:query')")
    @GetMapping("/{id}")
    public Result<OAuth2ClientDTO> getClientById(@PathVariable("id") String id) {
        OAuth2ClientDTO client = clientService.getClientById(id);
        return Result.success(client);
    }

    /**
     * 根据 ClientId 查询客户端
     */
    @PreAuthorize("@ss.hasPermi('system:client:query')")
    @GetMapping("/byClientId/{clientId}")
    public Result<OAuth2ClientDTO> getClientByClientId(@PathVariable("clientId") String clientId) {
        OAuth2ClientDTO client = clientService.getClientByClientId(clientId);
        return Result.success(client);
    }

    /**
     * 创建客户端
     */
    @PreAuthorize("@ss.hasPermi('system:client:add')")
    @PostMapping
    public Result<Void> createClient(@RequestBody OAuth2ClientDTO dto) {
        clientService.createClient(dto);
        return Result.success();
    }

    /**
     * 更新客户端
     */
    @PreAuthorize("@ss.hasPermi('system:client:edit')")
    @PutMapping
    public Result<Void> updateClient(@RequestBody OAuth2ClientDTO dto) {
        clientService.updateClient(dto);
        return Result.success();
    }

    /**
     * 删除客户端
     */
    @PreAuthorize("@ss.hasPermi('system:client:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> deleteClient(@PathVariable("id") String id) {
        clientService.deleteClient(id);
        return Result.success();
    }

    /**
     * 刷新客户端密钥
     */
    @PreAuthorize("@ss.hasPermi('system:client:edit')")
    @PostMapping("/refresh-secret/{clientId}")
    public Result<String> refreshSecret(@PathVariable("clientId") String clientId) {
        String newSecret = clientService.refreshSecret(clientId);
        return Result.success(newSecret, "密钥已刷新");
    }
}
