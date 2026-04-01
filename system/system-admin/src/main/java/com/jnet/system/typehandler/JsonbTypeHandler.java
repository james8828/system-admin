package com.jnet.system.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
 * PostgreSQL JSONB 类型处理器
 * 用于处理 Java 对象（Set、List、Map、Object）与 PostgreSQL JSONB 类型之间的转换
 * 
 * @param <T> 处理的类型
 */
@Slf4j
public class JsonbTypeHandler<T> extends BaseTypeHandler<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 设置非空参数到 PreparedStatement
     * 将 Java 对象转换为 JSONB 格式存储到数据库
     * 
     * @param ps PreparedStatement
     * @param i 参数位置
     * @param parameter 要设置的参数
     * @param jdbcType JDBC 类型
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            // 将 Java 对象序列化为 JSON 字符串
            String jsonValue = objectMapper.writeValueAsString(parameter);
            pGobject.setValue(jsonValue);
            ps.setObject(i, pGobject);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting object to JSONB: " + e.getMessage(), e);
        }
    }

    /**
     * 从 ResultSet 获取结果（通过列名）
     * 将数据库中的 JSONB 转换为 Java 对象
     * 
     * @param rs ResultSet
     * @param columnName 列名
     * @return Java 对象
     * @throws SQLException SQL 异常
     */
    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJsonb(rs.getString(columnName));
    }

    /**
     * 从 ResultSet 获取结果（通过列索引）
     * 将数据库中的 JSONB 转换为 Java 对象
     * 
     * @param rs ResultSet
     * @param columnIndex 列索引
     * @return Java 对象
     * @throws SQLException SQL 异常
     */
    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJsonb(rs.getString(columnIndex));
    }

    /**
     * 从 CallableStatement 获取结果（存储过程）
     * 将数据库中的 JSONB 转换为 Java 对象
     * 
     * @param cs CallableStatement
     * @param columnIndex 列索引
     * @return Java 对象
     * @throws SQLException SQL 异常
     */
    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJsonb(cs.getString(columnIndex));
    }

    /**
     * 解析 JSONB 字符串为 Java 对象
     * 根据目标类型自动转换为 Set、List 或 Map
     * 
     * @param json JSONB 字符串
     * @return Java 对象（Set、List、Map 或其他）
     * @throws SQLException SQL 异常
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private T parseJsonb(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            // 先解析为 Object
            Object obj = objectMapper.readValue(json, Object.class);
            
            // 如果目标是 Set 类型，将 List 转换为 Set
            if (obj instanceof List list) {
                // 检查是否需要转换为 Set
                if (isSetType()) {
                    Set<String> set = new HashSet<>();
                    for (Object item : list) {
                        if (item != null) {
                            set.add(item.toString());
                        }
                    }
                    return (T) set;
                }
                // 否则返回 List
                return (T) list;
            }
            
            // Map 或其他类型直接返回
            return (T) obj;
        } catch (JsonProcessingException e) {
            throw new SQLException("Error parsing JSONB: " + e.getMessage(), e);
        }
    }
    
    /**
     * 判断目标类型是否为 Set
     * 通过检查泛型类型信息来判断
     * 
     * @return 如果是 Set 类型返回 true
     */
    private boolean isSetType() {
        try {
            // 获取父类的泛型类型信息
            var genericSuperclass = this.getClass().getGenericSuperclass();
            if (genericSuperclass instanceof java.lang.reflect.ParameterizedType paramType) {
                var typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                    boolean isSet = Set.class.isAssignableFrom(clazz);
                    log.debug("Checking if target type is Set: {} -> {}", clazz.getName(), isSet);
                    return isSet;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to determine target type: {}", e.getMessage());
        }
        return false;
    }
}
