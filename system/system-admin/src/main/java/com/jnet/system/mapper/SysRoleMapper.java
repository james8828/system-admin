package com.jnet.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jnet.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

}
