package com.jnet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.del_flag = 0 " +
            "ORDER BY m.order_num")
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.perms IS NOT NULL AND m.perms != '' " +
            "AND m.del_flag = false")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "AND m.del_flag = false " +
            "ORDER BY m.order_num")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

}
