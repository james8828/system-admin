package com.jnet.system.config;

import com.jnet.common.security.service.PermissionMetadataService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限配置初始化
 * 演示如何配置动态 URL 权限
 */
@Slf4j
@Component
public class PermissionConfig implements CommandLineRunner {

    @Resource
    private PermissionMetadataService permissionMetadataService;

    @Override
    public void run(String... args) throws Exception {
        if (permissionMetadataService == null) {
            log.warn("PermissionMetadataService not available, skip permission configuration");
            return;
        }

        log.info("Initializing dynamic URL permissions...");
        // 配置匿名访问 URL（不需要登录）
        permissionMetadataService.addAnonymousUrl("/oauth2/token");
        permissionMetadataService.addAnonymousUrl("/oauth2/authorize");
        permissionMetadataService.addAnonymousUrl("/login");
        permissionMetadataService.addAnonymousUrl("/logout");

        permissionMetadataService.addAnonymousUrl("/api/system/permissions");

        log.info("Dynamic URL permissions initialized successfully");
    }

    private Set<String> createSet(String... items) {
        Set<String> set = new HashSet<>();
        for (String item : items) {
            set.add(item);
        }
        return set;
    }
}
