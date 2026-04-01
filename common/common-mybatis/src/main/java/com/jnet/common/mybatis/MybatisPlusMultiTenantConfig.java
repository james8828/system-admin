package com.jnet.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnProperty(prefix = "jnet.tenant", name = "enabled", havingValue = "true")
public class MybatisPlusMultiTenantConfig {

    @Value("${jnet.tenant.enabled:false}")
    private Boolean tenantEnabled;

    @Value("${jnet.tenant.exclude-tables:sys_user,sys_role,sys_menu,sys_dept,sys_oper_log}")
    private String excludeTables;

    private List<String> getExcludeTablesList() {
        if (excludeTables == null || excludeTables.trim().isEmpty()) {
            return Arrays.asList("sys_user", "sys_role", "sys_menu", "sys_dept", "sys_oper_log");
        }
        return Arrays.stream(excludeTables.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        List<String> excludeTablesList = getExcludeTablesList();

        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler(excludeTablesList));
        interceptor.addInnerInterceptor(tenantInterceptor);

        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInterceptor);

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

}
