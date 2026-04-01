package com.jnet.system.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PostgreSQL JSONB Set 类型处理器
 * 专门用于处理 Set<String> 与 PostgreSQL JSONB 类型之间的转换
 */
public class JsonbSetTypeHandler extends BaseTypeHandler<Set<String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 设置非空参数到 PreparedStatement
     * 将 Set<String> 转换为 JSONB 格式存储到数据库
     * 
     * @param ps PreparedStatement
     * @param i 参数位置
     * @param parameter 要设置的参数（Set<String>）
     * @param jdbcType JDBC 类型
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            // 将 Set 序列化为 JSON 数组字符串
            String jsonValue = objectMapper.writeValueAsString(parameter);
            pGobject.setValue(jsonValue);
            ps.setObject(i, pGobject);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting Set to JSONB: " + e.getMessage(), e);
        }
    }

    /**
     * 从 ResultSet 获取结果（通过列名）
     * 将数据库中的 JSONB 转换为 Set<String>
     * 
     * @param rs ResultSet
     * @param columnName 列名
     * @return Set<String> 对象
     * @throws SQLException SQL 异常
     */
    @Override
    public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJsonb(rs.getString(columnName));
    }

    /**
     * 从 ResultSet 获取结果（通过列索引）
     * 将数据库中的 JSONB 转换为 Set<String>
     * 
     * @param rs ResultSet
     * @param columnIndex 列索引
     * @return Set<String> 对象
     * @throws SQLException SQL 异常
     */
    @Override
    public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJsonb(rs.getString(columnIndex));
    }

    /**
     * 从 CallableStatement 获取结果（存储过程）
     * 将数据库中的 JSONB 转换为 Set<String>
     * 
     * @param cs CallableStatement
     * @param columnIndex 列索引
     * @return Set<String> 对象
     * @throws SQLException SQL 异常
     */
    @Override
    public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJsonb(cs.getString(columnIndex));
    }

    /**
     * 解析 JSONB 字符串为 Set<String>
     * 自动将 JSON 数组转换为 HashSet
     * 
     * @param json JSONB 字符串
     * @return Set<String> 对象
     * @throws SQLException SQL 异常
     */
    @SuppressWarnings("unchecked")
    private Set<String> parseJsonb(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            // Jackson 会将 JSON 数组解析为 List
            List<String> list = objectMapper.readValue(json, List.class);
            // 转换为 HashSet
            return new HashSet<>(list);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing JSONB to Set: " + e.getMessage(), e);
        }
    }
}
