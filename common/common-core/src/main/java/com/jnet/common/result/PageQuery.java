package com.jnet.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询参数封装类
 * 
 * <p>用于统一分页查询的输入参数，包含页码和每页大小</p>
 * 
 * <h3>字段说明：</h3>
 * <ul>
 *     <li>pageNum - 当前页码（默认 1）</li>
 *     <li>pageSize - 每页大小（默认 10）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用默认值（第 1 页，每页 10 条）
 * PageQuery query = new PageQuery();
 * 
 * // 自定义分页参数
 * PageQuery query = new PageQuery(2, 20);
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int pageNum = 1;

    private int pageSize = 10;

    public PageQuery() {
    }

    public PageQuery(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

}
