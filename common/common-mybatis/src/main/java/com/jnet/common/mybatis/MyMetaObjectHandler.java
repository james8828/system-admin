package com.jnet.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 * 
 * <p>实现插入和更新操作时自动填充公共字段（createTime、updateTime）</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>insertFill - 插入时自动填充 createTime 和 updateTime 为当前时间</li>
 *     <li>updateFill - 更新时自动填充 updateTime 为当前时间</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 实体类字段添加注解
 * @TableField(fill = FieldFill.INSERT)
 * private LocalDateTime createTime;
 * 
 * @TableField(fill = FieldFill.INSERT_UPDATE)
 * private LocalDateTime updateTime;
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

}
