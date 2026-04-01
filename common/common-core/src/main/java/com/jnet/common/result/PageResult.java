package com.jnet.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 * 
 * <p>用于统一分页查询的返回格式，包含数据列表和分页信息</p>
 * 
 * <h3>数据结构：</h3>
 * <ul>
 *     <li>records - 当前页的数据列表</li>
 *     <li>total - 总记录数</li>
 *     <li>size - 每页大小</li>
 *     <li>current - 当前页码（从 1 开始）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建分页结果
 * PageResult<User> result = PageResult.of(users, total, pageSize, pageNum);
 * }</pre>
 * 
 * @param <T> 数据类型
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    private long total;

    private long size;

    private long current;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, long size, long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long size, long current) {
        return new PageResult<>(records, total, size, current);
    }

}
