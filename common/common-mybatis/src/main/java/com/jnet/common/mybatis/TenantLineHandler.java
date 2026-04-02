package com.jnet.common.mybatis;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 多租户行级权限处理器
 * 
 * <p>实现 MyBatis Plus 的 TenantLineHandler 接口，用于在 SQL 执行时自动添加租户条件</p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *     <li>getTenantId - 获取当前租户 ID，用于 SQL 条件拼接</li>
 *     <li>ignoreTable - 判断表是否需要跳过租户过滤</li>
 * </ul>
 * 
 * <h3>工作原理：</h3>
 * <ol>
 *     <li>从 TenantContextHolder 获取当前请求的租户 ID</li>
 *     <li>如果租户 ID 为空或无效，返回 0（默认租户）</li>
 *     <li>检查表名是否在排除列表中，如果是则跳过租户过滤</li>
 * </ol>
 * 
 * <h3>排除表说明：</h3>
 * <p>以下表通常不需要租户隔离：</p>
 * <ul>
 *     <li>sys_user - 用户表（全局用户）</li>
 *     <li>sys_role - 角色表（全局角色）</li>
 *     <li>sys_menu - 菜单表（全局菜单）</li>
 *     <li>sys_dept - 部门表（跨租户）</li>
 *     <li>sys_oper_log - 操作日志表（审计需要）</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

public class TenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

    /**
     * 排除租户过滤的表名列表
     */
    private final List<String> excludeTables;

    /**
     * 构造函数
     * @param excludeTables 排除的表名列表
     */
    public TenantLineHandler(List<String> excludeTables) {
        this.excludeTables = excludeTables != null ? excludeTables : new java.util.ArrayList<>();
    }

    /**
     * 获取当前租户 ID
     * 
     * <p>从 TenantContextHolder 中获取租户 ID，用于 SQL 条件拼接</p>
     * 
     * @return 租户 ID 表达式，如果为空返回 0（默认租户）
     */
    @Override
    public Expression getTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return new LongValue(0);
        }
        try {
            return new LongValue(Long.parseLong(tenantId));
        } catch (NumberFormatException e) {
            return new LongValue(0);
        }
    }

    /**
     * 判断是否忽略某表的租户过滤
     * 
     * <p>检查表名是否在排除列表中</p>
     * 
     * @param tableName 表名
     * @return true-跳过租户过滤，false-需要租户过滤
     */
    @Override
    public boolean ignoreTable(String tableName) {
        if (excludeTables == null || excludeTables.isEmpty()) {
            return false;
        }
        return excludeTables.stream()
                .anyMatch(table -> table.equalsIgnoreCase(tableName));
    }

}
