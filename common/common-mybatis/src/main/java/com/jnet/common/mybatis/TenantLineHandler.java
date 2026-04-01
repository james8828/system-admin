package com.jnet.common.mybatis;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.util.StringUtils;

import java.util.List;

public class TenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

    private final List<String> excludeTables;

    public TenantLineHandler(List<String> excludeTables) {
        this.excludeTables = excludeTables != null ? excludeTables : new java.util.ArrayList<>();
    }

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

    @Override
    public boolean ignoreTable(String tableName) {
        if (excludeTables == null || excludeTables.isEmpty()) {
            return false;
        }
        return excludeTables.stream()
                .anyMatch(table -> table.equalsIgnoreCase(tableName));
    }

}
