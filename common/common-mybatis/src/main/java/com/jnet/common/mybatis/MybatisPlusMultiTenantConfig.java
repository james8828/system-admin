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

/**
 * MyBatis Plus 配置类（多租户模式）
 * 
 * <p>当 jnet.tenant.enabled=true 时启用，提供多租户数据隔离功能</p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *     <li>多租户隔离 - 基于 TenantLineInnerInterceptor 自动过滤 tenant_id</li>
 *     <li>分页查询 - 基于 PaginationInnerInterceptor 实现物理分页（MySQL）</li>
 *     <li>乐观锁 - 基于 OptimisticLockerInnerInterceptor 实现版本控制</li>
 * </ul>
 * 
 * <h3>配置项：</h3>
 * <ul>
 *     <li>jnet.tenant.enabled - 是否启用多租户（默认 false）</li>
 *     <li>jnet.tenant.exclude-tables - 排除的表名列表（逗号分隔）</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Configuration
@ConditionalOnProperty(prefix = "jnet.tenant", name = "enabled", havingValue = "true")
public class MybatisPlusMultiTenantConfig {

    @Value("${jnet.tenant.enabled:false}")
    private Boolean tenantEnabled;

    @Value("${jnet.tenant.exclude-tables:sys_user,sys_role,sys_menu,sys_dept,sys_oper_log}")
    private String excludeTables;

    /**
     * 解析排除表列表
     * @return 排除的表名列表
     */
    private List<String> getExcludeTablesList() {
        if (excludeTables == null || excludeTables.trim().isEmpty()) {
            return Arrays.asList("sys_user", "sys_role", "sys_menu", "sys_dept", "sys_oper_log");
        }
        return Arrays.stream(excludeTables.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 配置 MyBatis Plus 拦截器链（多租户模式）
     * 
     * @return MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        List<String> excludeTablesList = getExcludeTablesList();

        // 多租户拦截器 - 自动在 SQL 中添加 tenant_id 条件
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
        tenantInterceptor.setTenantLineHandler(new TenantLineHandler(excludeTablesList));
        interceptor.addInnerInterceptor(tenantInterceptor);

        // 分页拦截器 - 实现物理分页（MySQL）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L); // 限制最大查询条数，防止全表扫描
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁拦截器 - 实现版本控制
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

}
