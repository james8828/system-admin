package com.jnet.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class MybatisPlusConfig {

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
        
        // 多租户拦截器
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler(excludeTablesList));
        interceptor.addInnerInterceptor(tenantInterceptor);
        
        // 分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 乐观锁拦截器
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }

}
